# PrivacyPal progress log

## Phase 0 – Foundation (branch `phase-0-foundation`)

### 0.1 Project identity and dependencies
- Gradle project created by hand (no Studio template existed): single `:app` module,
  Gradle 9.8.2 wrapper, AGP 9.4.1 with built-in Kotlin 2.4.21, KSP 2.3.12, compile/target SDK 37, minSdk 29.
- `gradle/libs.versions.toml` with Compose BOM 2026.09.00, Material 3, Navigation Compose 2.10.2,
  Lifecycle 2.11.0, Hilt 2.60.1 + androidx.hilt 1.4.0, Room 2.8.5 (KSP), DataStore 1.3.0,
  WorkManager 2.12.0, Coroutines 1.11.0, and test libs (JUnit4, MockK, Turbine, Robolectric 4.17,
  Roborazzi 1.77.0, Compose UI test, androidx.test). All latest stable as of 2026-10-25.
- `PrivacyPalApplication` (`@HiltAndroidApp`), `MainActivity` (`@AndroidEntryPoint`) with splash screen.
- Manifest declares no permissions; cloud backup and device transfer are disabled (data stays on device).
- Verified: `assembleDebug` succeeds; installed and launched on moto g34 5G (Android 15); logcat clean.

### 0.2 Light and dark theme
- `ui/theme/`: `Color.kt` (full brand light/dark schemes, every M3 role incl. surface containers),
  `PrivacyPalColors.kt` (risk high/medium/low + on-colours via `LocalPrivacyPalColors`,
  `MaterialTheme.privacyPalColors`), `Type.kt`, `Shape.kt`, `Theme.kt` (dynamic colour on 12+, brand fallback).
- `MainActivity`: edge-to-edge, re-applied when the resolved dark mode changes so bar icons follow the app theme.
- Tests: `ContrastTest` checks >= 4.5:1 (WCAG) for risk/onRisk, risk/surface and key brand text pairs, both themes.
- Verified: build, unit tests (5/5), lint clean; on device in system dark and light: status bar icons readable,
  text contrast fine, logcat clean. System night mode restored to its original value (yes).
