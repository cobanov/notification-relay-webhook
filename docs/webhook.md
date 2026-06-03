# Webhook Payload

Notification Relay sends one JSON object per captured notification.

## Request

- Method: `POST`
- Content type: `application/json; charset=utf-8`
- Destination: every enabled webhook URL
- Headers: custom headers configured for that webhook

Delivery is attempted in parallel across enabled webhook URLs. Each URL is retried up to 3 times for retryable failures such as network errors, timeouts, DNS failures, and HTTP `5xx` responses.

## Payload Fields

| Field | Type | Description |
| --- | --- | --- |
| `package` | string | Android package name of the app that posted the notification. |
| `packageName` | string | Same value as `package`, included for camelCase consumers. |
| `app` | string | User-facing app label. |
| `appName` | string | Same value as `app`, included for camelCase consumers. |
| `title` | string or null | Notification title. |
| `text` | string or null | Notification body text. |
| `sub_text` | string or null | Notification sub text, if Android provides it. |
| `big_text` | string or null | Expanded notification text, if Android provides it. |
| `category` | string or null | Android notification category, such as `msg`, `status`, or `alarm`. |
| `post_time` | string | Notification post time as ISO-8601 UTC. |
| `postedAt` | string | Same value as `post_time`, included for camelCase consumers. |
| `key` | string | Android notification key. |
| `notificationKey` | string | Same value as `key`, included for camelCase consumers. |
| `ongoing` | boolean | Whether Android marks the notification as ongoing. |
| `group_summary` | boolean | Whether Android marks the notification as a group summary. |
| `test` | boolean | Present only on test deliveries sent from the UI. |

## Example

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

## Test Payload

The Test button sends the same shape with `test: true`:

```json
{
  "package": "com.notifrelay.app",
  "packageName": "com.notifrelay.app",
  "app": "Notification Relay",
  "appName": "Notification Relay",
  "title": "Test notification",
  "text": "This is a test webhook delivery.",
  "sub_text": null,
  "big_text": null,
  "category": "status",
  "post_time": "2026-06-03T12:34:56Z",
  "postedAt": "2026-06-03T12:34:56Z",
  "key": "test-2026-06-03T12:34:56Z",
  "notificationKey": "test-2026-06-03T12:34:56Z",
  "ongoing": false,
  "group_summary": false,
  "test": true
}
```

## Delivery Status

Recent notification history tracks each captured notification as:

- `queued`: waiting for manual send or retry.
- `sent`: delivered successfully to at least one enabled webhook.
- `failed`: delivery attempted but failed.
- `ignored`: captured but filtered out by app filters or ignore settings.

Webhook logs store the endpoint URL with sensitive query parameters redacted, status code, success flag, source package, response time, error message, and a truncated payload snapshot.
