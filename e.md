Your requirements are clear enough to answer those architecture questions.

### Framework Choice

**Native Android (Kotlin)**

Reason:

* Floating overlay (`SYSTEM_ALERT_WINDOW`)
* Volume control (`AudioManager`)
* Detect foreground app for exclusions
* Drag gestures, double-tap gestures
* Battery-efficient background service

All are much easier and more reliable in Kotlin than Flutter/React Native.

---

### Foreground App Detection

**AccessibilityService**

Reason:

* Instant detection when app changes
* No polling loop
* Lower battery usage
* More reliable exclusion handling

Yes, Android shows a scary warning, but many overlay apps use it.

Alternative:

* UsageStatsManager works but needs constant checking and can feel laggy.

---

### Volume Type

**Media Volume only (default)**.

Reasons:

* Most users want YouTube, Spotify, games, reels, etc.
* Doesn't accidentally change ringtone volume.

Optional setting:

* Media
* Ring
* Notification
* System

User chooses in settings.

---

### Suggested Architecture

```
Main Activity
├── Overlay Settings Screen
│   ├── Size Slider
│   ├── Opacity Slider
│   ├── Sensitivity Slider
│   └── Enable/Disable Overlay

├── Excluded Apps Screen
│   ├── Installed Apps List
│   └── Checkbox Selection

Accessibility Service
├── Detect Current App
└── Hide/Show Overlay

Overlay Service
├── Floating Button
├── Drag Detection
├── Double Tap Detection
└── Volume Control
```

---

### Gesture Mapping

| Gesture                | Action                    |
| ---------------------- | ------------------------- |
| Drag Up                | Volume +                  |
| Drag Down              | Volume -                  |
| Single Tap             | Optional haptic feedback  |(customisable)
| Double Tap + Drag Up   | Move floating button up   |
| Double Tap + Drag Down | Move floating button down |
| Long Press             | Open quick settings       |
| Drag Left/Right        | Future brightness control |

---

### Extra Features Worth Adding

* Snap button to screen edge
* Vibrate on volume change(customisable)
* Auto-hide when keyboard appears
* Different sizes in portrait/landscape
* Import/export excluded apps
* Floating button shape selection (circle, pill, square)

This app is completely feasible on Android using Kotlin + AccessibilityService + Overlay Service. The biggest challenge is handling gesture conflicts cleanly between **volume adjustment mode** and **overlay movement mode**.
Even Better UX

Use visual feedback:

Volume mode → button glows green
Move mode → button glows blue
Small vibration when entering move mode

User instantly knows which mode is active.

Another Option

Instead of double-tap + drag:

Normal drag → volume
Long press (300ms) → move button

This is what many floating apps do because it's simpler and easier to discover.

My recommendation:

Normal drag = volume
Long press + drag = move overlay

It's easier for users and simpler to implement than double-tap-and-drag