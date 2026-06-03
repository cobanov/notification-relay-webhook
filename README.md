# Notification Relay Webhook

Android notifications to your own webhooks.

Notification Relay Webhook captures incoming Android notifications, turns them into JSON, and sends them to the webhook URLs you configure. It also keeps a local recent-notification history, delivery logs, and an optional local HTTP server for tools on your network.

## Features

- Forward Android notifications to one or more webhook URLs.
- Add custom headers for auth tokens and API keys.
- Filter forwarded notifications by all apps, allowlist, or blocklist.
- Keep the latest 300 captured notifications locally.
- Retry or manually resend queued/failed notifications.
- Inspect delivery logs, payloads, errors, status codes, and response times.
- Optional local HTTP server with bearer-token auth.
- Export/import app settings as JSON.

## Install

Download the latest APK from [GitHub Releases](https://github.com/cobanov/notification-relay-webhook/releases).

You can also install and update through [Obtainium](https://github.com/ImranR98/Obtainium) with this repository URL:

```text
https://github.com/cobanov/notification-relay-webhook
```

## Setup

1. Open the app.
2. Grant notification access from Android settings.
3. Add a webhook URL.
4. Add headers if your endpoint requires them.
5. Tap Test, then Save.
6. Choose forwarding mode: all apps, allowlist, or blocklist.

## Webhook Payload

Each notification is sent as a single `POST` JSON object.

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

More details:

- [Webhook payload docs](docs/webhook.md)
- [Local HTTP server docs](docs/local-http.md)

## Local HTTP Server

The optional local server exposes recent notifications and logs on your LAN.

Default port:

```text
8787
```

Example:

```bash
curl "http://192.168.1.25:8787/recent?limit=25"
```

If bearer-token auth is enabled:

```bash
curl -H "Authorization: Bearer <token>" \
  "http://192.168.1.25:8787/recent?limit=25"
```

## Build

```bash
./gradlew assembleFossDebug
```

Release builds require a local signing key:

```bash
KEYSTORE_PASSWORD=... KEY_ALIAS=... KEY_PASSWORD=... ./gradlew assembleFossRelease
```

## Privacy

The app does not send notifications anywhere except the webhook URLs you configure. Webhook settings, recent notifications, and logs are stored locally on the device. If you enable the local HTTP server, use it only on trusted networks or enable bearer-token auth.

## License

AGPL-3.0-only. See [LICENSE](LICENSE).
