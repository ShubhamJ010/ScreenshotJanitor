# Notifications

## Flow

```mermaid
flowchart TD
    A[Screenshot Captured] --> B[Notification Appears]
    B --> C{User Action}
    C -->|Archive| D[Mark for Cleanup]
    C -->|Keep| E[Preserve]
    C -->|Delete| F[Immediate Delete]
    D --> G[Database Updated]
    E --> G
    F --> G
```

## Implementation

| Component | File | Role |
|---|---|---|
| Notification Manager | `notifications/ScreenshotNotificationManager.kt` | Creates and displays notifications with action buttons |
| Action Receiver | `notifications/NotificationActionReceiver.kt` | Handles user tap on notification actions, updates DB |

## Notification Behavior

- **Heads-up notification** — appears prominently when a new screenshot is detected.
- **Dismissible** — swiping away the notification does not affect the screenshot.
- **Auto-Archive mode** — when enabled, notification offers "Keep" and "Delete Now" instead of "Archive" and "Keep".

## Channels

A dedicated notification channel is configured during app initialization for screenshot alerts.
