# Notification Relay Webhook

An Android app that forwards your incoming phone notifications to your own webhook URLs as JSON. Built around an Android `NotificationListenerService`, with a Jetpack Compose UI, delivery logs, and an optional local HTTP server.

It reuses the proven webhook-delivery and local-server architecture from [health-connect-webhook](https://github.com/mcnaveen/health-connect-webhook), swapping the Health Connect data layer for notification capture.

## Features

- Capture incoming notifications via notification access and POST each one to your webhooks.
- Forward filter with three modes: **All apps**, **Allowlist**, **Blocklist**, plus toggles to ignore ongoing and group-summary notifications.
- Multiple webhook endpoints with custom headers, parallel delivery, retry with backoff.
- Delivery logs with status, timing, and redacted URLs.
- Live "Recent notifications" screen.
- Optional local HTTP server exposing recent notifications on the LAN (`/`, `/recent`, `/latest`, `/logs`, `/health`), with optional bearer-token auth.
- Settings export/import as JSON.

## Payload

Each notification is sent as a single JSON object:

```json
{
  "package": "com.whatsapp",
  "app": "WhatsApp",
  "title": "Alice",
  "text": "Hey there",
  "sub_text": null,
  "big_text": null,
  "category": "msg",
  "post_time": "2026-06-03T12:34:56Z",
  "key": "0|com.whatsapp|...",
  "ongoing": false,
  "group_summary": false
}
```

## Build

```bash
./gradlew assembleFossDebug
```

Requires JDK 17 and the Android SDK (compileSdk 35).

## Permissions

- **Notification access** (`BIND_NOTIFICATION_LISTENER_SERVICE`): granted by the user via system settings; required to read notifications.
- `INTERNET`: to deliver webhooks.
- `POST_NOTIFICATIONS`: for the app's own local-server status notification.

## Privacy

Notifications are sent only to the webhook URLs you configure. Nothing is stored on any external server. Choose exactly which apps are forwarded with the allowlist/blocklist modes.
