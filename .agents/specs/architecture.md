# StandBy-Clone — System Architecture (Part 2: Full Subsystem Design)

Extends `standby_clone_planning_doc.md`. That doc covered *what* to build and in what order. This one covers *how each piece is structured internally* — components, responsibilities, data flow, and state ownership — so the codebase stays extensible as you add widgets, themes, and triggers over time.

---

## 0. Architectural Style

**Clean Architecture (light) + MVVM, single direction of dependency, reactive state throughout.**

Rule: `Presentation → Domain ← Data`. Domain never imports anything Android-specific (no `Context`, no Compose, no `NotificationListenerService`). Presentation and Data both depend on Domain's interfaces; Domain never depends on either. This is the rule that lets you swap implementations (a new notification source, a new persistence backend, a new widget) without touching unrelated code.

Every subsystem below follows the same repeating shape:

**Repository (source of truth, exposes `Flow`) → UseCase (pure transformation, when logic is non-trivial) → ViewModel (collects flows, converts to UI State) → Compose (collects with lifecycle awareness, renders).**

Learn this shape once — every feature section below is just this shape applied to a different problem.

---

## 1. Trigger & Lifecycle Subsystem

**Owns:** deciding *when* the StandBy screen should appear and *making it appear*.

**Components:**
- `ScreenPowerReceiver` (data/platform) — dynamically-registered `BroadcastReceiver` for `SCREEN_ON`, `SCREEN_OFF`, `USER_PRESENT`, `POWER_CONNECTED`, `POWER_DISCONNECTED`. Lives inside the service, not the manifest.
- `StandByMonitorService` (data/platform) — foreground `Service` that owns the receiver's lifecycle (registers on `onCreate`, unregisters on `onDestroy`), and hosts a small coroutine scope for the subsystem.
- `PowerStateRepository` (data) — translates raw receiver callbacks into a domain-level `StateFlow<PowerState>` (`isScreenOn`, `isCharging`). This is the single source of truth every other component reads from — nothing else touches the receiver directly.
- `TriggerEvaluatorUseCase` (domain) — combines `PowerStateRepository.state` with `SettingsRepository.triggerMode` (via `combine()`) and emits a one-shot "launch now" event through a `SharedFlow`/`Channel` (not a `StateFlow` — this is an event, not a persisted state, so it shouldn't replay on new collectors).
- `StandByLauncher` (data/platform) — thin wrapper that turns a "launch now" event into an actual `Intent` + `startActivity` call, with the correct window flags.
- `StandByActivity` (presentation) — the full-screen surface itself. Owns its own dismissal logic (touch, optional motion-based pickup detection).
- `SessionStateHolder` (data, small singleton) — a single `isStandByCurrentlyShowing: StateFlow<Boolean>` flag shared between the Service and the Activity, to prevent double-launch races (e.g. two screen-off broadcasts firing close together).
- `BootReceiver` (data/platform) — manifest-registered, restarts `StandByMonitorService` after device reboot (this one *can* be manifest-declared since `BOOT_COMPLETED` is explicit, not implicit-broadcast-restricted).

**Data flow:** raw `Intent` → `ScreenPowerReceiver` → `PowerStateRepository` updates `StateFlow` → `TriggerEvaluatorUseCase` combines with settings → emits event → `StandByLauncher` starts `StandByActivity` → Activity sets `SessionStateHolder = true` → on dismiss, sets it back to `false`.

**Why this split matters:** if you later add a new trigger condition (e.g. "only if phone is stationary," using an accelerometer), you add one new input `Flow` into `TriggerEvaluatorUseCase`'s `combine()` call — nothing else in the chain changes.

---

## 2. Notification Subsystem

**Owns:** capturing live notifications and turning them into something a widget can render.

**Components:**
- `StandByNotificationListenerService` (data/platform) — extends `NotificationListenerService`, does nothing but forward raw `StatusBarNotification` objects onward. Keep this class as thin as possible; it's the hardest thing to unit test, so business logic should not live here.
- `NotificationMapper` (data) — pure function: `StatusBarNotification → NotificationEntry` (domain model: id, package, app label, icon, title, text, timestamp). This is also where you'd strip or redact fields later if you add a "hide sensitive content" option.
- `NotificationRepository` (data) — holds the current set as `StateFlow<List<NotificationEntry>>`, updated by the mapper's output on `onNotificationPosted`/`onNotificationRemoved`.
- `NotificationFilterUseCase` (domain) — pure function taking `(rawList, zenState, excludeList) → DisplayableNotification` (a sealed result: `None` or `Single(entry)`). Pure and framework-free, so it's trivially unit-testable without touching Android.
- `ExcludeListRepository` (data) — part of `SettingsRepository`, a set of package names to always ignore.
- `ListenerHealthMonitor` (data) — small component that checks `NotificationManagerCompat.getEnabledListenerPackages()` on service start and on each screen-on event, to catch the OS silently unbinding the listener (a known platform quirk) and prompt re-enabling if needed.

**Data flow:** listener callback → mapper → repository `StateFlow` update → a ViewModel combines this with `zenState` and `excludeList` flows via `NotificationFilterUseCase` → UI State → `NotificationWidget` composable renders it.

**Design win worth noting:** because this is all `StateFlow`-driven, a notification being dismissed *while the StandBy screen is already showing* updates the UI automatically through recomposition — you don't need any special "refresh" logic for that case; it falls out of the reactive architecture for free.

---

## 3. Zen Mode

**Not a separate physical subsystem** — architect it as a *derived policy value*, not a standalone service.

- `ZenState` (domain) — currently just `enabled: Boolean`, computed via `SettingsRepository.settings.map { it.zenModeEnabled }`.
- This narrow, single-purpose `Flow<ZenState>` — rather than passing the entire `AppSettings` object downstream — is what lets `NotificationFilterUseCase` (§2) depend on a small stable interface. When you later add "auto-zen after 10pm," you change what produces `ZenState`, and nothing that *consumes* it needs to change.

---

## 4. Widget System (the extensibility core)

**Owns:** what appears on the StandBy screen, and how new things get added to it.

**Components:**
- `WidgetSpec` (domain) — static metadata per widget: stable id, display name, size class (small / medium / full-width), optional settings schema. No UI-framework types here.
- `WidgetDataSource` (domain interface) — one per widget type, declaring which repository flow(s) it needs (e.g. the clock widget needs a time-ticker flow; the notification widget needs `NotificationFilterUseCase`'s output; the battery widget needs `PowerStateRepository`).
- `WidgetRegistry` (presentation/DI, e.g. a Hilt multibinding `Set<WidgetSpec>` plus a parallel `Map<id, @Composable (State) -> Unit>`) — the single place where "what widgets exist" is assembled. This is the DI boundary where domain metadata meets Compose rendering.
- `LayoutEngine` (domain) — a pure function: `(enabledWidgetIds, orientation, sizeClass) → arrangement`. Knows nothing about Compose; just returns an ordered structure describing where each widget goes.
- `StandByScreenViewModel` (presentation) — reads the enabled-widget list from settings, asks `LayoutEngine` for the arrangement, and **only subscribes to the data sources actually needed by currently-enabled widgets** (avoid combining every repository flow unconditionally — that wastes recomposition cycles for widgets the user hasn't turned on).

**How adding a widget works, concretely:** implement `WidgetDataSource` (only if it needs new data), define its `WidgetSpec`, write one `@Composable` renderer, register both in `WidgetRegistry`. Nothing in `StandByActivity`, `LayoutEngine`, or any *other* widget needs to change. This is the mechanism, not just a principle — it's the concrete reason the architecture is extensible rather than just "organized."

**Practical note:** build this contract against two widgets first (clock + notification), then validate it against a third (battery) before you trust the interface shape. Interfaces designed against a single implementation are usually wrong in some small way that only shows up on the second use.

---

## 5. Theming Subsystem

**Owns:** how things look, independent of what's shown (widgets = what, theme = how).

**Components:**
- `ThemeConfig` (domain) — one versioned data object: font choice, background type + params, accent color, text color. Versioned via a `schemaVersion` field (see §6) so it can evolve without breaking old persisted data.
- `ThemeRepository` (data) — DataStore-backed, exposes `Flow<ThemeConfig>`.
- `FontResolver` (data/platform) — maps a `FontChoice` id to an actual `FontFamily`: bundled assets for a curated default set, Downloadable Fonts API for extras, with local caching and a guaranteed fallback to the system font if resolution fails (never let a font failure blank the screen).
- `BackgroundRenderer` (presentation) — a strategy map: `BackgroundType → @Composable` painter (solid, gradient, static image, blurred wallpaper-behind). Same registry pattern as widgets, same reason: adding a background type is additive.
- `ThemeProvider` (presentation) — a `CompositionLocal` populated once near the root of the StandBy screen's composition tree from `ThemeRepository.themeFlow`, so every widget reads font/colors from the local ambient value instead of having theme data threaded through every function signature.

**Data flow:** `ThemeRepository` flow → collected once at the screen root → provided via `CompositionLocal` → all widgets read it → Settings screen writes changes back through `ThemeRepository` → recomposition cascades automatically to every widget, with no manual "notify" step.

---

## 6. Settings & Persistence Subsystem

**Owns:** the single durable state of the whole app.

**Components:**
- `AppSettings` (domain) — one aggregate root data class holding trigger mode, orientation mode, zen mode, exclude list, theme config, enabled widget ids, plus a `schemaVersion: Int`.
- `SettingsRepository` (data) — DataStore-backed, serializes the whole object as one JSON blob via `kotlinx.serialization`. One entry, not dozens of scattered keys — this is deliberate: it means adding a new field later is a one-line change with a default value, not a migration.
- `SettingsMigration` (data) — a small `when (storedVersion)` ladder that upgrades old JSON shapes forward when `schemaVersion` doesn't match current. Build this scaffolding on day one even with only version 1 — retrofitting it after you've shipped a breaking change is much more painful.
- Per-section `ViewModel`s (presentation, e.g. `ThemeSettingsViewModel`, `TriggerSettingsViewModel`) read narrow slices of `AppSettings` and write back through a single transactional update path on the repository (`update { current -> current.copy(...) }`) rather than each maintaining separate write paths — this avoids two settings screens racing each other's writes.

---

## 7. Permission & Onboarding Subsystem

**Owns:** getting the user through the OS-level grants and catching when they've been revoked.

**Components:**
- `PermissionRepository` (data) — queries *live* OS state on demand (`NotificationManagerCompat.getEnabledListenerPackages()`, `PowerManager.isIgnoringBatteryOptimizations()`, etc.). This is **not** a `Flow` — the OS doesn't push you updates when the user changes these in system settings, so it's a pull-based check, not a stream.
- `PermissionState` (domain) — per-permission sealed state: `Granted / Denied / SpecialAccessRequired`.
- `OnboardingViewModel` (presentation) — re-validates all permission states on every `onResume` via a lifecycle observer, since the user may have just come back from the system settings screen.
- A sequence of independent onboarding steps (notification access, battery optimization exemption), each pairing a short explanation composable with an intent-launcher for the relevant system settings screen. Independent = you can reorder or add a step (e.g. a future "exact alarm" permission) without touching the others.

**Re-check strategy, stated explicitly because it's easy to miss:** re-validate on `onResume` of the settings screen, the onboarding screen, *and* on `StandByMonitorService` startup — not just once at install. Special-access grants can be silently revoked by the user or the OS at any time.

---

## 8. Orientation & Layout Adaptation

**Owns:** portrait vs. landscape, and adapting the widget arrangement to it.

**Components:**
- `OrientationController` (presentation) — sets `StandByActivity.requestedOrientation` at runtime from `SettingsRepository`'s orientation mode (`LANDSCAPE_ONLY / PORTRAIT_ONLY / AUTO_FOLLOW_DEVICE`).
- A `Configuration`/`WindowSizeClass` observer (presentation) feeds the current orientation into `LayoutEngine` (§4), which already accepts orientation as an input — no new concept here, just another input to an existing pure function.

---

## 9. Suggested Package Structure

```
di/                          - Hilt modules, WidgetRegistry assembly, BackgroundRenderer registry
service/                     - StandByMonitorService, ScreenPowerReceiver, BootReceiver,
                                StandByNotificationListenerService, ListenerHealthMonitor
domain/
  model/                     - PowerState, NotificationEntry, ZenState, AppSettings,
                                ThemeConfig, WidgetSpec
  usecase/                   - TriggerEvaluatorUseCase, NotificationFilterUseCase, LayoutEngine
  repository/                - interfaces only (PowerStateRepository, NotificationRepository,
                                SettingsRepository, ThemeRepository, PermissionRepository)
data/
  repository/                - concrete implementations of the above interfaces
  local/                     - DataStore setup, SettingsMigration
  platform/                  - StandByLauncher, FontResolver, NotificationMapper
presentation/
  standby/                   - StandByActivity, StandByScreenViewModel, LayoutEngine consumers
  settings/                  - per-section settings screens + ViewModels
  onboarding/                - permission onboarding flow
  theme/                     - ThemeProvider (CompositionLocal), BackgroundRenderer
widgets/
  clock/                     - WidgetSpec + WidgetDataSource + Composable, self-contained
  notification/               - same shape
  battery/                    - same shape
  (media/, calendar/, ...)    - future widgets, same shape, added without touching the above
```

Each widget living in its own self-contained package (spec + data source + composable together) is the physical expression of the plug-in architecture from §4 — you should be able to delete a widget's folder and have the app still compile (registry entry aside).

---

## 10. End-to-End Walkthrough (tying it together)

Concrete scenario: **user's phone starts charging while the screen is off, zen mode is off.**

1. `ScreenPowerReceiver` receives `ACTION_POWER_CONNECTED` → `PowerStateRepository.state` updates (`isCharging = true`).
2. `TriggerEvaluatorUseCase`'s `combine()` re-fires (power state changed), checks `SettingsRepository.triggerMode` — matches "charging" mode — emits a launch event.
3. `StandByLauncher` starts `StandByActivity` with lock-screen window flags; `SessionStateHolder` flips to `true`.
4. `StandByActivity` composes its root, `ThemeProvider` collects the current `ThemeConfig` and provides it via `CompositionLocal`.
5. `StandByScreenViewModel` reads enabled widget ids from `SettingsRepository`, asks `LayoutEngine` for the arrangement given current orientation.
6. For the notification widget specifically: `StandByScreenViewModel` combines `NotificationRepository.notifications`, `ZenState` (`false`, since zen is off), and `ExcludeListRepository.excluded` through `NotificationFilterUseCase` → produces `Single(latestEntry)`.
7. Compose renders the arranged widgets; the notification widget shows the one entry; the clock widget reads its own ticker source independently.
8. User picks the phone up — motion detected (or a tap) — `StandByActivity` calls `finish()`, `SessionStateHolder` flips back to `false`.

Notice every step is either "a repository's `StateFlow` changed" or "a pure function combined some flows" — there's no step that required reaching across layers or special-casing. That uniformity is the actual payoff of the architecture, not just a tidiness preference.

---

## 11. Testability Notes

- Everything in `domain/` has zero Android imports — `TriggerEvaluatorUseCase`, `NotificationFilterUseCase`, and `LayoutEngine` are plain Kotlin functions/classes, testable with plain JUnit and fake flows, no `Robolectric` or instrumentation needed.
- Because repository *interfaces* live in `domain/` and implementations in `data/`, every ViewModel can be tested against fake repositories (simple in-memory `MutableStateFlow`-backed fakes) without touching DataStore, `NotificationListenerService`, or real broadcasts at all.
- The one thing you can't meaningfully unit test is the platform glue itself (§1, §2's listener/receiver classes) — keep those deliberately thin (forward-only, no logic) so the untested surface area is as small as possible.