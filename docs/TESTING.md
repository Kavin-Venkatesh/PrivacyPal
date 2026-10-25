# Testing and quality checks

All commands run from the repository root. On Windows use `gradlew.bat` (or `./gradlew` in Git Bash).

## Everyday loop

| Command | What it does |
|---|---|
| `./gradlew assembleDebug` | Builds the debug APK. |
| `./gradlew testDebugUnitTest` | JVM unit tests (JUnit4, MockK, Turbine, Robolectric). Screenshot tests also run here but do not record or compare. |
| `./gradlew lintDebug` | Android Lint. Errors fail the build (`abortOnError = true`); report in `app/build/reports/lint-results-debug.html`. |
| `./gradlew spotlessCheck` | ktlint formatting check (Android Studio style, 120 columns, see `.editorconfig`). |
| `./gradlew spotlessApply` | Fixes formatting in place. |
| `./gradlew detekt` | Static analysis with `config/detekt/detekt.yml` on top of the defaults. Report in `app/build/reports/detekt/`. |

Run everything CI runs:

```
./gradlew assembleDebug testDebugUnitTest verifyRoborazziDebug lintDebug spotlessCheck detekt
```

## Screenshot tests (Roborazzi)

Screenshot tests live in `app/src/test/.../ui/ScreensScreenshotTest.kt`. Each screen is rendered inside the
real bottom navigation bar, once in light and once in dark (`captureLightAndDark` in
`app/src/test/.../testing/ThemedScreenshot.kt`), using the brand colour scheme so output is deterministic.
Baselines are committed in `app/src/test/screenshots/`.

| Command | What it does |
|---|---|
| `./gradlew recordRoborazziDebug` | Re-renders and overwrites the baseline PNGs. Run after an intended UI change, then open the PNGs and check them (clipping, overlap, contrast, spacing, no placeholder text) before committing. |
| `./gradlew verifyRoborazziDebug` | Renders and compares against the baselines; fails on any difference. Diffs are written next to the outputs in `app/build/outputs/roborazzi/`. |
| `./gradlew compareRoborazziDebug` | Like verify, but only reports differences without failing. |

New or changed screens need a screenshot test in both themes (see CLAUDE.md, Definition of Done).

## Instrumented tests (needs a phone or emulator)

| Command | What it does |
|---|---|
| `./gradlew connectedDebugAndroidTest` | Installs the app and runs `app/src/androidTest` (Compose UI tests, e.g. `NavigationTest`) on every connected device. Report in `app/build/reports/androidTests/connected/`. |

Check `adb devices` first. The Gradle task uninstalls the app when it finishes, so reinstall with
`./gradlew installDebug` afterwards if you want to keep using it.

## Manual device check

```
adb shell cmd uimode night            # note the current value
adb shell cmd uimode night no         # light
adb exec-out screencap -p > shots/home_light.png
adb shell cmd uimode night yes        # dark
adb exec-out screencap -p > shots/home_dark.png
adb logcat -d '*:E' | grep -i privacypal
```

Restore the original night mode afterwards. `shots/` is gitignored.

## Notes

- Robolectric tests need JVM flags for native graphics on JDK 17+; they are set in `app/build.gradle.kts`
  (`testOptions.unitTests.all`).
- `DataStoreSettingsRepositoryTest` uses DataStore's Okio storage because the default `FileStorage` cannot
  replace files on Windows hosts.
- detekt 1.23.8 (latest stable) is built against Kotlin 2.0; its Gradle configuration is pinned to that
  Kotlin version so it can analyse this Kotlin 2.4 project.
