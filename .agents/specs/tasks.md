# StandBy-Clone — Implementation Tasks

Each task is scoped to be handed to a coding agent (or done yourself) one at a time. Every task lists the acceptance criteria it implements — when a task is done, you should be able to verify it against those exact lines in `requirements.md`. Tasks are ordered so nothing depends on a task below it.

Check off as you go. Don't start a task whose dependencies aren't checked.

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

- [ ] **6. `TriggerEvaluatorUseCase`** — combine `PowerStateRepository` + `SettingsRepository.triggerMode`, emit launch events via `SharedFlow`.
    - _Requirements: 1.1, 1.2, 1.3_

- [ ] **7. `SessionStateHolder`** — shared `isStandByCurrentlyShowing` flag.
    - _Requirements: 1.4_

- [ ] **8. `StandByLauncher` + bare `StandByActivity`** — window flags (`setShowWhenLocked`, `setTurnScreenOn`, keep-screen-on), shows just a clock; tap to dismiss.
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.3_

- [ ] **9. Manual validation checkpoint** — on your actual device: lock screen, plug in charger, confirm the clock screen appears within 1s and dismisses on tap. Test with battery optimization *not yet* exempted to see real-world OEM behavior before building anything else.
    - _Requirements: 1.1–1.4, 2.1–2.4, 3.1, 3.3 (manual verification)_

---

### Phase 2 — Notification access

- [ ] **10. `StandByNotificationListenerService` + `NotificationMapper`** — thin listener forwarding to the mapper; map to `NotificationEntry`.
    - _Requirements: 4.2_

- [ ] **11. `NotificationRepository`** — `StateFlow<List<NotificationEntry>>` updated by listener callbacks.
    - _Requirements: 4.2, 4.3_

- [ ] **12. `PermissionRepository` + notification-listener onboarding step** — check `getEnabledListenerPackages()`, deep-link to `ACTION_NOTIFICATION_LISTENER_SETTINGS`, re-check on `onResume`.
    - _Requirements: 4.1, 11.1, 11.2, 11.3, 11.4_

- [ ] **13. `ListenerHealthMonitor`** — periodic re-check, prompts re-enable if silently revoked.
    - _Requirements: 4.5_

- [ ] **14. Render latest notification as plain text on `StandByActivity`** — no styling yet, just prove the data flow end to end.
    - _Requirements: 4.2, 4.3_

---

### Phase 3 — Settings, zen mode, exclude list

- [ ] **15. Extend `AppSettings`** — add `zenModeEnabled`, `excludedPackages`, `schemaVersion` bump + `SettingsMigration` scaffolding.
    - _Requirements: 10.3_

- [ ] **16. `ZenState` + `NotificationFilterUseCase`** — pure function `(rawList, zenState, excludeList) → DisplayableNotification`.
    - _Requirements: 4.4, 5.1, 5.2, 5.3_

- [ ] **17. Basic settings screen** — trigger mode, zen mode toggle, exclude-list picker (from currently-installed apps with notification access).
    - _Requirements: 5.3, 10.1, 10.4_

- [ ] **18. Wire `NotificationFilterUseCase` output into `StandByActivity`** — replace the plain-text placeholder from task 14.
    - _Requirements: 5.1, 5.2, 5.3_

---

### Phase 4 — Widget system (the extensibility refactor)

- [ ] **19. Define `WidgetSpec`, `WidgetDataSource` contracts** — no implementations yet, just the interfaces.
    - _Requirements: 9.1_

- [ ] **20. `WidgetRegistry`** — Hilt multibinding assembling `WidgetSpec` + Composable renderer pairs.
    - _Requirements: 9.1_

- [ ] **21. Refactor clock into a widget** — first implementation against the new contract.
    - _Requirements: 9.1, 9.3_

- [ ] **22. Refactor notification display into a widget** — second implementation; this is where contract gaps usually surface — fix the contract now, not later.
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

- [ ] **23. Add battery widget as the third proof case** — uses `PowerStateRepository.batteryPercent`; if this drops in cleanly, the contract is validated.
    - _Requirements: 9.1, 9.2, 9.3_

- [ ] **24. `LayoutEngine`** — pure function `(enabledWidgetIds, orientation, sizeClass) → arrangement`.
    - _Requirements: 6.4, 9.3_

- [ ] **25. `StandByScreenViewModel`** — reads enabled widgets from settings, subscribes only to needed data sources, asks `LayoutEngine` for arrangement.
    - _Requirements: 9.2, 9.4_

- [ ] **26. Widget enable/disable + reorder UI in settings** — writes `enabledWidgetIds` back through `SettingsRepository`.
    - _Requirements: 9.2, 10.1_

---

### Phase 5 — Orientation

- [ ] **27. `OrientationController`** — sets `requestedOrientation` from `SettingsRepository.orientationMode`.
    - _Requirements: 6.1, 6.2, 6.3_

- [ ] **28. Orientation setting UI** — landscape-only / portrait-only / auto.
    - _Requirements: 6.1, 6.2, 6.3_

- [ ] **29. Verify `LayoutEngine` produces distinct arrangements per orientation** — on-device check across all three modes.
    - _Requirements: 6.4 (manual verification)_

---

### Phase 6 — Theming

- [ ] **30. `ThemeConfig` model + `ThemeRepository`** — extend `AppSettings`, DataStore-backed.
    - _Requirements: 7.1, 8.1_

- [ ] **31. `FontResolver`** — bundled fonts first, then Downloadable Fonts API, local caching, guaranteed fallback.
    - _Requirements: 7.1, 7.2, 7.3_

- [ ] **32. `BackgroundRenderer`** — strategy map for solid / gradient / image / blurred-wallpaper.
    - _Requirements: 8.1, 8.2, 8.3_

- [ ] **33. `ThemeProvider` `CompositionLocal`** — wire into `StandByActivity` root; update all widgets to read font/colors from it instead of hardcoded values.
    - _Requirements: 7.1, 8.1_

- [ ] **34. Theme settings screen** — font picker, background type + params picker.
    - _Requirements: 7.1, 8.1, 8.2, 10.1_

---

### Phase 7 — Polish & resilience

- [ ] **35. Motion-based dismissal** — accelerometer-based pickup detection, toggleable in settings.
    - _Requirements: 3.2_

- [ ] **36. Battery-optimization exemption onboarding step** — `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` flow.
    - _Requirements: 12.4_

- [ ] **37. OEM-specific guidance screen** — detect known aggressive OEMs (MIUI/Samsung/etc.), show manual whitelist instructions.
    - _Requirements: 12.3 (mitigation, not a fix)_

- [ ] **38. Burn-in dimming** — subtle periodic pixel-shift while the StandBy screen is shown for extended periods.
    - _Non-functional (screen protection, not in requirements.md — add as requirement 13 if you want it tracked formally)_

- [ ] **39. Full manual regression pass** — walk every acceptance criterion in `requirements.md` (1.1 through 12.4) on-device before considering this "done."
    - _Requirements: all_

---

## How to use this with a coding agent

Feed one unchecked task at a time, along with `requirements.md` and `design.md` for context. A good prompt shape: *"Implement task 6 from tasks.md. Reference requirements 1.1–1.3 in requirements.md and the TriggerEvaluatorUseCase description in design.md §2."* Check the box, verify against the referenced acceptance criteria, move to the next task. Don't batch multiple phases into one session — the phase boundaries exist because task 9's manual checkpoint should genuinely gate whether Phase 2 is worth starting.