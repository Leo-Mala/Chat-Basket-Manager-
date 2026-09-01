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
