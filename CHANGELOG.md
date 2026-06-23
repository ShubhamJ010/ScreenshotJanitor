# Changelog

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
