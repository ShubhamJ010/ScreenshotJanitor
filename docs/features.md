# Features

## Screenshot Detection

Detect newly created screenshots using `MediaStore` and `ContentObserver`.

- Supports Android 14+ scoped storage model.
- Event-driven architecture — no continuous polling.
- **URI-based detection** — queries by content URI ID instead of bulk-scanning latest images, minimizing read overhead.
- **Cold-start handling** — `performInitialScan()` catches screenshots taken during app startup; `scanLatestScreenshots()` fallback handles edge cases where `onChange` fires before MediaStore creates the row.
- **`IS_PENDING` filtering** — skips rows still being written by MediaStore.
- **Exponential-backoff retry** — up to ~10.3s retry window (200ms → 2s × 3) for URI queries to handle MediaStore indexing delay on fresh process.
- **Blank column detection** — retries when `DISPLAY_NAME` or `RELATIVE_PATH` are empty (the root cause of false-positive cold-start misses).
- **Deduplication** — synchronized sets with 200-entry cap and oldest-25% eviction to prevent unbounded memory growth.
- Filters images by screenshot naming/path conventions.

**Implementation:** `observer/ScreenshotContentObserver.kt` · `observer/ScreenshotDetector.kt`

---

## Battery Optimization Opt-Out

A dedicated permission card allows the user to disable battery optimization for the app with a single tap.

- Separated from the general permission warning card in `0.4.1-alpha`.
- Ensures reliable background screenshot detection on devices with aggressive battery management.
- Declares `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission.

**Implementation:** `ui/components/PermissionWarningCard.kt`

---

## Action Notifications

After a screenshot is detected, the user receives a notification with quick actions:

| Action | Effect |
|---|---|
| **Archive** | Marks screenshot for automatic cleanup by the Janitor |
| **Keep** | Preserves screenshot — excluded from cleanup |
| **Delete** | Immediately deletes from device storage |

Notifications are intentionally dismissible and non-intrusive.

**Implementation:** `notifications/ScreenshotNotificationManager.kt` · `notifications/NotificationActionReceiver.kt`

---

## Archive System

The app stores metadata locally using Room instead of physically moving files.

- Archived screenshots are marked for automatic cleanup.
- Screenshots marked as "Keep" are preserved.
- The Janitor processes archived entries on its next scheduled run.

**Implementation:** `data/repository/ScreenshotRepository.kt`

---

## Auto-Archive Mode

For power users who want to cleanup everything by default.

| Aspect | Detail |
|---|---|
| **Toggle** | Long-press the "Archived" card on the Home screen |
| **Behavior** | Every new screenshot is automatically marked as "Archived" upon detection |
| **Indicator** | An "AUTO" badge appears on the Archived stats card when active |
| **Notifications** | Reflect auto-archived status — offers "Keep" or "Delete Now" |

**Implementation:** `ui/screens/home/` · `data/repository/SettingsRepository.kt`

---

## Automatic Cleanup

A scheduled WorkManager worker removes archived screenshots from device storage.

- Runs daily via `WorkManager` periodic task.
- Queries Room for archived screenshots.
- Deletes from `MediaStore` (Scoped Storage compliant).
- Updates database state on completion.
- Includes retry handling for failed deletions.
- Also identifies unarchived screenshots beyond retention period for cleanup recommendations.

**Implementation:** `worker/ScreenshotCleanupWorker.kt`
