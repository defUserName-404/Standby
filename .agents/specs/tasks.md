# StandBy-Clone — Implementation Tasks

Each task is scoped to be handed to a coding agent (or done yourself) one at a time. Every task lists the acceptance criteria it implements — when a task is done, you should be able to verify it against those exact lines in `requirements.md`. Tasks are ordered so nothing depends on a task below it.

Check off as you go. Don't start a task whose dependencies aren't checked.

**Phase 2 comes before the notification pipeline on purpose:** requirement 12.4 says battery-optimization exemption is prompted *during onboarding*, and requirement 11 requires a first-launch flow listing every grant. The onboarding phase therefore lands immediately after the trigger skeleton is validated, and the battery-exemption / OEM-guidance / notification-access steps all live there instead of being scattered across later phases.

---

### Phase 1 — Trigger skeleton (validate the riskiest part first)

- [x] **1. Project setup** — Gradle module, package structure per `design.md`/`standby_clone_system_architecture.md` §9, Hilt wired up empty.
    - _Foundational, no specific acceptance criteria._

- [x] **2. `PowerStateRepository` + `ScreenPowerReceiver`** — dynamically-registered receiver for `SCREEN_ON`, `SCREEN_OFF`, `USER_PRESENT`, `POWER_CONNECTED`, `POWER_DISCONNECTED`; expose `StateFlow<PowerState>`.
    - _Requirements: 1.1, 1.2, 1.3_

- [x] **3. `StandByMonitorService`** — foreground service hosting the receiver's lifecycle and a coroutine scope; persistent notification; `START_STICKY`.
    - _Requirements: 12.1, 12.3_

- [x] **4. `BootReceiver`** — manifest-registered, restarts `StandByMonitorService` after reboot.
    - _Requirements: 1.5, 12.2_

- [x] **5. `AppSettings` model + `SettingsRepository` (minimal: just `triggerMode` for now)** — DataStore-backed, JSON via kotlinx.serialization, `schemaVersion` field from day one.
    - _Requirements: 10.1, 10.2_

- [x] **6. `TriggerEvaluatorUseCase`** — combine `PowerStateRepository` + `SettingsRepository.triggerMode`, emit launch events via `SharedFlow`.
    - _Requirements: 1.1, 1.2, 1.3_

- [x] **7. `SessionStateHolder`** — shared `isStandByCurrentlyShowing` flag.
    - _Requirements: 1.4_

- [x] **8. `StandByLauncher` + bare `StandByActivity`** — window flags (`setShowWhenLocked`, `setTurnScreenOn`, keep-screen-on), shows just a clock; tap to dismiss.
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.3_

- [x] **9. Manual validation checkpoint** — on your actual device: lock screen, plug in charger, confirm the clock screen appears within 1s and dismisses on tap. Test with battery optimization *not yet* exempted to see real-world OEM behavior before building anything else.
    - _Requirements: 1.1–1.4, 2.1–2.4, 3.1, 3.3 (manual verification)_

---

### Phase 2 — First-launch onboarding & permissions

- [x] **10. `PermissionRepository` + `PermissionState`** — single source of truth for the three grants: `POST_NOTIFICATIONS` (runtime check; treated as granted pre-API 33), notification-listener (`getEnabledListenerPackages()`), battery-optimization exemption (`PowerManager.isIgnoringBatteryOptimizations`). Exposes re-checkable state so UI can refresh on resume.
    - _Requirements: 11.3, 11.4_

- [x] **11. Stub `StandByNotificationListenerService` + manifest declaration** — minimal no-op `NotificationListenerService` with `BIND_NOTIFICATION_LISTENER_SERVICE` so the app appears in Settings → Notification access (without this, the onboarding deep-link has nothing to land on). Mapper/repository wiring stays in Phase 3.
    - _Requirements: 11.2, 4.1_

- [x] **12. `AppSettings.onboardingCompleted` + `schemaVersion` bump + `SettingsMigration` scaffolding** — first-launch detection lives in the single settings aggregate; add the migration hook even though v1→v2 only adds a defaulted field.
    - _Requirements: 10.3, 11.1_

- [x] **13. Onboarding flow UI (`OnboardingViewModel` + Compose screens in `MainActivity`)** — welcome screen explaining what the app does and why each grant is needed, then one step per grant: `POST_NOTIFICATIONS` (required; runtime request — moves the ad-hoc request out of `MainActivity.onCreate`), battery-optimization exemption (required; direct `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` dialog, manifest permission added, fall back to the settings list if the intent can't be resolved), notification-listener access (optional/skippable; deep-link to `ACTION_NOTIFICATION_LISTENER_SETTINGS`, skipping marks the notification widget degraded per 11.4), and a conditional OEM-guidance step shown only on known-aggressive manufacturers (MIUI/Samsung/Oppo/Vivo/Huawei/etc.) with manual autostart/battery-whitelist instructions. Every step re-checks live state on `ON_RESUME`; finishing persists `onboardingCompleted`; subsequent launches skip the flow.
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 12.4, 4.1, 12.3 (OEM mitigation)_

- [ ] **14. Manual validation checkpoint** — fresh install: onboarding appears on first launch only; each deep-link/dialog lands on the right system screen; returning to the app updates each step's status; denying battery exemption surfaces a degraded-reliability indicator; granting everything then force-stopping and relaunching skips onboarding with the monitor service running.
    - _Requirements: 11.1–11.4, 12.4 (manual verification)_

---

### Phase 3 — Notification pipeline

- [x] **15. `NotificationMapper` + wire into the stub listener service** — map `StatusBarNotification` to `NotificationEntry`; the service from task 11 starts forwarding real callbacks.
    - _Requirements: 4.2_

- [x] **16. `NotificationRepository`** — `StateFlow<List<NotificationEntry>>` updated by listener callbacks.
    - _Requirements: 4.2, 4.3_

- [x] **17. `ListenerHealthMonitor`** — periodic re-check, prompts re-enable if silently revoked.
    - _Requirements: 4.5_

- [x] **18. Render latest notification as plain text on `StandByActivity`** — no styling yet, just prove the data flow end to end.
    - _Requirements: 4.2, 4.3_

---

### Phase 4 — Settings, zen mode, exclude list

- [x] **19. Extend `AppSettings`** — add `zenModeEnabled`, `excludedPackages`, `schemaVersion` bump using the migration scaffolding from task 12.
    - _Requirements: 10.3_

- [x] **20. `ZenState` + `NotificationFilterUseCase`** — pure function `(rawList, zenState, excludeList) → DisplayableNotification`.
    - _Requirements: 4.4, 5.1, 5.2, 5.3_

- [x] **21. Settings home screen (replaces `MainActivity` placeholder)** — trigger mode, zen mode toggle, exclude-list picker (from currently-installed apps), plus a permission-status summary that re-surfaces degraded grants per 11.4 outside of onboarding.
    - _Requirements: 5.3, 10.1, 10.4, 11.4_

- [x] **22. Wire `NotificationFilterUseCase` output into `StandByActivity`** — replace the plain-text placeholder from task 18.
    - _Requirements: 5.1, 5.2, 5.3_

---

### Phase 5 — Widget system (the extensibility refactor)

- [ ] **23. Define `WidgetSpec`, `WidgetDataSource` contracts** — no implementations yet, just the interfaces.
    - _Requirements: 9.1_

- [ ] **24. `WidgetRegistry`** — Hilt multibinding assembling `WidgetSpec` + Composable renderer pairs.
    - _Requirements: 9.1_

- [ ] **25. Refactor clock into a widget** — first implementation against the new contract.
    - _Requirements: 9.1, 9.3_

- [ ] **26. Refactor notification display into a widget** — second implementation; this is where contract gaps usually surface — fix the contract now, not later.
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

- [ ] **27. Add battery widget as the third proof case** — uses `PowerStateRepository.batteryPercent`; if this drops in cleanly, the contract is validated.
    - _Requirements: 9.1, 9.2, 9.3_

- [ ] **28. `LayoutEngine`** — pure function `(enabledWidgetIds, orientation, sizeClass) → arrangement`.
    - _Requirements: 6.4, 9.3_

- [ ] **29. `StandByScreenViewModel`** — reads enabled widgets from settings, subscribes only to needed data sources, asks `LayoutEngine` for arrangement.
    - _Requirements: 9.2, 9.4_

- [ ] **30. Widget enable/disable + reorder UI in settings** — writes `enabledWidgetIds` back through `SettingsRepository`.
    - _Requirements: 9.2, 10.1_

---

### Phase 6 — Orientation

- [x] **31. `OrientationController`** — sets `requestedOrientation` from `SettingsRepository.orientationMode`.
    - _Requirements: 6.1, 6.2, 6.3_

- [x] **32. Orientation setting UI** — landscape-only / portrait-only / auto.
    - _Requirements: 6.1, 6.2, 6.3_

- [ ] **33. Verify `LayoutEngine` produces distinct arrangements per orientation** — on-device check across all three modes.
    - _Requirements: 6.4 (manual verification)_

---

### Phase 7 — Theming

- [ ] **34. `ThemeConfig` model + `ThemeRepository`** — extend `AppSettings`, DataStore-backed.
    - _Requirements: 7.1, 8.1_

- [ ] **35. `FontResolver`** — bundled fonts first, then Downloadable Fonts API, local caching, guaranteed fallback.
    - _Requirements: 7.1, 7.2, 7.3_

- [ ] **36. `BackgroundRenderer`** — strategy map for solid / gradient / image / blurred-wallpaper.
    - _Requirements: 8.1, 8.2, 8.3_

- [ ] **37. `ThemeProvider` `CompositionLocal`** — wire into `StandByActivity` root; update all widgets to read font/colors from it instead of hardcoded values.
    - _Requirements: 7.1, 8.1_

- [ ] **38. Theme settings screen** — font picker, background type + params picker.
    - _Requirements: 7.1, 8.1, 8.2, 10.1_

---

### Phase 8 — Polish & resilience

- [ ] **39. Motion-based dismissal** — accelerometer-based pickup detection, toggleable in settings.
    - _Requirements: 3.2_

- [ ] **40. Burn-in dimming** — subtle periodic pixel-shift while the StandBy screen is shown for extended periods.
    - _Non-functional (screen protection, not in requirements.md — add as requirement 13 if you want it tracked formally)_

- [ ] **41. Full manual regression pass** — walk every acceptance criterion in `requirements.md` (1.1 through 12.4) on-device before considering this "done."
    - _Requirements: all_

---

## How to use this with a coding agent

Feed one unchecked task at a time, along with `requirements.md` and `design.md` for context. A good prompt shape: *"Implement task 16 from tasks.md. Reference requirements 4.2–4.3 in requirements.md and the NotificationRepository description in design.md §2."* Check the box, verify against the referenced acceptance criteria, move to the next task. Don't batch multiple phases into one session — the phase boundaries exist because the manual checkpoint tasks (9, 14, 33) should genuinely gate whether the next phase is worth starting.
