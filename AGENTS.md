# Repository Automation Policy

Repository: `Leo-Mala/Chat-Basket-Manager-`
Default branch: `main`

## Operating mode

Automation must work from the real current GitHub state and follow this cycle:

**INVESTIGATE -> IMPLEMENT -> TEST -> FIX -> CERTIFY -> ADVANCE**

Do not stop after producing a report when a safe, evidence-based repository change is required and can be completed with the available GitHub access.

## Git and branch rules

- Always re-read the current `main` HEAD before starting a new change.
- Create automation work from the current `main` using a dedicated branch, preferably `automation/<short-purpose>`.
- Do not force-push.
- Do not rewrite published history.
- Do not bypass branch protections, required checks, or security gates.
- Do not push development changes directly to `main`; use a pull request.
- Before merge, verify that the PR head SHA being certified is still the current PR head.
- Merge only when the required CI for that exact head is green and there are no unresolved blocking review threads or conflicts.

## Required CI gates

Every code or build-system PR must keep and pass the repository GitHub Actions gates:

1. `Android Build & Test`
2. `Android Lint`
3. `Security Audit`

The Android build gate must continue to run unit tests, build the debug APK, build release outputs, run instrumented Android tests, verify expected artifacts, and upload test/build artifacts.

If a gate fails, inspect the failing job/step logs, correct the underlying cause, push the correction to the same PR branch, and certify the new exact head. Do not weaken or skip a test merely to obtain a green result.

## Security and credentials

- Never commit secrets, private keys, production keystores, tokens, passwords, or API credentials.
- Keep workflow permissions at the least privilege required by each job.
- Existing repository secrets may be consumed only through GitHub Actions secret references.
- Do not print secret values to logs.
- Do not replace a real security gate with a no-op or `continue-on-error` workaround.

## Build and release discipline

- Preserve the Gradle Wrapper and Android toolchain required by the project.
- Do not change application data or gameplay behavior merely to make tests pass.
- Keep generated APK/AAB artifacts available through GitHub Actions when the build is valid.
- Do not create a GitHub Release, tag, or publish an official production version unless the user explicitly requests that release step.

## Automation reporting

For scheduled automation runs, report the current branch/PR, exact head SHA, what changed, tests/checks run, failures being corrected, artifact availability, blockers, and the next concrete repository action.

## Shared Codex budget and model escalation

The repository owner actively develops three projects with the same Codex allowance:

- `Leo-Mala/Football-Dynasty`: target up to 35% of the shared weekly Codex budget;
- `Leo-Mala/Chat-Pro-Football`: target up to 30%;
- `Leo-Mala/Chat-Basket-Manager-`: target up to 20%;
- keep at least 15% unassigned as a shared reserve for blockers, regressions, CI failures and manual-test fixes.

These percentages are planning targets, not permission to spend quota merely because it is available. This repository must stay within its target whenever practical so the other two projects can continue through the same week. ChatGPT Work is not part of this plan and must not be assumed as an available budget or execution path.

### Codex usage rules

- Use Codex only when it materially reduces engineering time or improves coverage. Do not spend Codex quota on routine HEAD/status checks, PR metadata, simple GitHub operations, trivial one-file edits or work that direct GitHub tools can complete safely.
- Prefer `Luna` for high-volume and repetitive work such as repository search, call-site mapping, inventory, log triage, documentation, straightforward test generation and mechanical comparisons.
- Escalate to `Terra` for normal implementation, debugging, persistence/Room work, non-trivial tests, CI root-cause analysis and integrated code changes when Luna is not sufficient.
- Use `Sol` only for genuinely difficult blockers, architecture-sensitive problems, subtle concurrency/determinism/persistence failures, or cases where the lower tier has proved inadequate. Do not make Sol the default.
- Use the lowest reasoning/effort level that can safely complete the task. Reserve maximum/high-cost modes for blocked or demonstrably complex work.
- Batch related investigation and implementation work so the same repository context and evidence are reused. Do not launch duplicate agents to repeat an investigation that is already certified or still valid for the exact current HEAD.
- Reuse valid test, audit and CI evidence when repository policy permits it. Do not rerun expensive Codex work merely to restate status.

### Shared-quota guardrails

When reliable Codex usage information is available, manage the shared weekly allowance conservatively:

- below 50% consumed: normal economical use;
- 50-70% consumed: favor Luna and batch work more aggressively;
- 70-85% consumed: use Codex only for tasks that materially advance a project or remove a real blocker;
- above 85% consumed: preserve the remainder for critical regressions, CI blockers and APK/manual-test fixes until the allowance resets.

If reliable usage information is unavailable, never invent a percentage. Default to the conservative policy: Luna first, no duplicate agents, batch related work, and escalate only when justified.

Do not assume extra paid credits, automatic top-ups or an additional per-project quota. Repository correctness, security, required tests, explicit user instructions and existing project safeguards always take priority over quota savings; never weaken a gate or implementation merely to reduce Codex consumption.
