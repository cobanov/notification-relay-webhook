package com.notifrelay.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

data class ServerRequestLog(
    val id: String,
    val timestamp: Long,
    val method: String,
    val path: String,
    val statusCode: Int,
    val responseTimeMs: Long,
    val clientIp: String
)

/**
 * A tiny raw-socket HTTP server that exposes recently captured notifications on the
 * LAN. Ported and trimmed from the health-connect-webhook project (which served
 * health payloads); here it serves the notification ring buffer.
 *
 * Endpoints: GET /ping, /latest, /recent, /logs, /server-logs, /health.
 */
object LocalHttpServerManager {
    private val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val serverMutex = Mutex()
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var currentPort: Int? = null
    private var appContext: Context? = null
    private val latestPayload = AtomicReference<String?>()
    private val serverStartTime = AtomicLong(0L)

    private val requestLogs = mutableListOf<ServerRequestLog>()
    private const val MAX_REQUEST_LOGS = 200

    private fun recordRequestLog(log: ServerRequestLog) {
        synchronized(requestLogs) {
            requestLogs.add(0, log)
            if (requestLogs.size > MAX_REQUEST_LOGS) requestLogs.removeAt(requestLogs.size - 1)
        }
    }

    fun getRequestLogs(): List<ServerRequestLog> =
        synchronized(requestLogs) { requestLogs.toList() }

    fun clearRequestLogs() {
        synchronized(requestLogs) { requestLogs.clear() }
    }

    suspend fun syncWithPreferences(context: Context) {
        appContext = context.applicationContext
        val prefs = PreferencesManager(context)
        if (prefs.isLocalHttpEnabled()) start(prefs.getLocalHttpPort()) else stop()
    }

    suspend fun start(port: Int) {
        serverMutex.withLock {
            val activeSocket = serverSocket
            if (activeSocket != null && !activeSocket.isClosed && currentPort == port) return
            stopLocked()

            val socket = ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))
            serverSocket = socket
            currentPort = port
            serverStartTime.set(System.currentTimeMillis())

            serverJob = serverScope.launch {
                while (isActive && !socket.isClosed) {
                    try {
                        val client = socket.accept()
                        launch { handleHttpClient(client) }
                    } catch (_: Exception) {
                        if (socket.isClosed) break
                    }
                }
            }
        }
    }

    suspend fun stop() {
        serverMutex.withLock { stopLocked() }
    }

    fun publishPayload(jsonPayload: String) {
        latestPayload.set(jsonPayload)
    }

    fun getCurrentPort(): Int? = currentPort

    private suspend fun stopLocked() {
        serverSocket?.close()
        serverSocket = null
        currentPort = null
        serverStartTime.set(0L)
        serverJob?.cancelAndJoin()
        serverJob = null
    }

    private fun handleHttpClient(socket: Socket) {
        val startTime = System.currentTimeMillis()
        val clientIp = socket.inetAddress?.hostAddress ?: "unknown"
        var logMethod = ""
        var logPath = "/"
        var logStatus = 0

        socket.use { client ->
            try {
                client.soTimeout = 5_000
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                val writer = BufferedWriter(OutputStreamWriter(client.getOutputStream()))

                fun reply(code: Int, body: String) {
                    logStatus = code
                    writeHttpResponse(writer, code, body)
                }

                val requestLine = reader.readLine()?.trim().orEmpty()
                if (requestLine.isEmpty()) {
                    reply(400, """{"status":"error","message":"Empty request"}""")
                    return
                }
                val parts = requestLine.split(" ")
                if (parts.size < 2) {
                    reply(400, """{"status":"error","message":"Invalid request line"}""")
                    return
                }
                val method = parts[0].uppercase()
                val rawPath = parts[1]
                logMethod = method
                logPath = rawPath.substringBefore("?")

                var authorizationHeader: String? = null
                while (true) {
                    val headerLine = reader.readLine() ?: break
                    if (headerLine.isBlank()) break
                    if (headerLine.lowercase().startsWith("authorization:")) {
                        authorizationHeader = headerLine.substringAfter(":").trim()
                    }
                }

                val ctx = appContext
                if (ctx != null) {
                    val prefs = PreferencesManager(ctx)
                    if (prefs.isLocalHttpAuthEnabled()) {
                        val expected = "Bearer ${prefs.getLocalHttpToken()}"
                        if (authorizationHeader != expected) {
                            reply(401, """{"status":"error","message":"Unauthorized"}""")
                            return
                        }
                    }
                }

                if (method == "OPTIONS") {
                    logStatus = 204
                    writeCorsPreflightResponse(writer)
                    return
                }

                val path = logPath
                val queryParams = parseQueryParams(rawPath.substringAfter("?", ""))

                when {
                    method == "GET" && path == "/ping" ->
                        reply(200, """{"status":"ok"}""")

                    method == "GET" && path == "/latest" ->
                        reply(200, latestPayload.get() ?: """{"status":"no_data"}""")

                    method == "GET" && (path == "/" || path == "/recent") -> {
                        val limit = queryParams["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50
                        val items = recentNotificationsForResponse().take(limit)
                        val arr = items.joinToString(",") { NotificationPayloadBuilder.build(it) }
                        reply(200, """{"status":"ok","count":${items.size},"notifications":[$arr]}""")
                    }

                    method == "GET" && path == "/logs" -> {
                        val context = appContext ?: run {
                            reply(500, """{"status":"error","message":"Server context unavailable"}""")
                            return
                        }
                        logStatus = handleLogsRequest(writer, context, queryParams)
                    }

                    method == "GET" && path == "/server-logs" ->
                        logStatus = handleServerLogsRequest(writer, queryParams)

                    method == "GET" && path == "/health" -> {
                        val uptimeMs = System.currentTimeMillis() - serverStartTime.get()
                        reply(200, """{"status":"ok","serverUptimeMs":$uptimeMs,"recentCount":${recentNotificationsForResponse().size}}""")
                    }

                    else -> reply(
                        404,
                        """{"status":"error","message":"Unknown endpoint. Available: GET /, /recent, /latest, /ping, /logs, /server-logs, /health"}"""
                    )
                }
            } catch (_: Exception) {
                // Ignore bad client connections.
            } finally {
                if (logMethod.isNotEmpty() && logStatus != 0) {
                    recordRequestLog(
                        ServerRequestLog(
                            id = UUID.randomUUID().toString(),
                            timestamp = startTime,
                            method = logMethod,
                            path = logPath,
                            statusCode = logStatus,
                            responseTimeMs = System.currentTimeMillis() - startTime,
                            clientIp = clientIp
                        )
                    )
                }
            }
        }
    }

    private fun handleLogsRequest(writer: BufferedWriter, context: Context, params: Map<String, String>): Int {
        val prefs = PreferencesManager(context)
        var logs = prefs.getWebhookLogs()

        params["success"]?.let { s ->
            val wantSuccess = s.lowercase() == "true"
            logs = logs.filter { it.success == wantSuccess }
        }
        params["since"]?.toLongOrNull()?.let { since -> logs = logs.filter { it.timestamp >= since } }

        val limit = params["limit"]?.toIntOrNull()?.coerceIn(1, 500) ?: 50
        logs = logs.take(limit)

        val logsJson = logs.joinToString(",") { log ->
            buildString {
                append("{")
                append(""""id":"${log.id}",""")
                append(""""timestamp":${log.timestamp},""")
                append(""""url":"${log.url.redactUrl().escapeJson()}",""")
                append(""""success":${log.success},""")
                append(""""statusCode":${log.statusCode ?: "null"},""")
                append(""""package":${log.sourcePackage?.let { "\"${it.escapeJson()}\"" } ?: "null"},""")
                append(""""responseTimeMs":${log.responseTimeMs ?: "null"},""")
                append(""""errorMessage":${log.errorMessage?.let { "\"${it.escapeJson()}\"" } ?: "null"}""")
                append("}")
            }
        }
        writeHttpResponse(writer, 200, """{"status":"ok","count":${logs.size},"logs":[$logsJson]}""")
        return 200
    }

    private fun recentNotificationsForResponse(): List<NotificationData> {
        val ctx = appContext
        return if (ctx != null) {
            PreferencesManager(ctx).getNotificationQueue().map { it.notification }
        } else {
            RecentNotificationsStore.snapshot()
        }
    }

    private fun handleServerLogsRequest(writer: BufferedWriter, params: Map<String, String>): Int {
        var logs = getRequestLogs()
        params["since"]?.toLongOrNull()?.let { since -> logs = logs.filter { it.timestamp >= since } }
        val limit = params["limit"]?.toIntOrNull()?.coerceIn(1, 500) ?: 50
        logs = logs.take(limit)

        val logsJson = logs.joinToString(",") { log ->
            buildString {
                append("{")
                append(""""id":"${log.id}",""")
                append(""""timestamp":${log.timestamp},""")
                append(""""method":"${log.method}",""")
                append(""""path":"${log.path.escapeJson()}",""")
                append(""""statusCode":${log.statusCode},""")
                append(""""responseTimeMs":${log.responseTimeMs},""")
                append(""""clientIp":"${log.clientIp.escapeJson()}"""")
                append("}")
            }
        }
        writeHttpResponse(writer, 200, """{"status":"ok","count":${logs.size},"logs":[$logsJson]}""")
        return 200
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split("&").mapNotNull { param ->
            val eq = param.indexOf('=')
            if (eq < 0) null else param.substring(0, eq) to param.substring(eq + 1)
        }.toMap()
    }

    private fun String.escapeJson() = replace("\\", "\\\\").replace("\"", "\\\"")

    private fun String.redactUrl(): String {
        return try {
            val uri = java.net.URI(this)
            val scheme = uri.scheme
            val host = uri.host
            if (scheme == null || host == null) "[redacted]"
            else {
                val port = uri.port
                if (port == -1) "$scheme://$host" else "$scheme://$host:$port"
            }
        } catch (_: Exception) {
            "[redacted]"
        }
    }

    private fun writeCorsPreflightResponse(writer: BufferedWriter) {
        writer.write("HTTP/1.1 204 No Content\r\n")
        writer.write("Access-Control-Allow-Origin: *\r\n")
        writer.write("Access-Control-Allow-Methods: GET, OPTIONS\r\n")
        writer.write("Access-Control-Allow-Headers: Authorization, Content-Type\r\n")
        writer.write("Content-Length: 0\r\n")
        writer.write("Connection: close\r\n")
        writer.write("\r\n")
        writer.flush()
    }

    private fun writeHttpResponse(writer: BufferedWriter, statusCode: Int, body: String) {
        val statusText = when (statusCode) {
            200 -> "OK"
            204 -> "No Content"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            404 -> "Not Found"
            else -> "Internal Server Error"
        }
        val versionedBody = if (body.startsWith("{") && body.endsWith("}")) {
            val inner = body.drop(1).dropLast(1)
            if (inner.isNotEmpty()) """{"app_version":"${BuildConfig.VERSION_NAME}",$inner}"""
            else """{"app_version":"${BuildConfig.VERSION_NAME}"}"""
        } else body
        val bodyBytes = versionedBody.toByteArray(Charsets.UTF_8)
        writer.write("HTTP/1.1 $statusCode $statusText\r\n")
        writer.write("Content-Type: application/json; charset=utf-8\r\n")
        writer.write("Cache-Control: no-store\r\n")
        writer.write("Access-Control-Allow-Origin: *\r\n")
        writer.write("Content-Length: ${bodyBytes.size}\r\n")
        writer.write("Connection: close\r\n")
        writer.write("\r\n")
        writer.write(versionedBody)
        writer.flush()
    }
}
