# PrivacyPal plan

Each phase's detailed brief is added here when that phase starts.
Rules for every phase (Definition of Done, git protocol) live in `CLAUDE.md`; what was built and how it
was verified is logged in `docs/PROGRESS.md`.

## Phases

| Phase | Branch | Tag | Status |
|---|---|---|---|
| 0 – Foundation: project setup, light/dark theme, theme setting, navigation shell, test + quality toolchain, CI | `phase-0-foundation` | `v0.0.1` | Done |
| 1 – Permission Auditor: scan apps and granted permissions, persist, risk rules, apps list, app detail, home summary | `phase-1-permission-auditor` | `v0.1.0` | Done |
| 2 – Mic and Camera Watchdog: sensor monitors, likely-app detection + usage access onboarding, foreground service + storage, alerts, Activity history + retention | `phase-2-watchdog` | `v0.2.0` | Done |
| Later phases (from the product scope in `CLAUDE.md`): local-VPN tracker blocker, tracker SDK scanner, privacy score | – | – | Not started; briefs to come |

## Data model (Room, `privacypal.db`)

### v1 (Phase 1)
- `apps` – `AppEntity`: `packageName` (PK), `label`, `category` (`AppCategory` name), `isSystem`, `versionCode`,
  `firstInstallTime`, `lastUpdateTime`, `scannedAt` (epoch ms).
- `permission_grants` – `PermissionGrantEntity`: (`packageName`, `permission`) PK, `permissionGroup`
  (`PermissionGroup` name, indexed), `granted`. `packageName` → `apps.packageName`, ON DELETE CASCADE.
- Only tracked (dangerous/runtime) permissions are stored; icons are never stored. Exported schemas live in
  `app/schemas/`; any schema change needs a version bump and a migration test.

### v2 (Phase 2, `AutoMigration(1 → 2)`, tested by `MigrationTest`)
- `sensor_events` – `SensorEventEntity`: `id` (PK, auto), `sensor` (MICROPHONE/CAMERA), `startedAt` (indexed),
  `endedAt` (null while in use or if `interrupted`), `interrupted`, `screenOn`, `likelyResolved`, `likelyPackage`
  (indexed), `confidence` (HIGH/MEDIUM/LOW), `appInForeground` (null without usage access).
- `app_rules` – `AppRuleEntity`: `packageName` (PK), `muteAlerts`.
- Kept 7 / 30 / 90 days (default 30, DataStore `history_retention_days`); daily `HistoryCleanupWorker`.

## Mic and camera watchdog (Phase 2)

- Android doesn't tell normal apps which app uses the mic or camera. PrivacyPal listens to
  `AudioManager.AudioRecordingCallback` and `CameraManager.AvailabilityCallback` (flashlight ignored via
  `TorchCallback`) and names the LIKELY app: the app on screen when the use started, from usage events (needs the
  user to grant usage access). Never shown as certain.
- Likely-app confidence: on screen within 2 s of the start → HIGH, within 60 s → MEDIUM, longer → LOW. Home
  screens, System UI and PrivacyPal are ignored; if nothing else was on screen it was a background app.
- Runs in `WatchdogService`, foreground service type `specialUse` (no RECORD_AUDIO/CAMERA; camera/microphone types
  need those permissions and can't start from BOOT_COMPLETED; dataSync is time-limited). Restarted after boot,
  app update and process death.
- Alerts: none for a use already over in under 1 s or a muted app; HIGH-importance channel when the screen was off
  or no app was on screen; DEFAULT otherwise.

## Risk rules (Phase 1)

Only granted permissions count. An app's risk is the worst rating among its granted groups.

| Group | LOW (expected) | HIGH (mismatch) | Otherwise |
|---|---|---|---|
| Microphone | Social, Video, Audio | Game, News, Maps, Image, Productivity | MEDIUM |
| Camera | Social, Video, Image | Game, News, Audio, Maps | MEDIUM |
| Contacts | Social | Game, News, Productivity, Image, Audio, Video, Maps | MEDIUM |
| Location | Maps | Game, Audio | MEDIUM |
| Location (all the time) | – | – (Maps = MEDIUM) | HIGH |
| SMS, Call logs | – | – (Social = MEDIUM) | HIGH |
| Phone | Social | Game, News | MEDIUM |
| Body sensors | – | Game, News, Social | MEDIUM |
| Physical activity | – | Game | MEDIUM |
| Calendar | Productivity | Game | MEDIUM |
| Nearby devices, Media/files, Notifications | all | – | LOW |

Apps with no declared category (most apps) get the "Otherwise" level; when that is MEDIUM the reason says the app
doesn't declare what it's for.
