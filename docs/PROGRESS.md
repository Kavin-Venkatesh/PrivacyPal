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

### 0.3 Theme setting
- `domain/model/ThemeMode`, `ThemeSettings`; `domain/theme/ThemeResolver.kt` (pure Kotlin).
- `data/settings/SettingsRepository` + `DataStoreSettingsRepository` (Preferences DataStore, corruption handler,
  unknown values fall back to SYSTEM); Hilt `DataStoreModule`, `RepositoryModule`.
- `ui/MainViewModel` (StateFlow `Loading`/`Ready`); `MainActivity` applies settings instantly and holds the splash
  until loaded.
- Tests: `DataStoreSettingsRepositoryTest` (6, temp-file DataStore), `ThemeResolverTest` (5), `MainViewModelTest` (2).
  Note: the repository test uses `OkioStorage` because DataStore's default `FileStorage` cannot replace files
  on Windows hosts (`File.renameTo`); production on Android is unaffected.
- Verified: build, 18/18 unit tests, lint clean; launched on device, follows system dark mode, logcat clean.
  In-app switching is verified on device in 0.4 once the Settings screen exists.

### 0.4 Navigation shell
- `ui/navigation/Destinations.kt` (`@Serializable` routes + `TopLevelDestination`), `PrivacyPalNavHost.kt`,
  `ui/PrivacyPalApp.kt` (Scaffold + NavigationBar, save/restore tab state, Back -> Home -> exit).
- `ui/components/EmptyState.kt`; Home/Apps/Activity placeholder screens with real empty-state copy.
- `ui/settings/SettingsScreen.kt` + `SettingsViewModel`: theme radio group, dynamic colour switch (API 31+ only),
  About (version, GPL-3.0, "Everything stays on your phone"). `LICENSE` (GPL-3.0) added.
- Tests: `SettingsViewModelTest` (4). Total 22 unit tests.
- Device (moto g34, Android 15): all four tabs in light and dark; in-app theme overrides the system both ways
  (status bar icons follow); brand scheme with dynamic colour off; rotation and process death keep the tab;
  Back behaviour correct; logcat clean. Found and fixed: tab labels were hidden from TalkBack.

### 0.5 Test and quality toolchain
- Roborazzi: `captureLightAndDark` helper + `InAppShell` (screen inside the real nav bar); `ScreensScreenshotTest`
  records Home, Apps, Activity, Settings and Settings-without-dynamic-colour (Android 10/11), each light and dark:
  10 baselines in `app/src/test/screenshots/`. Extracted `PrivacyPalNavigationBar` so it renders without Hilt.
- Reviewed every PNG: no clipping/overlap, readable contrast, consistent spacing. Fixed ambiguous Activity copy
  ("contact trackers") and re-recorded.
- `NavigationTest` (Compose UI, androidTest): taps every tab and asserts its screen; Back returns Home.
- Spotless + ktlint 1.8.0 (`.editorconfig`, Android Studio style), detekt 1.23.8 (`config/detekt/detekt.yml`,
  Compose-friendly overrides), lint `abortOnError`. All findings fixed. `docs/TESTING.md` explains the commands.
- Verified: assembleDebug, testDebugUnitTest (27), verifyRoborazziDebug, lintDebug (no issues), spotlessCheck,
  detekt all pass; connectedDebugAndroidTest 2/2 on moto g34 (Android 15).

### 0.6 Continuous integration
- `.github/workflows/android.yml`: push + pull_request, ubuntu-latest, Temurin 21, Gradle caching; runs
  assembleDebug, testDebugUnitTest, verifyRoborazziDebug, lintDebug, detekt, spotlessCheck; uploads reports on failure.
- Verified: first run green (GitHub Actions run 40167737064). Screenshot baselines recorded on Windows verify
  on Linux without a tolerance.

### Phase 0 end
- Final device check (moto g34, Android 15): all four tabs in light and dark, logcat clean, night mode restored.
- Known cosmetic issue: commit fc67b2f's subject starts with a UTF-8 BOM (left as is; no history rewrite).
- Merged to main, tagged `v0.0.1`.

## Phase 1 – Permission Auditor (branch `phase-1-permission-auditor`)

### 1.1 Package scanner
- `domain/model/PermissionGroup` (14 groups; background location separate), `InstalledApp`, `AppCategory`,
  `PermissionState`; `domain/permissions/PermissionGroups` maps ~50 raw permission names (incl. Android 16
  health permissions) to groups; `domain/apps/visibleApps` hides system apps by default.
- `data/scanner/InstalledAppsSource` + `PackageManagerAppsSource` (PackageInfoFlags on API 33+, int flags below;
  granted = `REQUESTED_PERMISSION_GRANTED`; PrivacyPal excluded), `PackageInfoMapper`, lazy `AppIconLoader`
  (LRU cache, never stored). Hilt `ScannerModule`, `DispatchersModule` (`@IoDispatcher`).
- "System app" follows the system permission manager: preinstalled AND no launcher icon (so Chrome-like
  preinstalled apps stay visible).
- Manifest: `QUERY_ALL_PACKAGES` with a comment on the Play permissions declaration.
- Tests: `PermissionGroupsTest`, `PackageInfoMapperTest` (Robolectric), `PackageManagerAppsSourceTest`
  (Robolectric ShadowPackageManager), `AppListFilterTest`; device test `PackageManagerAppsSourceDeviceTest`.
- Verified: build, 48 unit tests, lint/detekt/spotless clean, screenshots unchanged; connectedDebugAndroidTest 4/4
  on moto g34 (scanner sees the installed apps, not itself; Settings visible with launcher icon).

### 1.2 Persistence
- Room database `privacypal.db` v1 (`data/db`): `AppEntity`, `PermissionGrantEntity` (FK cascade, index on group),
  `AppWithGrants`, `AppDao` with transactional `replaceAll` / `replaceApp` (chunked so Android 10's 999-variable
  SQLite limit is never hit). Schema exported to `app/schemas/` via the Room Gradle plugin.
- `data/apps/AppsRepository` + `RoomAppsRepository`: `apps` Flow, `app(pkg)`, `refresh()` / `refresh(pkg)` behind a
  mutex, `refreshState` (a failed full scan keeps the previous data and reports FAILED).
- `AppsRefresher` (started from `PrivacyPalApplication`): full rescan whenever the app comes to the foreground
  (`ProcessLifecycleOwner`), single-package rescan on PACKAGE_ADDED/REMOVED/REPLACED (runtime receiver, not exported).
- Tests: `AppDaoTest` (7, incl. 1200 apps over the variable limit, cascade, Flow emissions), `RoomAppsRepositoryTest` (9).
- Verified: build, 64 unit tests, lint/detekt/spotless clean, screenshots unchanged. On the moto g34: database holds
  464 apps (360 system, 104 visible), 1420 grant rows, PrivacyPal itself excluded. Stored grants match
  `dumpsys package` exactly for WhatsApp (16), Truecaller (12) and Subway Surfers (0). Installing a package while
  the app was open added its row; uninstalling removed it. No errors from PrivacyPal in logcat.
