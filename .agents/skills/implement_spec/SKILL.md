---
name: implement_spec
description: Implements one task at a time from a spec-driven development task list (tasks.md), tracing every change back to specific acceptance criteria in requirements.md and the responsible component in design.md, checking off the task when verified, then stopping. Use this whenever the user asks to implement, continue, or run "the next task"; references tasks.md; asks to pick up spec-driven work on this project; says things like "do the next unchecked item," "continue where we left off," or "implement task N"; or when a project directory contains requirements.md, design.md, and tasks.md and the user asks for implementation work on it. Always prefer this over ad-hoc implementation when these three files are present.
---

# Spec Task Runner

Runs a spec-driven development loop against a project's `requirements.md` (EARS-format acceptance criteria), `design.md` (traceability from requirement to component), and `tasks.md` (ordered checkbox implementation plan). One task per invocation, always verified against the exact acceptance criteria it cites, never batched.

## Before starting

Locate `.agents/specs/requirements.md`, `.agents/specs/design.md`, and `.agents/specs/tasks.md` (check `.agents/specs/` first, then search the project if not found there). If any are missing, tell the user which one and stop — don't improvise a substitute or invent acceptance criteria.

If the user named a specific task ("implement task 6"), use that task regardless of checkbox state, but warn if any earlier task is still unchecked. Otherwise, find the **first unchecked (`- [ ]`) task in document order** — never skip ahead to a later unchecked task.

## The loop, per task

1. **Read context.** Pull the task's own description from `tasks.md`, then read every acceptance criterion it cites in `requirements.md` — including that requirement's user story, not just the bare numbered line, for the *why* behind the *what*. Cross-check `design.md`'s traceability table for which component(s) this touches and read that component's description.

2. **Check whether this is a manual/device checkpoint.** If the task's description says things like "manual validation checkpoint," "on your actual device," or otherwise describes testing rather than building: do not write code. Tell the user precisely what to test and how (walk through the referenced acceptance criteria as a manual test script), then wait for their confirmation before checking the box. This is a hard stop, not a suggestion — some things (lock-screen window flags, OEM battery behavior, notification listener rebinding) genuinely cannot be verified by reading code.

3. **Implement narrowly.** Build only what's needed to satisfy the cited acceptance criteria. If you notice something a *later* task will need, leave it for that task — don't pull work forward, even when it seems efficient. Pulling work forward breaks the traceability the whole point of this workflow is protecting.

4. **Verify explicitly.** For each acceptance criterion the task cites, state in plain terms how it's now satisfied — point at the specific code path, and if a unit test applies (per this project's testing rules, generally the domain layer), run it or note that it should be written as part of this task.

5. **Check the box.** Flip that one task's `- [ ]` to `- [x]` in `tasks.md`. Don't touch any other task's checkbox.

6. **Report and stop.** Summarize: task number/name, files touched, requirement IDs satisfied, verification performed, and the next unchecked task's number and name. Then stop — do not automatically continue into the next task or the next phase. If the user wants to keep going, they'll say so; spec-driven work is meant to be gated, especially across phase boundaries (see below).

## Phase boundaries are hard stops

If `tasks.md` is organized into phases (e.g. "Phase 1 — Trigger skeleton," "Phase 2 — Notifications"), each phase typically ends in a manual validation checkpoint task. Never begin the first task of a new phase until that checkpoint is checked off. If asked to jump into a later phase anyway, name the pending checkpoint explicitly and ask for confirmation before proceeding — don't silently comply and don't silently refuse either.

## If requirements, design, and reality disagree

`requirements.md` is the source of truth for *what* must be true; `design.md` is the source of truth for *which component* is responsible. If implementing a task reveals that the design doesn't actually satisfy the requirement — stop, explain the mismatch, and propose an update to `design.md` for the user to confirm rather than quietly improvising a fix that isn't reflected in the spec. The spec files should always describe what the code actually does.

## Tasks without numbered acceptance criteria

Some tasks (typically early scaffolding, e.g. "project setup") aren't tied to specific requirement IDs. For these, "done" means the described work exists and the project still builds — say that plainly rather than inventing a criterion to point to.

## Output format

Keep the per-task report short and consistent:

```
Task <N>: <name>
Files touched: <list>
Requirements satisfied: <IDs>
Verified: <one line per cited criterion, how it's met>
tasks.md: [x] updated
Next up: Task <N+1> — <name>
```

For manual-checkpoint tasks, replace "Files touched" / "Verified" with the on-device test steps and leave the checkbox unflipped until the user confirms the result.