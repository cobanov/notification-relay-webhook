# Notification Relay Webhook

Notification Relay Webhook is an Android app that forwards incoming phone notifications to your own webhook endpoints as JSON. It is built around Android's `NotificationListenerService`, a Jetpack Compose UI, delivery logs, a persistent recent-notification history, and an optional local HTTP server for LAN tools and agents.

The app adapts the webhook-delivery and local-server approach from [health-connect-webhook](https://github.com/cobanov/health-connect-webhook), but replaces Health Connect reads with Android notification capture.

## Overview

Use this app when you want Android notifications to become structured webhook events. You can forward all notifications, only selected apps, or everything except blocked apps. Delivery attempts are logged, failed or queued notifications can be resent manually, and the latest 300 captured notifications remain available in the app for inspection.

## How It Works

1. Android posts a notification from an app such as WhatsApp, Slack, Gmail, or a system service.
2. Notification Relay captures it through user-granted notification access.
3. The app applies your forwarding mode: all apps, allowlist, or blocklist.
4. Matching notifications are stored in recent history and sent to every enabled webhook URL.
5. Your webhook receiver, local agent, automation, or server processes the JSON payload.

## Features

- Capture Android notifications through notification access.
- Forward to multiple webhook URLs with custom headers.
- Test a webhook before saving it.
- Edit existing webhook URLs and custom headers.
- Filter by all apps, allowlist, or blocklist.
- Optional "show system apps" control in app selection.
- Ignore ongoing notifications and group-summary notifications.
- Keep the latest 300 captured notifications in recent history.
- Track delivery status: queued, sent, failed, or ignored.
- Manually resend queued or failed notifications.
- View detailed webhook logs, payloads, status codes, response times, and errors.
- Optional local HTTP server with bearer-token auth.
- Export and import settings as JSON.
- Battery optimization guidance for more reliable background operation.

## API Reference

Integrations can rely on these documents:

- [Webhook payload](docs/webhook.md): POST body schema, fields, examples, and delivery behavior.
- [Local HTTP server](docs/local-http.md): endpoints, auth, query parameters, and examples.

## Requirements

- Android 8.0 (API 26) or newer.
- Notification access granted by the user.
- Internet access for webhook delivery.
- Optional: unrestricted battery usage for more reliable background forwarding and local HTTP availability.

## Installation

### GitHub Releases

Download the latest APK from the [Releases](https://github.com/cobanov/notification-relay-webhook/releases) page when a signed build is published.

### Install via Obtainium

You can install and update the app with [Obtainium](https://github.com/ImranR98/Obtainium):

1. Install Obtainium on your Android device.
2. Tap "Add App".
3. Enter this repository URL:

```text
https://github.com/cobanov/notification-relay-webhook
```

4. Let Obtainium scan GitHub releases.
5. Install or update from the detected APK release.

### From Source

```bash
git clone https://github.com/cobanov/notification-relay-webhook
cd notification-relay-webhook
./gradlew assembleFossDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/foss/debug/app-foss-debug.apk
```

## Usage

### Initial Setup

1. Open the app.
2. Grant notification access in Android settings.
3. Add a webhook URL.
4. Add headers if your endpoint requires authentication.
5. Use "Test" to verify delivery.
6. Save the webhook.
7. Choose whether to forward all apps, allowlist apps, or blocklist apps.

### Webhooks

Webhook delivery uses:

- Method: `POST`
- Content type: `application/json; charset=utf-8`
- Body: one JSON object per notification
- Retries: up to 3 attempts for retryable failures

Example payload:

```json
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
```

### Recent Notifications

The app keeps the latest 300 captured notifications locally. Sent notifications are shown as sent, failed deliveries remain available for retry, and ignored notifications can be reviewed to confirm filtering behavior.

### Local HTTP Server

Enable the local HTTP server from the app to expose recent notifications and logs to tools on your local network.

Default port: `8787`

Example:

```text
http://192.168.1.25:8787/recent?limit=25
```

If bearer-token auth is enabled, send:

```text
Authorization: Bearer <token>
```

See [docs/local-http.md](docs/local-http.md) for endpoint details.

## Configuration

### Forwarding Mode

- `All apps`: forward every captured notification unless ignored by the ongoing/group-summary toggles.
- `Allowlist`: forward only selected app packages.
- `Blocklist`: forward everything except selected app packages.

### Local HTTP

The local server runs as a foreground service while enabled. Android battery optimization, Wi-Fi sleep, and OEM background restrictions can still affect availability.

### Settings Backup

Use the About screen to export or import webhook URLs, headers, filtering settings, and local HTTP settings as JSON.

## Privacy and Security

- Notifications stay on your device unless sent to endpoints you configure.
- Webhook URLs and headers are stored locally in app preferences.
- The latest 300 notification records are stored locally for review and retry.
- Webhook log payloads are stored locally and truncated.
- Local HTTP server data is available to clients that can reach your device on the configured port.
- Enable local HTTP bearer-token auth when using the server outside a trusted LAN.

## Known Limitations

- Notification access must remain enabled in Android settings.
- Android/OEM battery rules may pause background work or the local server.
- The local HTTP server binds to `0.0.0.0`; use it only on trusted networks and enable bearer-token auth when needed.
- Release APKs require a stable signing key. Debug builds are suitable for local testing, not public distribution.

## Technical Details

- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Notification capture: `NotificationListenerService`
- Networking: OkHttp
- Local server: lightweight raw-socket HTTP server in a foreground service
- Storage: SharedPreferences with Kotlinx Serialization JSON
- Minimum SDK: 26
- Compile SDK: 35

### Key Components

- `MainActivity`: navigation host and app entry point.
- `NotificationRelayService`: captures notifications and triggers delivery.
- `NotificationPayloadBuilder`: serializes notification data into webhook JSON.
- `WebhookManager`: sends webhook requests and records delivery logs.
- `PreferencesManager`: stores webhooks, filters, logs, recent history, and local HTTP settings.
- `LocalHttpServerService`: foreground service wrapper for the local server.
- `LocalHttpServerManager`: socket server and HTTP endpoint implementation.
- `RecentNotificationsScreen`: recent history, details, and manual resend.
- `WebhooksScreen`: webhook list, test, add, edit, and header management.
- `LogsScreen`: webhook delivery logs and payload details.

## Building

```bash
# FOSS debug build
./gradlew assembleFossDebug

# FOSS release build, requires app/release.jks and signing env vars
KEYSTORE_PASSWORD=... KEY_ALIAS=... KEY_PASSWORD=... ./gradlew assembleFossRelease
```

`app/release.jks` is intentionally ignored by git.

## Contributing

Issues and pull requests are welcome.

1. Fork the repository.
2. Create a feature branch.
3. Commit your changes.
4. Open a pull request.

## License

This project is licensed under the GNU Affero General Public License v3.0.

SPDX-License-Identifier: AGPL-3.0-only
