# Changelog

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