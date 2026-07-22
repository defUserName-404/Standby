# StandBy-Clone — Design Document

This document connects `requirements.md` to concrete implementation structure. Full component rationale and data-flow walkthroughs already exist in `standby_clone_system_architecture.md` — this file adds what a spec needs that a narrative architecture doc doesn't: a **traceability table**, explicit **data model shapes**, an **error-handling table**, and a **testing strategy**. Read them together: architecture doc for *why*, this one for *what satisfies which requirement*.

---

## 1. Architecture Reference

Three layers, dependency rule `Presentation → Domain ← Data` (see the layered-architecture diagram and full breakdown in `standby_clone_system_architecture.md`, §0–§9). Nothing in that structure changes here — this document is the traceability layer on top of it.

---

## 2. Requirements-to-Component Traceability

| Requirement | Component(s) responsible | Architecture doc section |
|---|---|---|
| 1. Screen-off trigger | `ScreenPowerReceiver`, `PowerStateRepository`, `TriggerEvaluatorUseCase`, `BootReceiver` | §1 |
| 2. Lock-screen overlay | `StandByLauncher`, `StandByActivity` (window flags) | §1 |
| 3. Dismissal | `StandByActivity`, `SessionStateHolder` | §1 |
| 4. Notification access & display | `StandByNotificationListenerService`, `NotificationMapper`, `NotificationRepository`, `ListenerHealthMonitor` | §2 |
| 5. Zen mode | `ZenState`, `NotificationFilterUseCase` | §3 |
| 6. Orientation | `OrientationController`, `LayoutEngine` | §8, §4 |
| 7. Fonts | `ThemeConfig`, `FontResolver` | §5 |
| 8. Backgrounds | `ThemeConfig`, `BackgroundRenderer` | §5 |
| 9. Widget system | `WidgetSpec`, `WidgetDataSource`, `WidgetRegistry`, `LayoutEngine` | §4 |
| 10. Settings persistence | `AppSettings`, `SettingsRepository`, `SettingsMigration` | §6 |
| 11. Permission onboarding | `PermissionRepository`, `PermissionState`, `OnboardingViewModel` | §7 |
| 12. Service resilience | `StandByMonitorService`, `BootReceiver`, `PermissionRepository` (battery exemption) | §1, §7 |

Every row here is a promise: if a requirement's acceptance criteria aren't met, the bug lives in one of the components in that row — this table is what makes the spec debuggable, not just descriptive.

---

## 3. Data Models

All domain models are plain Kotlin data classes with zero Android framework dependencies (enforces the dependency rule at compile time — if a domain model needs an Android import, it's in the wrong layer).

### `PowerState`
- `isScreenOn: Boolean`
- `isCharging: Boolean`
- `batteryPercent: Int` (0–100, for the battery widget)

### `NotificationEntry`
- `id: String` (stable key, package + notification key)
- `packageName: String`
- `appLabel: String`
- `iconRef: IconRef` (opaque reference the presentation layer resolves to a `Drawable`/`Painter`)
- `title: String`
- `text: String`
- `postedAt: Instant`
- `isSensitive: Boolean` (from `Notification.visibility`, reserved for a future "hide sensitive content" toggle)

### `ZenState`
- `enabled: Boolean`
- (reserved: `scheduledWindows: List<TimeRange>` for a future "auto zen at night" feature — leave the field absent, not stubbed, until it's built; don't pre-build unused fields)

### `AppSettings` (aggregate root)
- `schemaVersion: Int`
- `onboardingCompleted: Boolean` (false until the first-launch permission flow is finished or dismissed)
- `triggerMode: TriggerMode` (enum: `CHARGING_ONLY`, `ALWAYS`)
- `orientationMode: OrientationMode` (enum: `LANDSCAPE_ONLY`, `PORTRAIT_ONLY`, `AUTO`)
- `zenModeEnabled: Boolean`
- `excludedPackages: Set<String>`
- `enabledWidgetIds: List<String>` (ordered — order is display order)
- `motionDismissalEnabled: Boolean`
- `theme: ThemeConfig`

### `ThemeConfig`
- `fontChoice: FontChoice` (enum/id referencing bundled or downloadable fonts)
- `backgroundType: BackgroundType` (`SOLID`, `GRADIENT`, `IMAGE`, `BLURRED_WALLPAPER`)
- `backgroundParams: BackgroundParams` (variant-specific: color(s), image URI, blur radius)
- `accentColor: Color`
- `textColor: Color`

### `WidgetSpec`
- `id: String`
- `displayName: String`
- `sizeClass: WidgetSizeClass` (`SMALL`, `MEDIUM`, `FULL_WIDTH`)
- `settingsSchema: List<WidgetSettingField>` (optional, for auto-generated per-widget config UI)

### `PermissionState`
- `type: PermissionType` (`NOTIFICATION_LISTENER`, `BATTERY_OPTIMIZATION_EXEMPTION`, `POST_NOTIFICATIONS`)
- `status: Granted | Denied | SpecialAccessRequired`

---

## 4. Error Handling

| Failure mode | Requirement(s) at risk | Handling strategy |
|---|---|---|
| Notification listener silently unbound by OS | 4.2, 4.5 | `ListenerHealthMonitor` checks `getEnabledListenerPackages()` on service start and each screen-on; surfaces a re-enable prompt rather than failing silently |
| Downloadable font fails to fetch | 7.1, 7.2 | `FontResolver` catches the failure, returns the system default `FontFamily`, logs locally; never blocks rendering |
| Widget data source throws | 9.4 | Each widget's data collection is wrapped (`catch` on its `Flow`) at the point it's combined in `StandByScreenViewModel`; a failing widget renders its own empty/error state, sibling widgets are unaffected |
| Settings JSON fails to deserialize (corruption) | 10.2, 10.3 | `SettingsRepository` falls back to a default `AppSettings` instance rather than crashing on launch; the user re-configures from a clean state instead of being locked out |
| Two settings writes race | 10.4 | All writes go through `SettingsRepository`'s single transactional `update { current -> ... }` function — DataStore serializes concurrent updates internally, so last-write-wins only ever applies to genuinely concurrent edits of the *same* field, never cross-field clobbering |
| Monitoring service killed by OS/OEM manager | 12.3 | Foreground service uses `START_STICKY`; `BootReceiver` provides a hard restart path on reboot as a backstop when `START_STICKY` isn't honored by an aggressive OEM manager |
| StandBy screen fails to acquire window flags (OEM quirk) | 2.1, 2.2 | Log the failure state; fall back to launching without `FLAG_TURN_SCREEN_ON` rather than crashing — degraded (screen may need manual wake) but not broken |
| Duplicate trigger events fire close together | 1.4 | `SessionStateHolder`'s `isStandByCurrentlyShowing` flag is checked by `StandByLauncher` before every launch attempt |

---

## 5. Testing Strategy

Maps directly onto the layer boundaries — this is the payoff of keeping domain framework-free.

- **Domain layer (unit tests, plain JUnit, no Android):**
    - `TriggerEvaluatorUseCase` — feed fake `PowerState` and settings flows, assert correct launch-event emission for every combination in requirements 1.1–1.4.
    - `NotificationFilterUseCase` — feed fake notification lists, zen states, exclude lists, assert output matches 4.4, 5.1, 5.2.
    - `LayoutEngine` — feed widget lists + orientation, assert arrangement matches 6.4, 9.3.

- **Data layer (unit tests with fakes/in-memory DataStore):**
    - `SettingsRepository` — round-trip serialize/deserialize, verify `SettingsMigration` against a deliberately old-schema fixture (10.3).
    - `NotificationMapper` — verify `StatusBarNotification → NotificationEntry` field mapping and exclude-list filtering.

- **Presentation layer (Compose UI tests + ViewModel tests with fake repositories):**
    - `StandByScreenViewModel` — verify it only subscribes to data sources for currently-enabled widgets (9.2).
    - Widget composables — snapshot/behavior tests against injected fake state, including the error state from 9.4.

- **Platform glue (manual/device testing only, by design — see architecture doc §11):**
    - `ScreenPowerReceiver`, `StandByNotificationListenerService`, window-flag behavior, and OEM battery-manager interactions cannot be meaningfully unit tested. Validate these directly on your target device(s) per requirement, using the acceptance criteria in `requirements.md` as the manual test script.

- **Regression checklist before each release build:** walk requirements 1–12 in order on-device; each acceptance criterion is a one-line manual check (e.g. "toggle zen mode while StandBy is showing → notification disappears immediately" for 5.3).