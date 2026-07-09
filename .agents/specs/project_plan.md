# StandBy-Clone — Android App Planning Document

**Goal:** A personal-use Android app that mimics iOS StandBy (full-screen glanceable display when the phone is charging/screen-off/locked), built in Jetpack Compose, with no feature gating — notification access, portrait mode, custom fonts/backgrounds, zen mode — and a codebase designed to grow (more widgets, more themes) without rewrites.

---

## 1. Feature Checklist (parity + your requirements)

| Feature | Notes |
|---|---|
| Auto-launch on screen-off / lock | Core trigger system |
| Auto-launch on charge-connect | Secondary/primary trigger, configurable |
| Works over lock screen | No unlock required to view |
| Notification access (read + display) | OS "special access" permission, not a runtime one |
| Zen mode toggle | ON = no notifications shown; OFF = show latest 1 |
| Portrait + landscape support | Configurable, not locked to landscape like iOS |
| Custom fonts | Font family picker, extensible list |
| Custom backgrounds | Solid, gradient, image, blur |
| Widget-style layout (clock, notif, battery, media, calendar…) | Must be pluggable, not hardcoded |
| Settings screen | Central control panel, persisted |
| Extensible codebase | New widgets/themes addable without touching core logic |

---

## 2. High-Level Architecture

Use **Clean Architecture (light version) + MVVM**, single Gradle module to start (multi-module later if it grows). Three layers:

- **presentation/** — Compose screens, ViewModels, UI state
- **domain/** — use cases, models, widget & theme contracts (the "rules" everything else plugs into)
- **data/** — repositories: `PowerStateRepository`, `NotificationRepository`, `SettingsRepository`

Why this split matters for you specifically: the **domain layer defines interfaces** (widget contract, theme contract) and the **presentation layer just renders whatever implements them**. Adding a new widget later means writing one new class and registering it — nothing else changes. That's your "extensible" requirement satisfied structurally, not just by convention.

---

## 3. Trigger & Display System (the hardest part, do this first)

### 3.1 Detecting the right moment
Android doesn't let you declare `SCREEN_ON`/`SCREEN_OFF`/`ACTION_POWER_CONNECTED`/`ACTION_POWER_DISCONNECTED` as manifest-registered broadcast receivers (blocked since API 26 for implicit broadcasts). You need a **long-running foreground Service** that dynamically registers a `BroadcastReceiver` for:
- `Intent.ACTION_SCREEN_OFF`
- `Intent.ACTION_SCREEN_ON`
- `Intent.ACTION_USER_PRESENT`
- `Intent.ACTION_POWER_CONNECTED`
- `Intent.ACTION_POWER_DISCONNECTED`

This service must show a persistent notification (foreground service requirement). On Android 14+ you must also declare a `foregroundServiceType` (use `specialUse` with a justification string, since none of the standard types fit).

**Trigger logic (configurable in settings):**
- "Charging only" (iOS-like): launch when screen turns off *while* charging, or when charging starts while screen is off/on.
- "Always on screen-off": launch on any screen-off, regardless of charge state.
- "Screen-off + docked/still": optionally use the accelerometer to detect the phone is stationary/propped up, so it doesn't fire every time it goes in your pocket. (Nice-to-have, phase later.)

### 3.2 Displaying over the lock screen
Launch a dedicated Activity (not a system overlay) with:
- `setShowWhenLocked(true)`
- `setTurnScreenOn(true)`
- `KeyguardManager.requestDismissKeyguard()` is **not** used (you don't want to unlock the phone, just draw on top of the lock screen)
- Window flags: `FLAG_SHOW_WHEN_LOCKED`, `FLAG_TURN_SCREEN_ON`, `FLAG_KEEP_SCREEN_ON` (for the duration it's showing)

This approach avoids needing `SYSTEM_ALERT_WINDOW` entirely, which is cleaner and less likely to get killed by OEM battery managers than a true overlay window.

### 3.3 Dismissing it
- Any touch/tap → finish the activity, fall through to real lock screen.
- Optional: accelerometer-detected pickup/movement → auto-dismiss (mimics iOS behavior of StandBy disappearing when you pick the phone up, before you even touch it).
- Screen turns fully off again → activity naturally goes away, service re-arms for next trigger.

### 3.4 Orientation (your "portrait mode" requirement)
Don't lock the Activity's orientation in the manifest. Instead:
- Read `Settings.orientationMode` from your settings repo: `LANDSCAPE_ONLY`, `PORTRAIT_ONLY`, `AUTO_FOLLOW_DEVICE`.
- Apply via `requestedOrientation` at runtime based on the setting, and use `Configuration`/`WindowInsets` in Compose to pick a portrait vs. landscape widget layout variant.

---

## 4. Notification Access

This is a **special app access** permission, not a runtime permission dialog — the user must manually flip it on in system settings; you can't request it programmatically.

- Implement a `NotificationListenerService` subclass, declared in the manifest with `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` and the standard intent filter.
- On first run, detect if access isn't granted (`NotificationManagerCompat.getEnabledListenerPackages()`), and deep-link the user to `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` with an explanatory screen first.
- In `onNotificationPosted` / `onNotificationRemoved`, maintain an in-memory (or DataStore-backed) list of "active notifications" — extract package name, app icon, title, text, timestamp. Respect `Notification.visibility` where sensible (some notifications are marked private/sensitive — since this is personal use you can choose to show everything, but build the filter hook now so it's a one-line toggle later).
- Add a **per-app exclude list** in settings from day one (e.g. banking/2FA apps) — cheap to build now, painful to retrofit.

---

## 5. Zen Mode & Notification Display Logic

Simple derived state, not a separate system:
- `zenModeEnabled: Boolean` in settings.
- If `true` → StandBy notification widget renders nothing (or a subtle "Zen" indicator).
- If `false` → StandBy notification widget renders the single most recent active notification (sorted by post-time, filtered by exclude list).
- This logic lives in a `NotificationDisplayUseCase` in the domain layer — takes the raw notification list + zen setting + exclude list, returns "what to render." Keeps the widget itself dumb (just renders whatever it's given), which matters for extensibility (see §6).

---

## 6. Extensible Widget System

Model each StandBy element (clock, notification, battery, media controls, calendar, weather…) as an implementation of a common **widget contract** living in the domain layer. Conceptually each widget exposes:
- a stable `id` and `displayName`
- a default/preferred size class (small / medium / full-width)
- a `Composable` render function that takes a state object and theme, and draws itself
- an optional settings schema (so the settings screen can auto-generate config UI for it, e.g. "12h vs 24h clock")

A central **WidgetRegistry** (simple list or DI multibinding via Hilt) holds all available widgets. The StandBy screen just asks the registry "give me the widgets enabled for this layout" and arranges them in a grid/column depending on orientation. Adding a new widget later = write one class + register it in the registry. Nothing in the Activity or layout engine needs to change.

Start with 2–3 widgets (clock, notification, battery) to validate the contract before building more — don't over-design the interface before you've built a second widget to prove it against.

---

## 7. Theming & Customization

Separate **Theme** from **Widgets** — theme controls how things look, widgets control what's shown.

Theme config (persisted, one object):
- Font family (start with a small curated list: system default + 3–4 Google Fonts bundled or downloaded via the Downloadable Fonts API)
- Background type: solid color / gradient / static image / blurred wallpaper-behind
- Accent/text color
- Corner radius / spacing scale (optional, for later "look presets")

Keep this as a single serializable data class so it's trivial to add new fields later (see §8 — use JSON serialization, not a rigid schema, so old settings don't break when you add a field).

Build a small set of **preset themes** (e.g. "Minimal Dark", "Warm Analog", "High Contrast") on top of the same config object — presets are just pre-filled instances of the config, not a separate system.

---

## 8. Settings & Persistence

Use **Jetpack DataStore (Preferences or Proto)**, not SharedPreferences — it's Flow-native, which matters because your Compose UI should reactively re-render the StandBy screen the instant a setting changes (e.g. toggling zen mode should visibly update immediately if you back out to test it).

Practical approach:
- One `SettingsRepository` exposing a `Flow<AppSettings>` (a single data class holding trigger mode, orientation mode, zen mode, exclude list, theme config, enabled widget IDs).
- Serialize with `kotlinx.serialization` to JSON and store as one DataStore entry, rather than dozens of separate keys — much easier to extend without migrations as you add fields (default values handle missing keys gracefully).
- ViewModels collect this Flow and expose UI state; Settings screen writes back through the repository.

---

## 9. Permissions & OS Integration Matrix

| Permission / API | Type | Purpose |
|---|---|---|
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | Manifest permission | Keep the trigger-detection service alive |
| `POST_NOTIFICATIONS` | Runtime (API 33+) | Required to show the foreground service's own notification |
| `RECEIVE_BOOT_COMPLETED` | Manifest permission | Restart the monitoring service after reboot |
| Notification Listener access | Special access (user-granted in Settings) | Read notifications |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Special access | Prevent OEMs from killing your service (see §10 gotchas) |
| `SET_WALLPAPER` (optional) | Manifest permission | Only if you want "blurred wallpaper" background style |
| `WAKE_LOCK` | Manifest permission | Briefly needed alongside `FLAG_TURN_SCREEN_ON` on some OEMs |

Note: you do **not** need `SYSTEM_ALERT_WINDOW` with the Activity-over-lockscreen approach in §3.2 — avoid it, it's a heavier, more restricted permission than you need.

---

## 10. Known Gotchas (worth knowing before you're 3 weeks in)

- **OEM battery managers** (Xiaomi/MIUI, Samsung, Huawei, OnePlus, Oppo) aggressively kill background services regardless of "ignore battery optimizations" being granted. There's no universal fix — the best you can do is detect the OEM and deep-link the user to the vendor-specific autostart/battery settings screen with instructions. Since this is for your own phone, you can just manually whitelist it once.
- **API 34 foreground service types** are stricter — `specialUse` requires a `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` string in the manifest explaining why (a one-line justification is enough, e.g. "screen-state-triggered display").
- **`setTurnScreenOn`/`setShowWhenLocked`** behave slightly differently pre/post API 27 — support the older `WindowManager.LayoutParams` flags as a fallback if you want to support older devices, otherwise just target API 27+ minimum and skip the fallback entirely (recommended, simplifies a lot).
- **Notification listener rebinding**: the system occasionally unbinds your listener service silently (especially after updates); consider a periodic self-check (`NotificationManagerCompat.getEnabledListenerPackages()`) from your foreground service to detect and prompt re-enabling if it drops.
- **Always-on display concerns**: if you leave the StandBy screen showing indefinitely while charging, consider dimming/burn-in protection (subtle pixel-shift every few minutes) — cheap to add, saves your screen on OLED devices.

---

## 11. Recommended Tech Stack

- **UI:** Jetpack Compose + Material3
- **DI:** Hilt (makes the widget registry and repositories clean to wire, and scales well as you add modules)
- **State/async:** Kotlin Coroutines + Flow
- **Persistence:** DataStore + kotlinx.serialization
- **Fonts:** Downloadable Fonts API (Google Fonts) + a couple of bundled fallback fonts for offline reliability
- **Min SDK:** API 27 (Oreo MR1) is the practical floor given `setShowWhenLocked`; API 26+ for background restrictions either way. If you don't care about old devices, set min SDK 29–31 and simplify further.

---

## 12. Suggested Build Order (phased roadmap)

1. **Phase 1 — Trigger skeleton:** Foreground service detecting screen-off/charging, launching a bare Activity over the lock screen showing just a clock. Validate this survives real-world use on your device (the OEM battery gotcha above) before building anything else on top.
2. **Phase 2 — Notification access:** Add the listener service, onboarding/permission flow, and render the latest notification as plain text on the StandBy screen (no styling yet).
3. **Phase 3 — Settings + persistence:** Build the DataStore-backed `SettingsRepository`, a basic settings screen, wire up zen mode toggle and trigger-mode choice end-to-end.
4. **Phase 4 — Widget system:** Extract clock/notification/battery into the widget contract + registry described in §6. This is the refactor that makes everything after it easy.
5. **Phase 5 — Theming:** Font picker, background picker, theme presets.
6. **Phase 6 — Orientation & polish:** Portrait/landscape layouts, dismiss-on-pickup via accelerometer, burn-in dimming, per-app notification exclude list UI.
7. **Phase 7 — More widgets:** Media controls, calendar, weather — should now be additive, not invasive, if Phase 4 was done right.

---

## 13. Future Extension Ideas (not needed now, just so the design doesn't block them)

- Multiple saved "layouts" (swipe between a clock-focused view and a widget-dense view)
- Per-time-of-day themes (auto dark background at night)
- Widget size variants (small/medium/large) with drag-to-resize
- Export/import settings as JSON (trivial if you're already using kotlinx.serialization for persistence)

---

**Where to start literally tomorrow:** Phase 1 only. Get the foreground service + lock-screen Activity trigger working reliably on your actual phone model first — that's the part most likely to fight you (OEM restrictions), and everything else in this doc is comparatively straightforward Compose/DataStore work once that foundation is solid.