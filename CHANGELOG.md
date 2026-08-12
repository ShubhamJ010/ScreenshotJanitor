# Changelog

## [1.1.3] - 2026-08-13

### Added
- **Drag haptic feedback** — Added 40px drag tick haptic feedback during pull-to-reveal gesture and restored `CONFIRM` threshold feedback when crossing the trigger limit.
- **Auto Mode off heads-up notification** — Added heads-up notification when Auto Mode / Janitor background detection is toggled off.

## [1.1.2] - 2026-08-11

### Performance
- **Pull-to-reveal gesture optimization** — Replaced 120Hz coroutine dispatches during touch scroll processing (`onPostScroll`/`onPreScroll`) with direct float state updates (`rawPullOffset`), eliminating input latency spikes and coroutine object allocation churn.
- **Gesture release & layout stability** — Removed `Modifier.animateItem()` and structural height expand/collapse from `PullToKeptIndicator`, using `graphicsLayer` translation & alpha for 100% smooth 60fps spring release animations without layout jumps.

### Changed
- **Compact count formatting** — Stats cards (Pending, Archived, Kept, Cleaned) and section headers now format counts using compact `1K`, `1.5K`, `10K`, `1.2M`, `1B` notation (`formatCompactCount`).
- **Kept stat card click action** — Tapping or long-pressing the **Kept** stat card reveals the kept screenshots list and smoothly animates the scroll position directly to the "Kept Screenshots" header.

## [1.1.1] - 2026-07-11

### Fixed
- **Accidental preview triggers** — Holding a thumbnail no longer opens the full-screen preview on a quick tap or a brush-past. A `HOLD_DURATION_MS` (350ms) hold delay now guards the trigger, during which a blur overlay quickly fades in over the thumbnail as a visual hold cue. Implemented in `ScreenshotThumbnail`.
- **Swiping cancelled the preview** — Moving or swiping the finger while holding used to end the hold early (the scrollable list consumed the gesture). The hold now persists through any finger movement and ends **only when the finger is lifted** (or leaves the window). Implemented in `ScreenshotThumbnail`.
- **List scrolled behind the preview** — While the full-screen preview was open, moving the still-held finger scrolled the list underneath it. User scrolling (and the pull-to-reveal gesture) is now disabled while the preview is visible. Implemented via an `isPreviewOpen` flag threaded from `HomeScreen` into `HomeContent`'s `LazyColumn` (`userScrollEnabled`).

## [1.1.0] - 2026-07-09

### Added
- **Janitor On/Off toggle** — Long-press the **Pending** stat card to toggle the Janitor background monitor on or off. When off, a Material 3 Expressive red **"OFF" stamp** appears on the Pending card, the background detection service is stopped (the squiggly rotating indicator in **Next Scheduled Cleanup** also freezes), and a snackbar confirms the state. The choice persists across restarts and reboots. Implemented via `HomeViewModel.toggleJanitor()`, `SettingsRepository`, `SsJanitorApp.stopDetectionService()`, the new `OffBadge`, and an `isActive` flag on `NextCleanupBanner`.
- **Material 3 container-transform hold-to-preview** — Reworked the hold-to-preview gesture into a true Material 3 container transform: an expressive colored blur blooms over the thumbnail the instant the hold starts (with a long-press haptic), and the thumbnail's bounds morph into a floating, centred preview card that lifts off the list with a growing shadow. The preview is decoded at full quality (sampled to the device's screen resolution) so it stays sharp and hovers above the app with the system bars visible. Releasing (or scrolling away) reverses the morph back into the thumbnail. Implemented in `ScreenshotThumbnail`, `ScreenshotCard`, `HomeContent`/`HomeScreen` (rect capture), and the rewritten `ScreenshotPreviewOverlay`.

### Changed
- **Docs** — Updated `docs/features.md` to describe the new hold-to-preview container transform and remove the old growing-ring animation description.

## [1.0.1] - 2026-07-09

### Added
- **Hold-to-preview thumbnails** — Press and hold any screenshot thumbnail to reveal a Material-expressive squircle border that thickens as the hold progresses; at 0.9s it fires a long-press haptic and opens a minimal, borderless full-screen preview. Releasing (or scrolling away) closes it. Both open and close are animated (scale + fade), and the preview is immersive (system bars hidden). Implemented in `ScreenshotThumbnail` (gesture + growing border) and the new `ScreenshotPreviewOverlay`.
- **Pre-cleanup heads-up reminder** — A high-priority (heads-up) notification titled "Janitor is on the way" fires 30 minutes before the scheduled daily cleanup, warning the user that archived screenshots will be auto-deleted soon. Only shown when there are screenshots actually pending deletion, with **Review** and **Clean up now** actions.
- **Default scheduled cleanup time** — Cleanup (and its reminder) now default to **11:30 PM in the device's local timezone**; the chosen time is persisted in `SettingsRepository`.
- **`CleanupScheduler`** — Shared scheduling helper that keeps the daily cleanup worker and the pre-cleanup reminder worker aligned across reschedules and reboots.
- **`CleanupReminderWorker`** — Worker that triggers the heads-up reminder; runs daily 30 minutes before cleanup.
- **`ssjanitor_reminder_channel`** — New `IMPORTANCE_HIGH` notification channel for the heads-up cleanup warning.

## [1.0.0] - 2026-06-27

### Changed
- **Package renamed** — Application ID changed from `com.example.screenshotjanitor` to `dev.sj010.ssjanitor`
- **README improved** — Added Install section with GitHub Releases, Obtainium, IzzyOnDroid, and F-Droid badges
- **Obtainium support** — One-tap deep-link to add app directly from GitHub Releases
- **Version bumped** to 1.0.0 (stable public release)

## [0.5.0-alpha] - 2026-06-26

### Added
- **Pull-to-reveal kept screenshots gesture** — New `NestedScrollPullToRevealState` + `PullToKeptIndicator` composable for swipe-down gesture to reveal kept screenshot section
- **`KeptScreenshotsSection`** — Dedicated section composable to display kept screenshots pulled down from the gesture
- **`PermissionWarningSection`** — Extracted permission warnings from `HomeContent` into its own composable for cleaner delegation
- **`SectionHeader`** — Reusable header composable for home screen sections
- **`EmptyStateView`** — New animated empty state with entrance transition (opacity + slide-in)

### Changed
- **Refactored home screen package structure** — Moved components into organized subpackages: `common/`, `screenshot/`, `stats/`, `permissions/`, `gesture/`
- **Simplified `HomeContent`** — ~540 lines reduced to ~55 lines by delegating to extracted composables (`EmptyStateView`, `StatsGrid`, `KeptScreenshotsSection`, `PermissionWarningSection`, `NextCleanupBanner`)
- **`StatsGrid` redesigned** — "Last cleared" card now shows relative time (e.g. "2h ago") with elapsed counter, compact layout, and auto-archive badge; added days-since counters for total and daily cleanup stats
- **Animation polish** — Empty state fade-in/slide-up entrance animation; auto-archive badge entrance animation
- **`ScreenshotContentObserver`** — Removed dead `AutoStartUtil` dependency and unused retry constants
- **Documentation** — Synced `README`, `docs/architecture.md`, `docs/features.md`, `docs/development.md` with 0.4.x changelog and removed stale `AutoStartUtil` references

### Removed
- **`AutoStartUtil`** — Removed OEM-specific auto-start settings intents (Xiaomi, Huawei, OPPO, vivo, OnePlus, Samsung) and all related permission request callbacks
- **Old `components/` directory** — Replaced by `common/`, `screenshot/`, `stats/`, `permissions/`, `gesture/` subpackages

## [0.4.2-alpha] - 2026-06-24

### Fixed
- **Cold-start screenshot detection race** — Complete rewrite of `ScreenshotContentObserver` to handle MediaStore cold-start indexing delay on fresh app process:
  - Extended URI-based retry window from 1.5s to ~10.3s with exponential backoff (200ms → 2s × 3) so MediaStore has time to populate `DISPLAY_NAME` and `RELATIVE_PATH` columns on first launch
  - Added `IS_PENDING` column check — skips rows that MediaStore has not finished writing, returning `false` for retry instead of giving up
  - Added blank column detection — returns `false` for retry when `displayName` or `relativePath` are empty (the root cause: old code returned `true` and marked the URI as processed before columns were populated)
  - Added `performInitialScan()` — scans the last 30 seconds of MediaStore immediately after observer registration to catch screenshots taken during the app startup window
  - Added `scanLatestScreenshots()` fallback — scans the last 60 seconds of MediaStore when URI-based retries are exhausted (handles edge cases where `onChange` fires before MediaStore creates the row at all)
  - Null/parse-error `onChange` URI now falls back to scanning recent images instead of silently dropping the event
  - `handleNewScreenshot` converted from fire-and-forget coroutine to `suspend` function so callers wait for DB insert + notification before marking the URI as processed
  - Dedup set capped at 200 entries with oldest-25% eviction to prevent unbounded memory growth

### Changed
- `ScreenshotDetector.startDetector()` now calls `performInitialScan()` on the observer right after registration

## [0.4.1-alpha] - 2026-06-24

### Changed
- Extracted battery optimization opt-out from `PermissionWarningCard` into its own dedicated card with standalone "Battery Usage" button
- `PermissionWarningCard` — simplified to only handle storage, notification, and all-files-access permissions

### Removed
- Auto-start permission request flow (removed `onRequestAutoStart` callback and `AutoStartUtil` dependency from HomeScreen)

### Fixed
- `ScreenshotContentObserver.onChange` return value — returns `true` only when a screenshot is actually detected, `false` otherwise (prevented false-positive triggering of downstream handlers)

## [0.4.0-alpha] - 2026-06-23

### Added
- URI-based screenshot detection in `ScreenshotContentObserver` (query by content URI ID instead of scanning latest)
- Deduplication with synchronized `processedUris`/`pendingUris` sets to prevent duplicate processing
- Retry logic with `queryByIdWithRetry` (3 attempts with 500ms delay) to handle MediaStore index race
- `clearProcessedUris()` lifecycle hook called after cleanup work completes
- `goAsync()`/`pendingResult.finish()` pattern in `BootReceiver` and `NotificationActionReceiver` for proper BroadcastReceiver lifecycle
- `contentObserver` reference on `SsJanitorApp` to enable cleanup worker to reset dedup state
- `notificationManager` lifecycle management in `ScreenshotDetectionService` (dismiss on destroy)
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission declaration for battery optimization opt-out
- Battery optimization check with re-evaluation on lifecycle resume
- `AutoStartUtil` — OEM-specific auto-start settings intents (Xiaomi, Huawei, OPPO, vivo, OnePlus, Samsung)
- `EmptyStateView` subtitle parameter for multi-line empty state messages

### Changed
- Switched `SsJanitorApp` database/repository/settings from `lateinit` to `by lazy` for thread-safe initialization
- Refactored `ScreenshotDetectionService.onCreate()` to start foreground in `onCreate` (moved out of helper method)
- Simplified `ScreenshotCleanupWorker` — removed dead `autoDelete = true` branch; always deletes archived screenshots
- Shared single `CoroutineScope(Dispatchers.IO)` in `ScreenshotContentObserver` instead of creating per-screenshot scopes
- `BootReceiver` `exported` attribute set to `true` for reliable `BOOT_COMPLETED` delivery on modern Android
- Permission warning card now includes "No Kill" and "Auto Start" action buttons
- Empty state message split into separate lines with kept count subtitle
- Animated stats grid entrance and extracted auto-archive badge
- Tracked file size for screenshots with freed-up bytes display

### Removed
- All `Log.d`/`Log.e`/`Log.w` statements across the codebase (production code cleanup)
- `ScreenshotDetector` stale-run guard log messages

### Fixed
- `ContentUris.parseId()` crash path — wrapped in try/catch for `NumberFormatException`
- `queryByIdWithRetry` — fixed to 3 total attempts (was 4 due to fallthrough)
- `BootReceiver` — missing `goAsync()` which could cause ANR on boot
- `NotificationActionReceiver` — missing `goAsync()` which could cause ANR

## [0.3.1-alpha] - 2026-06-10

### Added
- `ScreenshotDetectionService` for reliable background monitoring
- `BootReceiver` to restart detection after device reboot
- Foreground service support with dedicated notification channel
- `FOREGROUND_SERVICE_SPECIAL_USE` permission for screenshot monitoring

### Changed
- Refactored `SsJanitorApp` to use the background service instead of direct `ScreenshotDetector`
- Updated notification manager to support service notifications

## [0.3.0-alpha] - 2026-06-10

### Changed
- AVD: Update scale and align lid pivot for `avd_auto_delete.xml`

## [0.2.0-alpha] - 2026-06-09

### Added
- Documentation split into `docs/` with separation of concerns
  - `docs/architecture.md` — MVVM layers, process flows, component details
  - `docs/features.md` — Detailed feature descriptions
  - `docs/database.md` — Room entities, DAO, repository
  - `docs/notifications.md` — Notification flow & action handling
  - `docs/cleanup.md` — WorkManager-based cleanup pipeline
  - `docs/development.md` — Principles, design goals, MVP scope
- Standard GitHub README replacing monolithic README

### Removed
- Root `architecture.md` (superseded by `docs/architecture.md`)

## [0.1.0-alpha] - 2026-06-09

### Added
- Material You splash screen support
- Adaptive icon support for Android
- Architecture documentation for ScreenshotJanitor
- Project structure refactored and renamed to ScreenshotJanitor
- GitHub workflows configuration

### Changed
- Refactored icon resources
- Updated splash screen theme for Material You support
- Updated notification icons

### Fixed
- Icon and splash screen issues