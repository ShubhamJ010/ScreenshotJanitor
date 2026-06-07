# ScreenshotJanitor Architecture

ScreenshotJanitor is an Android application designed to help users manage their screenshots by automatically detecting new captures, providing quick actions via notifications, and performing periodic cleanups of unwanted images.

---

## High-Level Architecture

The project follows the **MVVM (Model-ViewModel-View)** architectural pattern and utilizes a layered approach to separate concerns.

### Architectural Diagram (Conceptual)

```text
[ User Interface (Compose) ]
          ^  |
          |  v
[ HomeViewModel ]
          ^  |
          |  v
[ ScreenshotRepository ] <------------------- [ ScreenshotCleanupWorker (WorkManager) ]
          ^  |                                      |
          |  +------> [ Room Database (Local) ] <---+
          |                                         |
          +------> [ MediaStore (Device Storage) ] <+
                      ^
                      |
[ ScreenshotDetector (ContentObserver) ] ----> [ ScreenshotNotificationManager ]
                                                      |
                                                      v
                                        [ NotificationActionReceiver (Broadcast) ]
```

---

## Layers and Components

### 1. Presentation Layer (UI)
- **Jetpack Compose**: Used for building the entire UI.
- **HomeViewModel**: Manages the state of the home screen, providing a stream of screenshots from the repository.
- **Material 3**: Implementation of Material Design 3 for theming and components.

### 2. Data Layer
- **Room Database**: Acts as the "Source of Truth" for screenshot metadata (URI, file name, creation date, status: archived/kept/deleted).
    - `ScreenshotEntity`: The data model.
    - `ScreenshotDao`: Interface for database operations.
- **ScreenshotRepository**: Orchestrates data flow between the Room database and the Android MediaStore. It handles logic for:
    - Reconciling local DB with device storage.
    - Creating deletion requests (Scoped Storage compatible).
    - Updating screenshot status (Archive/Keep).
- **SettingsRepository**: Manages user preferences (e.g., auto-archive toggle).

### 3. Background & System Interaction
- **ScreenshotDetector (Observer Pattern)**: 
    - Uses a `ContentObserver` registered to `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`.
    - Automatically identifies new images that fit the screenshot naming or path convention.
- **ScreenshotNotificationManager**: 
    - Handles displaying notifications when a new screenshot is detected or when a cleanup is recommended/performed.
- **NotificationActionReceiver**: 
    - A `BroadcastReceiver` that handles user clicks on notification actions (Keep, Archive).
- **ScreenshotCleanupWorker (WorkManager)**: 
    - A periodic background task that identifies "archived" screenshots and attempts to delete them from device storage to free up space.

---

## Key Processes & Workflows

### A. Screenshot Detection Flow
1. **Detection**: `ScreenshotContentObserver` detects a change in MediaStore.
2. **Filtering**: It queries the latest image and checks if it's a screenshot (based on name or path).
3. **Storage**: The screenshot is recorded in the `AppDatabase`.
4. **Interaction**: A notification is shown to the user with "Archive" (prepare for deletion) and "Keep" (save) actions.

### B. Cleanup Workflow
1. **Scheduling**: `ScreenshotCleanupWorker` is scheduled via WorkManager.
2. **Identification**: The worker queries the repository for screenshots marked as `archived` but not `kept` or `deleted`.
3. **Execution**: 
    - On Android 10+ (API 29+), it attempts `deleteScreenshotsDirectly`.
    - If direct deletion fails (due to Scoped Storage permissions), it notifies the user to perform a batch deletion.
4. **Sync**: Upon successful deletion, the database is updated to reflect the `deleted` status.

### C. Data Reconciliation
- Periodically, the repository checks if files in the database still exist in MediaStore.
- If a file was deleted externally (e.g., by the user in the Gallery app), the repository marks it as `deleted` in the local DB to maintain consistency.

---

## Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose, Material 3
- **Local DB**: Room
- **Async**: Kotlin Coroutines & Flow
- **Background Tasks**: WorkManager
- **OS Integration**: MediaStore API, ContentObservers, BroadcastReceivers
- **Dependency Injection**: Manual (Constructor Injection)
