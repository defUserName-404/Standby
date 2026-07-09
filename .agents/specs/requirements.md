# StandBy-Clone — Requirements Specification

**Format:** each requirement has a user story and acceptance criteria written in EARS syntax (Easy Approach to Requirements Syntax): `WHEN <trigger> THE SYSTEM SHALL <response>`, `IF <condition> THEN THE SYSTEM SHALL <response>`, `WHILE <state> THE SYSTEM SHALL <response>`. This phrasing is deliberate — it's unambiguous enough to test against and to hand to a coding agent one requirement at a time.

Acceptance criteria are numbered `<requirement>.<criterion>` (e.g. `4.3`) so `design.md` and `tasks.md` can reference them precisely.

---

## 1. Screen-Off Trigger Display

**User story:** As the user, I want the StandBy screen to appear automatically when my screen turns off under conditions I've configured, so I get a glanceable display without any manual action.

1.1 WHEN the screen turns off AND trigger mode is "charging only" AND the device is charging, THE SYSTEM SHALL launch the StandBy screen within 1 second.
1.2 WHEN the screen turns off AND trigger mode is "always", THE SYSTEM SHALL launch the StandBy screen regardless of charge state.
1.3 WHEN charging begins AND the screen is already off AND trigger mode includes charging, THE SYSTEM SHALL launch the StandBy screen.
1.4 IF the StandBy screen is already showing THEN THE SYSTEM SHALL NOT launch a duplicate instance.
1.5 WHEN the device finishes rebooting, THE SYSTEM SHALL resume trigger monitoring without requiring the user to manually reopen the app.

## 2. Lock-Screen Overlay Display

**User story:** As the user, I want the StandBy screen to display over the lock screen without unlocking my phone, so my device stays secure while still showing me information.

2.1 WHEN the StandBy screen launches while the device is locked, THE SYSTEM SHALL display it without dismissing the keyguard.
2.2 WHEN the StandBy screen launches, THE SYSTEM SHALL turn the display on if it was off.
2.3 IF the user authenticates (biometric/PIN/pattern) while the StandBy screen is showing, THEN THE SYSTEM SHALL transition to the normal unlocked state.
2.4 WHILE the StandBy screen is visible, THE SYSTEM SHALL keep the display on, overriding the normal screen-timeout setting.

## 3. Dismissal Behavior

**User story:** As the user, I want the StandBy screen to go away when I pick up or interact with my phone, so it never gets in the way of normal use.

3.1 WHEN the user taps the StandBy screen, THE SYSTEM SHALL dismiss it and return to the standard lock/home screen.
3.2 WHERE motion-based dismissal is enabled, WHEN the device is picked up or moved, THE SYSTEM SHALL dismiss the StandBy screen.
3.3 WHEN the screen subsequently turns off again, THE SYSTEM SHALL close the StandBy activity if still open and re-arm trigger monitoring for the next event.

## 4. Notification Access & Display

**User story:** As the user, I want to see my latest notification on the StandBy screen, so I can stay informed at a glance without picking up my phone.

4.1 IF notification listener access has not been granted, THEN THE SYSTEM SHALL show an explanation and a direct link to the relevant system settings screen.
4.2 WHEN a new notification is posted AND notification access is granted, THE SYSTEM SHALL capture it and make it available to the notification widget.
4.3 WHEN a notification is dismissed or removed, THE SYSTEM SHALL update the displayed state within the current StandBy session without requiring the screen to relaunch.
4.4 IF a notification's originating app is on the exclude list, THEN THE SYSTEM SHALL NOT display it.
4.5 IF the system detects notification listener access has been silently revoked, THEN THE SYSTEM SHALL prompt the user to re-enable it.

## 5. Zen Mode

**User story:** As the user, I want to toggle whether notifications appear on the StandBy screen, so I can choose between a distraction-free display and an informative one.

5.1 WHILE zen mode is enabled, THE SYSTEM SHALL NOT display any notification content on the StandBy screen.
5.2 WHILE zen mode is disabled, THE SYSTEM SHALL display the single most recent non-excluded notification, if any exists.
5.3 WHEN the user changes the zen mode setting while the StandBy screen is visible, THE SYSTEM SHALL update the display immediately, without requiring dismissal and relaunch.

## 6. Orientation Support

**User story:** As the user, I want the StandBy screen to support both portrait and landscape, so I can use it however my phone happens to be positioned.

6.1 WHERE orientation mode is set to "landscape only", THE SYSTEM SHALL lock the StandBy screen to landscape.
6.2 WHERE orientation mode is set to "portrait only", THE SYSTEM SHALL lock the StandBy screen to portrait.
6.3 WHERE orientation mode is set to "auto", THE SYSTEM SHALL follow the device's physical orientation.
6.4 WHEN the effective orientation changes, THE SYSTEM SHALL re-arrange enabled widgets using the layout appropriate to the new orientation.

## 7. Font Customization

**User story:** As the user, I want to choose the font used on the StandBy screen, so I can personalize its appearance.

7.1 WHEN the user selects a font in settings, THE SYSTEM SHALL apply it to all text on the StandBy screen.
7.2 IF a downloadable font fails to load, THEN THE SYSTEM SHALL fall back to the system default font without crashing or leaving text unrendered.
7.3 WHEN a font is selected for the first time and requires download, THE SYSTEM SHALL cache it locally for offline reuse on subsequent launches.

## 8. Background Customization

**User story:** As the user, I want to customize the StandBy screen's background, so it matches my preference.

8.1 WHEN the user selects a background type (solid / gradient / image / blurred wallpaper), THE SYSTEM SHALL render that background on the StandBy screen.
8.2 WHEN the user selects a custom image as the background, THE SYSTEM SHALL persist a reference to it and apply it on every subsequent launch.
8.3 IF no background has been configured, THEN THE SYSTEM SHALL apply a sensible default.

## 9. Widget System / Extensibility

**User story:** As the developer (myself), I want to add new widgets without modifying existing widgets or core screen logic, so the app can grow over time without regressions.

9.1 WHEN a new widget is registered in the widget registry, THE SYSTEM SHALL make it available for enabling in settings without any change to the StandBy screen, the layout engine, or any other widget's code.
9.2 WHEN a widget is disabled in settings, THE SYSTEM SHALL exclude it from the layout and SHALL NOT subscribe to its underlying data source.
9.3 WHEN multiple widgets are enabled, THE SYSTEM SHALL arrange them according to each widget's declared size class and the current orientation.
9.4 IF a widget's data source throws an error, THEN THE SYSTEM SHALL render that widget in a safe empty/error state without crashing the StandBy screen.

## 10. Settings Persistence

**User story:** As the user, I want my settings to persist across app restarts and device reboots, so I never have to reconfigure the app.

10.1 WHEN the user changes any setting, THE SYSTEM SHALL persist it durably before returning control to the UI.
10.2 WHEN the app is relaunched or the device rebooted, THE SYSTEM SHALL restore the previously saved settings.
10.3 IF the persisted settings schema version is older than the app's current expected schema, THEN THE SYSTEM SHALL migrate the data forward without losing user-configured values where a mapping exists.
10.4 WHEN two settings are changed in quick succession (e.g. from two different screens), THE SYSTEM SHALL apply both changes without either silently overwriting the other.

## 11. Permission Onboarding

**User story:** As the user, I want to be guided through granting the permissions the app needs, so I understand what's required and why.

11.1 WHEN the app is launched for the first time, THE SYSTEM SHALL present an onboarding flow listing every required permission or special-access grant.
11.2 WHEN the user selects a permission step, THE SYSTEM SHALL deep-link directly to the corresponding system settings screen.
11.3 WHEN the user returns to the app from system settings, THE SYSTEM SHALL re-check the live permission state and update the onboarding UI accordingly.
11.4 IF a required permission is missing, THEN THE SYSTEM SHALL clearly indicate which features are degraded as a result.

## 12. Background Service Resilience

**User story:** As the user, I want the trigger-detection service to keep running reliably, so the StandBy screen keeps working over time without me having to relaunch the app.

12.1 WHILE the monitoring service is running, THE SYSTEM SHALL display the persistent foreground-service notification required by the OS.
12.2 WHEN the device finishes rebooting, THE SYSTEM SHALL automatically restart the monitoring service.
12.3 IF the OS terminates the monitoring service, THEN THE SYSTEM SHALL request it be restarted via the appropriate service flags.
12.4 WHERE battery-optimization exemption has not yet been granted, THE SYSTEM SHALL prompt the user to grant it during onboarding.

---

## Non-Functional Requirements

- **Latency:** the StandBy screen SHALL be visibly on-screen within 1 second of a qualifying trigger event under normal system load.
- **Privacy:** notification content captured by the notification listener SHALL remain on-device; the system SHALL NOT transmit it over the network.
- **Stability:** a failure in any single widget's data source SHALL NOT crash the StandBy screen or the monitoring service (see 9.4).
- **Compatibility:** minimum SDK is API 27 (required for `setShowWhenLocked`/`setTurnScreenOn`); target the latest stable API.
- **Extensibility:** adding a new widget or a new background type SHALL require touching only that widget's/type's own files plus one registry entry (see 9.1).

## Out of Scope (this spec)

- Play Store distribution requirements (data-safety form, notification-access policy declaration) — only relevant if this stops being personal-use-only.
- Multi-user / work-profile support.
- Cross-device sync of settings.
- Any always-on-display hardware integration beyond software-level burn-in mitigation.