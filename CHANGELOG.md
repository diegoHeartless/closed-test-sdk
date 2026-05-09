# Changelog

All notable changes to this project are documented in this file. SDK version follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html); public API is `io.closedtest.sdk`.

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
