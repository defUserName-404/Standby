# AGENTS.md

Context for any AI coding agent (Claude Code, or otherwise) working in this repository. Read this
before making changes.

## Project

Android StandBy-clone: a personal-use app that shows a glanceable full-screen display over the lock
screen (clock, notifications, battery, etc.), built with Jetpack Compose, Clean Architecture, no
feature gating. Full rationale in `.agents/specs/project_plan.md` and
`.agents/specs/architecture.md` — background reading, not authoritative (see precedence rule
below)..

## This repo follows spec-driven development — read these first, in order

1. `.agents/specs/requirements.md` — every feature as EARS-format acceptance criteria (`1.1`, `4.3`, etc.). This is what "correct" means.
2. `.agents/specs/design.md` — traces each requirement to the component that implements it, plus data models and error handling.
3. `.agents/specs/tasks.md` — the ordered, checkbox implementation plan. Every task cites the acceptance criteria it satisfies.

**Precedence if documents conflict:** `.agents/specs/requirements.md` wins on *what* must be true. `.agents/specs/design.md` wins on *which component* is responsible. `.agents/specs/project_plan.md` and `.agents/specs/architecture.md` are rationale/background only — if they disagree with `requirements.md` or `design.md`, the spec
files win. Flag the conflict to the user rather than silently picking one.

## Default workflow

Unless told otherwise, work from the next unchecked (`- [ ]`) task in `tasks.md`, in order:

- Implement only what the task's cited acceptance criteria require — nothing from a later task, even
  if convenient.
- After implementing, explicitly state how each cited criterion is satisfied.
- Check the task's box in `tasks.md`.
- Report what changed, which requirements it satisfies, and what's next.
- **Stop after one task.** Don't chain into the next task or the next phase without being asked.
  This is deliberate — see `tasks.md`'s phase-boundary notes.
- Tasks marked as manual/device checkpoints (e.g. "manual validation checkpoint" tasks) are never
  satisfied by writing code. State what to test on-device and wait for confirmation before checking
  the box.

If you have the `implement_spec` skill available, prefer it for this workflow — it encodes these rules as an invokable procedure.

## Architecture rules (non-negotiable)

- Dependency direction is `Presentation → Domain ← Data`. Nothing in `domain/` may import an Android
  framework type (no `Context`, no Compose, no `NotificationListenerService`).
- **New widget** = implement `WidgetSpec` + `WidgetDataSource` (if new data is needed) + one
  `@Composable` renderer, registered in `WidgetRegistry`. Never modify `StandByActivity`,
  `LayoutEngine`, or another widget's files to add one.
- **New background type** = same pattern, via `BackgroundRenderer`'s strategy map.
- Settings are a single `AppSettings` aggregate root in one DataStore entry, written only through
  the repository's transactional `update { current -> current.copy(...) }` path. Never add a second
  DataStore key or a parallel settings store.
- Do not use `SYSTEM_ALERT_WINDOW`. The lock-screen overlay is a normal `Activity` using
  `setShowWhenLocked`/`setTurnScreenOn` — see `design.md` and the architecture doc, §1.
- Do not manifest-register `BroadcastReceiver`s for `SCREEN_ON`/`SCREEN_OFF`/power broadcasts —
  these are implicit broadcasts blocked since API 26. Register dynamically inside
  `StandByMonitorService`.

## Code conventions

- Kotlin, Jetpack Compose, Material3.
- Hilt for dependency injection, including widget/background-type registries via multibinding.
- Coroutines + `Flow`/`StateFlow` for all async and reactive state. No callbacks-as-API, no
  LiveData.
- `kotlinx.serialization` for persistence (DataStore stores one serialized JSON blob), not
  Gson/Moshi.
- One widget = one self-contained package under `widgets/<name>/` holding its spec, data source, and
  composable together.

## Build & test

- Build: `./gradlew assembleDebug`
- Unit tests (domain + data layers only — this is where tests belong, see below):
  `./gradlew testDebugUnitTest`
- Install to a connected/emulated device: `./gradlew installDebug`
- Lint: `./gradlew lint`

## What gets unit tested vs. what doesn't

- **Unit test:** everything in `domain/` (`TriggerEvaluatorUseCase`, `NotificationFilterUseCase`,
  `LayoutEngine`) and repository logic against fakes/in-memory DataStore. Plain JUnit, no Android
  framework, no `Robolectric`.
- **Manual/device only, by design:** `ScreenPowerReceiver`, `StandByNotificationListenerService`,
  window-flag/lock-screen behavior, OEM battery-manager interactions. Don't attempt to unit-test
  these or fake up an instrumentation harness for them — validate on-device against the relevant
  `requirements.md` acceptance criteria instead.

## Definition of done for any task

1. Code compiles; relevant unit tests pass.
2. The specific acceptance criteria cited by the task are verifiably met (state how).
3. `tasks.md` checkbox updated for that task only.
4. No files touched outside what the task requires.

## Hard "don't"s

- Don't skip ahead to a later task while an earlier one is unchecked, unless explicitly told to.
- Don't start a new phase in `tasks.md` before its predecessor's manual checkpoint task is checked
  off.
- Don't add a permission, dependency, or architectural pattern not called for by `requirements.md`/
  `design.md` without flagging it to the user first.
- Don't touch `.agents/specs/project_plan.md` or `.agents/specs/architecture.md` to resolve a spec conflict — update `.agents/specs/requirements.md`/`.agents/specs/design.md` instead, with the user's confirmation.