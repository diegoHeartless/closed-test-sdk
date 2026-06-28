# Changelog

All notable changes to this project are documented in this file. SDK version follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html); public API is `io.closedtest.sdk`.

## [0.2.14] — 2026-06-01

### Fixed

- **Ingest handshake:** reuse a stored refresh token before `POST /v1/init` on cold start, foreground, and flush — avoids a new init session on every app launch when the refresh token is still valid (helps keep roster accurate; fewer redundant server sessions).

## [0.2.13] — 2026-06-01

### Added

- **`daily_ping`:** background ingest signal at most **once per calendar day** (device local), scheduled via WorkManager (~24h period, not exact time). Configure with `ClosedTestOptions.dailyPingEnabled` (default **true**) or manifest `io.closedtest.sdk.daily_ping_enabled`. Requires network; does not run when SDK is a no-op (e.g. debuggable + `collectInDebuggableBuilds=false`). See `spec.md` §4.

## [0.2.12] — 2026-06-01

### Added

- **Already-installed tracked invite:** `ClosedTest.handleDeepLink` accepts `referrer` / `install_referrer` / `df_referrer` on `closedtest://bind?…` (from DozenFlow `GET /open/app/{token}`). Value is cached and sent on the next `POST /v1/init` as `install_referrer`. Requires `intent-filter` on the host activity — see `docs/SDK_USAGE.md`.

## [0.2.11] — 2026-06-01

### Added

- **Tracked Play Install Referrer:** reads Play Install Referrer once per install (`com.android.installreferrer:installreferrer`) and sends optional **`install_referrer`** on `POST /v1/init` (e.g. `df_{token}` from organizer DozenFlow links). Failures do not block handshake. See `spec.md` §8.1, ProofFlow `docs/TRACKED_PLAY_INVITE.md`.

## [0.2.10] — 2026-06-01

### Added

- **Roster contact self-report:** optional one-time dialog after the first `session_start` (`cold_start`) when `ClosedTestOptions.rosterContactPromptEnabled` is true (default **false**). Submits Telegram via `POST /v1/tester-contact` with the ingest session token. Manifest meta-data: `io.closedtest.sdk.roster_contact_prompt_enabled`. See `docs/SDK_USAGE.md`, `ProofFlow/docs/UNIFIED_API_DRAFT.md` §5.1.

## [0.2.9] — 2026-06-01

### Added

- **Local daily test reminder:** optional notification at a fixed local time (default **15:00**) when the app has **not** been opened yet today. Configure via `ClosedTestOptions` (`dailyReminderEnabled`, `dailyReminderHourLocal`, `dailyReminderMinuteLocal`) or manifest meta-data (`io.closedtest.sdk.daily_reminder_*`). Requires `POST_NOTIFICATIONS` on Android 13+ (host app should request permission). Not a remote FCM push.

## [0.2.8] — 2026-05-31

### Fixed

- **`device_id` after reinstall:** first install after app data is cleared derives a **stable** UUID (v3) from `Settings.Secure.ANDROID_ID` instead of a new random UUID each time, so ingest **roster** is not inflated by reinstalls on the same physical device. Existing ids already stored in prefs are unchanged until cleared.

## [0.2.7] — 2026-05-14

### Fixed

- **Room `onOpen` PRAGMA:** `PRAGMA mmap_size = 0` is applied via **`query(SimpleSQLiteQuery(…)).close()`** instead of `execSQL`, fixing **`SQLiteException: Queries can be performed using … query or rawQuery methods only`** on some devices (e.g. Samsung SM-A525F / Android 12).

## [0.2.6] — 2026-05-13

### Changed

- **Room / SQLite:** internal queue DB uses **`JournalMode.TRUNCATE`** (no WAL `-wal`/`-shm`) and **`PRAGMA mmap_size = 0`** on open to reduce OEM SELinux **`ioctl`** denials on `closed_test_sdk.db` (e.g. Samsung `ioctlcmd=0xf522` in auditd) without changing the public API.

## [0.2.5] — 2026-05-11

### Changed

- **`ClosedTestOptions.proofFlowHintEnabled`** defaults to **true** (was false). Startup meta-data `io.closedtest.sdk.proofflow_hint_enabled` defaults to enabled when omitted; set **`false`** to opt out.

## [0.2.4] — 2026-05-11

### Added

- **ProofFlow hint after init:** optional dialog when `POST /v1/init` returns `proofflow_test_id` and `ClosedTestOptions.proofFlowHintEnabled` is true. Opens PF-TEST `proofflow://test/{id}`; limits via `proofFlowHintMaxShows`, `proofFlowHintCooldownMs`; meta-data `io.closedtest.sdk.proofflow_hint_enabled` for Startup auto-init. See `docs/SDK_USAGE.md`, `ProofFlow/docs/STATS_AND_DEEPLINKS_DRAFT.md` §3.1.1.

## [0.2.3] — 2026-05-11

### Added

- **Implicit Intent discovery for ProofFlow:** `ClosedTest.DISCOVERY_INTENT_ACTION` (`io.closedtest.sdk.DISCOVERY`), exported invisible `ClosedTestDiscoveryStubActivity` with matching `intent-filter`. Consumers use `PackageManager.queryIntentActivities`; Intent-only matches are spoofable — ContentProvider marker remains the strict check per package (`docs/SDK_USAGE.md`).

## [0.2.2] — 2026-05-09

### Added

- **Base ingest handshake:** `POST /v1/init` may omit `publishable_key`; SDK sends `package_name`, `build_type` (`debug`/`release`), `version_name`, `version_code` so the server can resolve an allowlisted build tuple (see AndroidServer `CLOSED_TEST_INGEST_BASE_IDENTITIES`).
- **Auto-init without key:** `ClosedTestInitializer` always calls `ClosedTest.initialize` when auto-init is enabled; empty `io.closedtest.sdk.publishable_key` uses Base mode.

## [0.2.1] — 2026-05-07

### Added

- **Discovery `ContentProvider`** (authority `<applicationId>.closedtest.discovery`) for ProofFlow: read-only `query` with columns `sdk_version` and `host_package`; access limited to known ProofFlow package UIDs. Optional `meta-data` `io.closedtest.sdk.discovery_enabled` to disable.
- **`ClosedTest.DISCOVERY_AUTHORITY_SUFFIX`** and **`ClosedTest.discoveryAuthority(applicationId)`** for integrators and host apps.

## [0.2.0] — 2026-04-22

### Added

- Upload **exponential backoff with jitter** on retryable failures (`IOException`, HTTP 408, 425, 429, 5xx); capped retries per batch.
- **`ClosedTestOptions`**: `maxQueuedEvents`, `eventsBatchSize` (1..100), `uploadBackoffInitialMs`, `uploadBackoffMaxMs`.
- **Memory pressure**: best-effort `flush` on `onTrimMemory` / `onLowMemory`, and a short delayed flush when the app goes to background to narrow the data-loss window before process death.
- **ProGuard / R8**: expanded `consumer-rules.pro` for OkHttp, Room, and serialization metadata.

### Changed

- **`BuildConfig.SDK_VERSION`** is driven from `gradle/libs.versions.toml` (`closedTestSdk`).

## [0.1.0] — 2026-04-22

### Added

- Initial Android library MVP: `ClosedTest.initialize`, lifecycle-driven sessions, heartbeat, queue + batch upload, deep link / `bindTester`, `flush`, optional debug no-op.
