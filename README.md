# ssJanitor

Minimal Android 14+ screenshot management utility built with Kotlin and Jetpack Compose.

ssJanitor monitors newly created screenshots, allows users to archive or delete them through lightweight notifications, and automatically cleans up unarchived screenshots on a schedule.

The app is intentionally lightweight, battery-friendly, and fully aligned with modern Android storage and background execution policies.

---

# Features

## Screenshot Detection
- Detect newly created screenshots using MediaStore and ContentObserver
- Supports Android 14+ scoped storage model
- Event-driven architecture with no continuous polling

---

## Lightweight Action Notifications
After a screenshot is detected, the user receives a notification with quick actions:

- Archive
- Keep
- Delete

Notifications are intentionally dismissible and non-intrusive.

---

## Archive System
Archived screenshots are marked for automatic cleanup by the Janitor.

Screenshots the user wants to preserve should be marked as "Keep".

The app stores metadata locally using Room instead of physically moving files.

---

## Auto-Archive Mode
For power users who want to cleanup everything by default:
- **Toggle**: Long-press the "Archived" card on the Home screen to toggle Auto-Archive mode.
- **Behavior**: When enabled, every new screenshot is automatically marked as "Archived" upon detection.
- **Visual Indicator**: An "AUTO" badge appears on the Archived stats card when active.
- **Smart Notifications**: Notifications reflect the auto-archived status, offering "Keep" or "Delete Now" as primary actions.

---

## Automatic Cleanup
A scheduled cleanup worker removes:
- archived screenshots
- or recommends cleanup for old unarchived screenshots

Powered by WorkManager for battery-efficient background execution.

---

# Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Design System | Material 3 Expressive |
| Local Database | Room |
| Background Tasks | WorkManager |
| Storage APIs | MediaStore |
| Notifications | NotificationCompat |
| Architecture | MVVM-lite |

---

# Design Goals

- Tiny APK size
- Minimal memory usage
- Battery efficient
- Android-native behavior
- No unnecessary services
- No cloud dependency
- No analytics or tracking
- No account system

---

# Android Version Support

- Android 14+
- Min SDK: 34

Older Android versions are intentionally unsupported to simplify:
- storage handling
- permission management
- background execution
- maintenance overhead

---

# Permissions

## Required Permissions

```xml
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" tools:ignore="ScopedStorage" />
```

---

# Architecture

```text
MediaStore Observer
        ↓
Screenshot Detection
        ↓
Notification Actions
        ↓
Room Database
        ↓
Cleanup Worker
```

The application follows a lightweight event-driven architecture.

The app remains idle most of the time and only wakes when:
- a screenshot is created
- cleanup execution is scheduled

---

# Project Structure

```
ssJanitor/
│
├── app/
│   ├── src/main/
│   │
│   ├── java/com/example/screenshotjanitor/
│   │
│   ├── core/
│   │   ├── constants/
│   │   ├── extensions/
│   │   ├── utils/
│   │   └── logger/
│   │
│   ├── data/
│   │   ├── db/
│   │   │   ├── dao/
│   │   │   ├── entity/
│   │   │   └── AppDatabase.kt
│   │   │
│   │   ├── model/
│   │   │
│   │   ├── repository/
│   │   │   └── ScreenshotRepository.kt
│   │   │
│   │   └── datastore/
│   │
│   ├── domain/
│   │   ├── model/
│   │   └── usecase/
│   │
│   ├── observer/
│   │   ├── ScreenshotContentObserver.kt
│   │   └── ScreenshotDetector.kt
│   │
│   ├── notifications/
│   │   ├── receiver/
│   │   │   └── NotificationActionReceiver.kt
│   │   │
│   │   ├── manager/
│   │   │   └── ScreenshotNotificationManager.kt
│   │   │
│   │   └── channel/
│   │
│   ├── worker/
│   │   └── ScreenshotCleanupWorker.kt
│   │
│   ├── ui/
│   │   ├── screens/
│   │   │   ├── home/
│   │   │   ├── settings/
│   │   │   └── history/
│   │   │
│   │   ├── components/
│   │   ├── theme/
│   │   └── navigation/
│   │
│   ├── viewmodel/
│   │   ├── HomeViewModel.kt
│   │   ├── SettingsViewModel.kt
│   │   └── HistoryViewModel.kt
│   │
│   ├── service/
│   │   └── ScreenshotMonitorService.kt
│   │
│   ├── MainActivity.kt
│   │
│   └── SsJanitorApp.kt
│   │
│   ├── res/
│   └── AndroidManifest.xml
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```
---

# Database Schema

kotlin
@Entity(tableName = "screenshots")
data class ScreenshotEntity(
    @PrimaryKey
    val uri: String,
    val fileName: String,
    val createdAt: Long,
    val archived: Boolean = false,
    val deleted: Boolean = false,
    val kept: Boolean = false
)

---

# Notification Flow

```text
Screenshot Captured
        ↓
Notification Appears
        ↓
User Action:
- Archive (Mark for Cleanup)
- Keep (Preserve)
- Delete (Immediate)
        ↓
Database Updated
```

---

# Cleanup Flow

```text
Scheduled Worker Executes
        ↓
Fetch Archived Screenshots
        ↓
Delete From MediaStore
        ↓
Update Database State
```

---

# Material 3 Expressive Guidelines

The UI should follow:
- dynamic color
- edge-to-edge layouts
- large touch targets
- rounded surfaces
- minimal visual clutter

Avoid:
- heavy animations
- dashboard-heavy layouts
- excessive settings
- unnecessary screens

The app should feel like a native Android utility rather than a productivity suite.

---

# Performance Goals

## Memory
- Minimal background memory usage
- No permanent daemon processes

## Battery
- No filesystem polling
- No abusive foreground services
- WorkManager-only scheduled execution

## Startup
- Fast cold startup
- Lightweight dependency graph

---

# MVP Scope

## Version 1.0

### Included
- Screenshot detection
- Action notifications
- Archive support
- Auto-Archive mode (Toggle via long-press on Archive card)
- Daily cleanup worker
- Settings screen
- History screen

### Excluded
- Cloud sync
- AI categorization
- OCR
- Backup systems
- Analytics
- Multi-device sync

---

# Future Ideas

Potential future improvements:
- OCR search using ML Kit
- Auto categorization
- Smart cleanup rules
- Export archived screenshots
- Per-folder retention rules

These are intentionally out of scope for the MVP.

---

# Implementation TODO

## Phase 1 — Project Setup
- [x] Create Android project
- [x] Configure Material 3
- [x] Configure edge-to-edge UI
- [x] Add Room dependencies
- [x] Add WorkManager dependencies
- [x] Configure notification channels

---

## Phase 2 — Storage & Detection
- [x] Implement MediaStore query utilities
- [x] Implement screenshot detection logic
- [x] Add ContentObserver
- [x] Filter screenshot folders
- [x] Validate duplicate detection handling

---

## Phase 3 — Notification System
- [x] Create notification actions
- [x] Implement Archive action receiver
- [x] Implement Keep action receiver
- [x] Implement Delete action receiver
- [x] Add heads-up notification behavior

---

## Phase 4 — Database Layer
- [x] Create ScreenshotEntity
- [x] Create DAO
- [x] Create Room database
- [x] Implement repository layer

---

## Phase 5 — Cleanup Worker
- [x] Create periodic WorkManager task
- [x] Query expired screenshots
- [x] Delete from MediaStore
- [x] Update database state
- [x] Add retry handling

---

## Phase 6 — UI
- [x] Create Home screen (showing tracked screenshots, stats, and next scheduled cleanup time)
- [x] Implement Auto-Archive toggle (long-press on Archived card)
- [ ] Create Settings screen
- [ ] Create History screen
- [x] Implement Material 3 expressive design
- [x] Add dynamic color support

---

## Phase 7 — Testing
- [ ] Test screenshot detection
- [ ] Test notification actions
- [ ] Test cleanup reliability
- [ ] Test battery impact
- [x] Test Android 14 behavior
- [ ] Test process death recovery

---

# Development Principles

- Keep the codebase small
- Prefer platform-native APIs
- Avoid overengineering
- Avoid unnecessary abstractions
- Prioritize reliability over features
- Battery efficiency is a core feature

---

# License

MIT License