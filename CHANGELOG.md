# Changelog

All notable changes to this project are documented in this file. SDK version follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html); public API is `io.closedtest.sdk`.

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
