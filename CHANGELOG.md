# Changelog

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
