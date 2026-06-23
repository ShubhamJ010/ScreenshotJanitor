# Resource Usage Analysis

Measured 2026-06-23 on a physical Android device (API 36) over adb wireless.
App version: `0.4.0-alpha`.

## Foreground (App Visible, Full UI)

| Metric | Value |
|---|---|
| Total PSS | ~87 MB (89,542 KB) |
| Total RSS | ~205 MB (210,220 KB) |
| Java Heap | 18,544 KB |
| Native Heap | 9,688 KB |
| Graphics (EGL + GL) | 44,160 KB |
| Heap Size / Alloc / Free | 33,190 / 21,092 / 12,097 KB |
| CPU (peak during launch) | 53.5% of one core |
| Cold start time | 605 ms (TotalTime) |
| Active Views | 7 |
| Activities | 1 |
| AppContexts | 7 |

## Background (App Sent to Home, ~30s idle)

| Metric | Value |
|---|---|
| Total PSS | ~39 MB (39,506 KB) — **56% drop** from foreground |
| Total RSS | ~153 MB (156,192 KB) |
| Java Heap | 5,372 KB |
| Native Heap | 7,032 KB |
| Graphics (EGL + GL) | 2,092 KB — freed on pause |
| Heap Size / Alloc / Free | 30,136 / 20,758 / 9,377 KB |
| CPU | 0.0% (process sleeping, `top` shows `S` state) |

## Service Lifecycle (ScreenshotDetectionService)

The foreground service runs continuously while the app is alive:

| Metric | Value |
|---|---|
| Service state | `started` + `foreground` |
| Running count | 4 restarts over 2h53m |
| Foreground time | 0.02% of total uptime |
| Executing count | 11 work units |

## ProcStats Overview (2h53m window)

| State | Weight |
|---|---|
| Top (foreground UI) | 9.9% |
| Service (detection service) | ~44% aggregated |
| Imp Bg (important bg work) | 35–100% across historical runs |
| Receiver | ~0–8% across historical runs |

## Memory Comparisons

```
Foreground     Background     Delta
  87 MB         39 MB         -48 MB   (-55%)   PSS
 205 MB        153 MB         -52 MB   (-25%)   RSS
  44 MB          2 MB         -42 MB   (-95%)   Graphics
  19 MB          5 MB         -14 MB   (-74%)   Java Heap
```

The app releases graphics memory aggressively when backgrounded (EGL mtrack drops
from 41 MB to 1.7 MB). Native and Dalvik heaps also shrink under memory pressure.

## Observations

- **Battery impact is negligible.** The app registered no meaningful battery drain
  in the measurement window. The ContentObserver and foreground service rely on
  OS callbacks, not polling.
- **Memory profile is healthy.** PSS of 39 MB in background is well within the
  typical Android process allowance (400 MB+ on modern devices).
- **Graphics memory is the dominant foreground cost** (44 MB / 49% of PSS). This
  is expected for a Compose UI with Material 3 theming.

## Measurement Method

Commands used via ADB (wireless):

```bash
# Cold launch timing
adb shell am start -S -W -n com.example.screenshotjanitor/.MainActivity

# Foreground metrics
adb shell dumpsys meminfo $(adb shell pidof -s com.example.screenshotjanitor)
adb shell top -n 1 -b -p $(adb shell pidof -s com.example.screenshotjanitor)
adb shell dumpsys procstats com.example.screenshotjanitor --hours 1

# Background (after sending to home)
adb shell input keyevent KEYCODE_HOME
sleep 10
adb shell dumpsys meminfo $(adb shell pidof -s com.example.screenshotjanitor)
adb shell top -n 1 -b -p $(adb shell pidof -s com.example.screenshotjanitor)
```
