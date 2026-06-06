# Screenshot Janitor

Minimal Android 14+ screenshot management utility built with Kotlin and Jetpack Compose.

Screenshot Janitor monitors newly created screenshots, allows users to archive or delete them through lightweight notifications, and automatically cleans up unarchived screenshots on a schedule.

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
Archived screenshots are protected from automatic cleanup.

The app stores metadata locally using Room instead of physically moving files.

---

## Automatic Cleanup
A scheduled cleanup worker removes:
- non-archived screenshots
- older than the configured retention duration

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

xml <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" /> <uses-permission android:name="android.permission.POST_NOTIFICATIONS" /> 

---

# Architecture

text MediaStore Observer         ↓ Screenshot Detection         ↓ Notification Actions         ↓ Room Database         ↓ Cleanup Worker 

The application follows a lightweight event-driven architecture.

The app remains idle most of the time and only wakes when:
- a screenshot is created
- cleanup execution is scheduled

---

# Project Structure

```
ScreenshotJanitor/
│
├── app/
│   ├── src/main/
│   │
│   ├── java/com/shubham/screenshotjanitor/
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
│   └── ScreenshotJanitorApp.kt
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

kotlin @Entity(tableName = "screenshots") data class ScreenshotEntity(     @PrimaryKey     val uri: String,      val fileName: String,      val createdAt: Long,      val archived: Boolean = false,      val deleted: Boolean = false ) 

---

# Notification Flow

text Screenshot Captured         ↓ Notification Appears         ↓ User Action: - Archive - Keep - Delete         ↓ Database Updated 

---

# Cleanup Flow

text Scheduled Worker Executes         ↓ Fetch Old Unarchived Screenshots         ↓ Delete From MediaStore         ↓ Update Database State 

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
- [ ] Create Android project
- [ ] Configure Material 3
- [ ] Configure edge-to-edge UI
- [ ] Add Room dependencies
- [ ] Add WorkManager dependencies
- [ ] Configure notification channels

---

## Phase 2 — Storage & Detection
- [ ] Implement MediaStore query utilities
- [ ] Implement screenshot detection logic
- [ ] Add ContentObserver
- [ ] Filter screenshot folders
- [ ] Validate duplicate detection handling

---

## Phase 3 — Notification System
- [ ] Create notification actions
- [ ] Implement Archive action receiver
- [ ] Implement Keep action receiver
- [ ] Implement Delete action receiver
- [ ] Add heads-up notification behavior

---

## Phase 4 — Database Layer
- [ ] Create ScreenshotEntity
- [ ] Create DAO
- [ ] Create Room database
- [ ] Implement repository layer

---

## Phase 5 — Cleanup Worker
- [ ] Create periodic WorkManager task
- [ ] Query expired screenshots
- [ ] Delete from MediaStore
- [ ] Update database state
- [ ] Add retry handling

---

## Phase 6 — UI
- [ ] Create Home screen
- [ ] Create Settings screen
- [ ] Create History screen
- [ ] Implement Material 3 expressive design
- [ ] Add dynamic color support

---

## Phase 7 — Testing
- [ ] Test screenshot detection
- [ ] Test notification actions
- [ ] Test cleanup reliability
- [ ] Test battery impact
- [ ] Test Android 14 behavior
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