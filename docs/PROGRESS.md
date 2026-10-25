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

### 1.3 Risk rules
- `domain/risk`: `RiskLevel` (NONE < LOW < MEDIUM < HIGH), `RiskReason` (17 plain-language reasons, mapped to
  strings in `ui/risk/RiskText.kt`), `RiskRules` (category overrides as a data list + an exhaustive per-group
  default), `assessApp()` → `AppRisk` (worst granted group, grants worst first, `flaggedCount` = MEDIUM/HIGH grants).
- Apps declaring no category get MEDIUM with "This app doesn't say what it's for, so check whether it really needs
  this." Background location, SMS and call logs stay HIGH regardless (MEDIUM only for Maps / Social).
- Tests: `RiskRulesTest` checks all 140 group × category combinations against an independent copy of the approved
  table, plus reasons; `AppRiskTest` (no permissions, denied ignored, worst-of aggregation, ordering, counts);
  `RiskTextTest` (every reason/level has a distinct string; apostrophes render).
- detekt: `CyclomaticComplexMethod.ignoreSimpleWhenEntries` so flat enum → resource mappings pass.
- Verified: build, 81 unit tests, lint/detekt/spotless clean, screenshots unchanged. No UI yet, so no device check.

### 1.4 Apps list
- `domain/apps/AppQuery` (search by label/package, filters All/High risk/Camera/Microphone/Location incl.
  background/Contacts, sorts Risk/Name/Recently installed, system toggle) and `AppsSummary` counts.
- `ui/apps/AppsViewModel`: query kept in `SavedStateHandle`; Loading / Error (first scan failed, nothing stored) /
  Content (with a "couldn't refresh, showing last check" banner when a rescan fails).
- `AppsScreen`: summary card (tap a line → filter + scroll to the results), search, filter chips, sort menu, system
  apps switch, rows with lazily loaded app icon, up to 5 permission icons and a `RiskBadge` (icon + text, never colour
  alone). Each row is read by TalkBack as one sentence (name, risk, permissions). All text in strings.xml.
- From this feature on, verification is on the phone (per user request: no new test files). Existing tests kept
  green; the half-recorded Apps screenshot baselines were dropped.
- Verified on moto g34 (Android 15): 104 apps / 17 high risk shown, real icons; search "whats" → WhatsApp (Review);
  Microphone chip, summary tap → Camera filter and scrolls to results; sort by name; system toggle → 464 apps;
  light and dark; rotation and process death keep filter; logcat clean. Night mode restored.

### 1.5 App detail
- Navigation: nested `AppsGraph` (list + `AppDetailRoute(packageName)`), so the Apps tab stays selected on detail.
- `ui/apps/detail`: `AppDetailViewModel` (package from the route; Loading / NotInstalled / Content),
  `AppDetailScreen` (icon, name, package, locale-formatted install date, risk badge; each granted group with icon,
  badge and plain-language reason; "Asked for, but not allowed" list), `ManagePermissionsIntent`
  (`ACTION_APPLICATION_DETAILS_SETTINGS` with a `package:` URI, falls back to Settings). The app is rescanned on
  every resume, because permission changes in Settings send no broadcast.
- Fixed: the search box dropped characters when typing fast (its text went through an async StateFlow); the text now
  lives in synchronous Compose state in the ViewModel.
- Verified on moto g34 (Android 15), no new test files: WhatsApp detail in light and dark; Manage permissions opens
  the system App info page for WhatsApp, Back returns; `pm revoke` Adobe Scan CAMERA → after returning, Camera moves
  to "Asked for, but not allowed" and the badge drops to Low; `pm grant` → back to Review (restored, dumpsys
  granted=true); uninstalling an app while its detail is open shows "App not installed" and Back works; rotation
  keeps the screen; typing "adobescan" via adb arrives intact; no PrivacyPal errors in logcat. Night mode restored.

### 1.6 Home summary
- `ui/home/HomeViewModel` + `HomeScreen`: "Permission check" card with apps checked, high-risk count, up to 3
  riskiest apps (MEDIUM/HIGH, worst first; tap → app detail, Back → Home) and "See all apps" (→ Apps tab).
  Loading state (with title) during the first scan; the original empty state if nothing has been checked.
- `navigateToTopLevel` moved to `ui/navigation/TopLevelNavigation.kt` so Home can switch tabs.
- Existing tests adjusted only: Home screenshot uses the Empty state (baseline unchanged); `NavigationTest` waits
  for Home/Apps content, which now loads from the database asynchronously.
- Verified on moto g34 (Android 15): 104 apps checked / 17 high-risk, top 3 Google, Phone, GPay; tap opens detail,
  Back returns Home; See all apps opens the Apps tab; light, dark and landscape; logcat clean;
  connectedDebugAndroidTest 4/4. Night mode restored.

### Phase 1 end
- Accuracy: stored grants match the system (`dumpsys package`, the same data Settings > Privacy > Permission manager
  shows) for WhatsApp (16), Truecaller (12) and Subway Surfers (0). App info for WhatsApp lists the same groups.
- Manage permissions → system App info → Back works; `pm revoke`/`pm grant` round trip on Adobe Scan CAMERA is
  reflected on return (permission restored afterwards).
- Rotation, process death (filters kept), light/dark, logcat clean; night mode restored to the user's setting.
- From 1.4 on, verification was on the phone instead of new test files (user request).
- Merged to main, tagged `v0.1.0`.

## Phase 2 – Mic and Camera Watchdog (branch `phase-2-watchdog`)

Plan decisions: foreground service type `specialUse` (camera/microphone types need CAMERA/RECORD_AUDIO, which we
never request, and are barred from BOOT_COMPLETED; dataSync has a 6 h/24 h limit and is barred from BOOT_COMPLETED on
Android 15+; systemExempted needs a role we don't have). Testing: phone verification only, plus one Room migration
test (user decision).

### 2.1 Sensor monitors
- `domain/watchdog`: `Sensor`, `SensorEvent` (STARTED / STOPPED / CANCELLED per source), `SensorSession`
  (duration, `isShort` < 1 s, kept), `SensorSessionTracker` (pairs starts and stops per sensor; overlapping sources,
  e.g. two cameras, merge into one session; duplicate/unmatched signals ignored; CANCELLED discards a session).
- `data/watchdog`: `AudioManagerMicMonitor` (`registerAudioRecordingCallback`, empty ↔ non-empty, an already
  running recording counts from registration) and `CameraManagerCameraMonitor` (`registerAvailabilityCallback` per
  camera id; `TorchCallback` so the flashlight is not reported as camera use). Cold `Flow`s, no permissions.
  PrivacyPal never opens a camera, so there is no own use to filter. Hilt `WatchdogModule`.
- Debug-only `MonitorDebugReceiver` (src/debug, absent from the release manifest) logs monitor events via adb.
- Verified on moto g34 (Android 15): system Camera open ~6 s → one CAMERA session of 6445 ms; voice assistant
  (`VOICE_COMMAND`) → one MICROPHONE session of 8361 ms (system audio log shows the same recording). Found: while
  PrivacyPal is cached, Android's app freezer stops callbacks (a recording was missed until the process was
  unfrozen with `am unfreeze`), which is why 2.3 runs the monitors in a foreground service. Merged debug and release
  manifests contain no RECORD_AUDIO or CAMERA. Torch filtering not checked on device (no adb way to toggle the torch).

### 2.2 Likely-app detection
- `domain/watchdog/LikelyApp`: `LikelyAppRules.resolve` picks the app on screen when a sensor started from usage
  events (events up to 2 s after the start count). Confidence HIGH (came on screen within 2 s), MEDIUM (≤ 60 s),
  LOW (longer). Home screens, System UI and PrivacyPal are never the likely app (→ "nothing on screen", i.e. the
  sensor was used in the background). Always labelled "likely", never certain.
- `data/watchdog/ForegroundAppResolver`: `AppOpsUsageAccessChecker` (GET_USAGE_STATS app op) and
  `UsageStatsForegroundAppResolver` (`UsageStatsManager.queryEvents`, waits until 2.5 s after the start because
  usage events are written with a delay). Without usage access it returns "unavailable" and nothing is queried.
- Usage Access onboarding: Settings → "Mic and camera" → "Likely-app detection" opens a screen explaining the
  limit, what is read, what happens without it and that nothing leaves the phone, with a status card and a button
  that opens PrivacyPal's own toggle in system Settings. Status refreshes on resume.
- `PACKAGE_USAGE_STATS` declared (special access the user grants in Settings; not a runtime permission).
- Verified on moto g34: the button opens Settings > Usage access on PrivacyPal's toggle; status changes off → on
  after granting; screen checked in light and dark. Camera app → `com.motorola.camera3` HIGH; voice assistant →
  `com.google.android.googlequicksearchbox` HIGH. Found: the first try gave "nothing on screen" because the app's
  RESUMED event is logged after the mic starts; fixed with the settle delay. With access off → unavailable.
  Logcat clean. Usage access left on for the rest of Phase 2 testing (restored at phase end).
