# PrivacyPal plan

Each phase's detailed brief is added here when that phase starts.
Rules for every phase (Definition of Done, git protocol) live in `CLAUDE.md`; what was built and how it
was verified is logged in `docs/PROGRESS.md`.

## Phases

| Phase | Branch | Tag | Status |
|---|---|---|---|
| 0 – Foundation: project setup, light/dark theme, theme setting, navigation shell, test + quality toolchain, CI | `phase-0-foundation` | `v0.0.1` | Done |
| 1 – Permission Auditor: scan apps and granted permissions, persist, risk rules, apps list, app detail, home summary | `phase-1-permission-auditor` | `v0.1.0` | In progress |
| Later phases (from the product scope in `CLAUDE.md`): mic/camera watchdog, local-VPN tracker blocker, tracker SDK scanner, privacy score | – | – | Not started; briefs to come |

## Data model (Room, `privacypal.db`)

### v1 (Phase 1)
- `apps` – `AppEntity`: `packageName` (PK), `label`, `category` (`AppCategory` name), `isSystem`, `versionCode`,
  `firstInstallTime`, `lastUpdateTime`, `scannedAt` (epoch ms).
- `permission_grants` – `PermissionGrantEntity`: (`packageName`, `permission`) PK, `permissionGroup`
  (`PermissionGroup` name, indexed), `granted`. `packageName` → `apps.packageName`, ON DELETE CASCADE.
- Only tracked (dangerous/runtime) permissions are stored; icons are never stored. Exported schemas live in
  `app/schemas/`; any schema change needs a version bump and a migration test.
