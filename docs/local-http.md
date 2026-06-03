# Local HTTP Server

Notification Relay can expose recent notifications and delivery logs over a small local HTTP server. The server is intended for trusted LAN tools, local agents, dashboards, and scripts.

The server runs as a foreground service while enabled.

## Configuration

- Default port: `8787`
- Allowed port range: `1024` to `65535`
- Bind address: `0.0.0.0`
- Supported methods: `GET`, plus `OPTIONS` for CORS preflight
- Optional auth: bearer token

When bearer-token auth is enabled, send:

```text
Authorization: Bearer <token>
```

## Endpoints

### `GET /ping`

Health check.

Response:

```json
{"status":"ok"}
```

### `GET /health`

Server status.

Response:

```json
{
  "status": "ok",
  "serverUptimeMs": 12345,
  "recentCount": 50
}
```

### `GET /latest`

Returns the latest notification payload published to the local server, or `{"status":"no_data"}` if nothing is available.

### `GET /`

Returns recent captured notifications.

Query parameters:

- `limit`: optional integer from `1` to `100`; default `50`.

Example:

```text
GET /?limit=25
```

Response:

```json
{
  "status": "ok",
  "count": 1,
  "notifications": [
    {
      "package": "com.whatsapp",
      "packageName": "com.whatsapp",
      "app": "WhatsApp",
      "appName": "WhatsApp",
      "title": "Alice",
      "text": "Hey there",
      "sub_text": null,
      "big_text": null,
      "category": "msg",
      "post_time": "2026-06-03T12:34:56Z",
      "postedAt": "2026-06-03T12:34:56Z",
      "key": "0|com.whatsapp|...",
      "notificationKey": "0|com.whatsapp|...",
      "ongoing": false,
      "group_summary": false
    }
  ]
}
```

### `GET /recent`

Same response as `GET /`.

Query parameters:

- `limit`: optional integer from `1` to `100`; default `50`.

### `GET /logs`

Returns webhook delivery logs.

Query parameters:

- `limit`: optional integer from `1` to `500`; default `50`.
- `success`: optional `true` or `false`.
- `since`: optional epoch milliseconds; filters logs at or after that timestamp.

Response fields:

- `id`
- `timestamp`
- `url` with sensitive values redacted
- `success`
- `statusCode`
- `package`
- `responseTimeMs`
- `errorMessage`

### `GET /server-logs`

Returns local HTTP server request logs.

Query parameters:

- `limit`: optional integer from `1` to `500`; default `50`.

Each log includes method, path, status code, response time, timestamp, and client IP.

## Examples

```bash
curl "http://192.168.1.25:8787/ping"
curl "http://192.168.1.25:8787/recent?limit=25"
curl "http://192.168.1.25:8787/logs?success=false&limit=10"
```

With bearer-token auth:

```bash
curl \
  -H "Authorization: Bearer <token>" \
  "http://192.168.1.25:8787/recent?limit=25"
```

## Reliability Notes

Android can still affect local server availability through Doze mode, Wi-Fi sleep, and OEM battery restrictions. For the most reliable local server behavior, use unrestricted battery mode for the app and keep the device on a stable Wi-Fi network.

## Security Notes

The server binds to all interfaces on the device. Use it only on networks you trust, and enable bearer-token auth when other devices can reach the phone.
