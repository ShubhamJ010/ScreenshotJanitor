# Development

## Principles

- Keep the codebase small.
- Prefer platform-native APIs over third-party libraries.
- Avoid overengineering and unnecessary abstractions.
- Prioritize reliability over features.
- Battery efficiency is a core feature.

## Performance Goals

| Area | Target |
|---|---|
| Memory | Minimal background usage, no permanent daemon processes |
| Battery | No filesystem polling, lightweight foreground service with minimal uptime, WorkManager for scheduled tasks |
| Startup | Fast cold startup, lightweight dependency graph |

## Android Version Support

- **Android 14+** (min SDK 34)
- Older versions intentionally unsupported to simplify storage handling, permission management, background execution, and maintenance.

## MVP Scope (v1.0)

### Included
- Screenshot detection via ContentObserver with URI-based querying, cold-start scan, retry logic, and `IS_PENDING` filtering
- Foreground detection service (ScreenshotDetectionService) with BootReceiver for reboot recovery
- Action notifications (Archive, Keep, Delete)
- Archive system (Room-based metadata)
- Auto-Archive mode (long-press toggle)
- Daily cleanup worker (WorkManager)
- Battery optimization opt-out card
- Settings screen
- History screen

### Excluded
- Cloud sync
- AI categorization / OCR
- Backup systems
- Analytics / tracking
- Multi-device sync
- Account system

## Future Ideas

Out of scope for MVP but considered for later:

- OCR search using ML Kit
- Auto categorization
- Smart cleanup rules
- Export archived screenshots
- Per-folder retention rules

## Material 3 Expressive Guidelines

**Do:**
- Dynamic color (Material You)
- Edge-to-edge layouts
- Large touch targets
- Rounded surfaces
- Minimal visual clutter

**Avoid:**
- Heavy animations
- Dashboard-heavy layouts
- Excessive settings
- Unnecessary screens

The app should feel like a native Android utility.

## Testing Status

| Area | Status |
|---|---|
| Screenshot detection | ❌ Not tested |
| Notification actions | ❌ Not tested |
| Cleanup reliability | ❌ Not tested |
| Battery impact | ❌ Not tested |
| Android 14 behavior | ✅ Verified |
| Process death recovery | ❌ Not tested |

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

No API keys, no configuration, no external services required.
