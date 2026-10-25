# PrivacyPal

Open-source Android privacy app. Shows and controls what apps do behind the user's back:
permission auditor, mic/camera watchdog, local-VPN tracker blocker, tracker SDK scanner, privacy score.
Plan: docs/PLAN.md. Progress log: docs/PROGRESS.md.

## Identity

- App name: PrivacyPal. Package: com.privacypal.app
- Promise: everything stays on the device. No accounts, no servers, no analytics.

## Stack

- Kotlin, Jetpack Compose + Material 3, Navigation Compose, Hilt, Room (KSP), DataStore,
  Coroutines/Flow, WorkManager. minSdk 29, target/compile SDK = latest stable.
- Tests: JUnit4, MockK, Turbine, Robolectric, Roborazzi (screenshots), Compose UI tests.
- Quality: Android Lint, ktlint (or Spotless), Detekt.
- Use latest stable library versions via gradle/libs.versions.toml. No alpha/beta libraries.

## Architecture

```
com.privacypal.app/
  ui/        screens, components, theme/, navigation/ (ViewModels next to screens)
  domain/    use cases, RiskRules, PrivacyScore, models (pure Kotlin, no Android imports)
  data/      Room db, DAOs, repositories, scanners (PackageManager wrappers)
  service/   WatchdogService, PrivacyPalVpnService
  work/      WorkManager workers
  di/        Hilt modules
```

- Services and workers write to Room; UI reads through repositories/use cases via StateFlow.
- Android system APIs are wrapped behind interfaces so they can be faked in tests.

## Theme rules (light + dark)

- Every screen must look correct in light AND dark mode.
- Use MaterialTheme.colorScheme tokens only. No hard-coded Color(...) in screens.
- Dynamic color on Android 12+, PrivacyPal brand scheme as fallback (both light and dark).
- Risk colors (high/medium/low) come from a PrivacyPalColors extension with light and dark values;
  risk is never shown by color alone (always an icon or label too).
- Theme setting: System / Light / Dark, stored in DataStore, applied instantly.
- Edge-to-edge enabled; content respects system bar insets.
- Text contrast at least 4.5:1 in both themes.

## Hard rules

- No network access except: VPN DNS forwarding and blocklist downloads the user triggers.
- No analytics, crash-reporting, ads or any SDK that sends data. Ever.
- Never request RECORD_AUDIO or CAMERA permissions in release builds.
- Debug-only helpers live in src/debug/ and never ship in release.
- Never commit secrets: keystores, local.properties, passwords.
- Strings in res/values/strings.xml (no hard-coded UI text). contentDescription on every icon.

## Definition of Done (a feature is done only when ALL pass)

Verification is done on the phone, not with new test files. Do not write new unit, screenshot or
UI test files; keep the existing tests compiling and passing (minimal fixes only) so CI stays green.

1. `./gradlew assembleDebug` succeeds with no new warnings you can fix.
2. Existing checks pass: `./gradlew testDebugUnitTest verifyRoborazziDebug`.
3. `./gradlew lintDebug` and the formatter/static checks (spotless, detekt) pass (or warnings are explained).
4. On the connected phone (`adb devices`): install, launch, exercise every part of the feature with adb,
   take screenshots in light and dark (`adb shell cmd uimode night yes|no`), open them and check:
   no clipped/overlapping text, readable contrast, correct spacing, no placeholder text.
   Check Logcat for crashes or errors from com.privacypal.app. Restore the phone's own settings
   (night mode, rotation, any permission changed with `pm revoke/grant`) afterwards.
5. Before sending any adb input, confirm PrivacyPal is the foreground app; if the user is using
   the phone, stop and ask.
6. Edge cases checked on the phone: permission denied, empty data, rotation, process death,
   Android 10 vs latest (note when Android 10 could not be checked on a device).
7. docs/PROGRESS.md updated with what was built and how it was verified on the phone.

## Git protocol

- Work on a branch per phase: `phase-N-short-name`. Merge to main at phase end.
- ONE commit per working feature, only after its Definition of Done passes.
- Conventional commits: feat:, fix:, test:, refactor:, chore:, docs:. Body says how it was verified.
- Never commit failing builds or tests. If something breaks, fix it before committing.
- Push the branch after each commit (`git push`). Tag phase completion: `v0.N.0`.

## Working style

- Before coding a phase, propose a plan and list files you'll create.
- Build in small steps; run the build/tests after each step, not only at the end.
- If an Android API behaves differently than expected, check developer.android.com and say so.
- If blocked (missing device, permission dialog needs a human tap), stop and tell me exactly
  what to do on the phone.
