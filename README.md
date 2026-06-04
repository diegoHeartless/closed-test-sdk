# Closed Test SDK for Android

[![Maven Central](https://img.shields.io/maven-central/v/com.groundspaceteam/closed-test-sdk)](https://central.sonatype.com/artifact/com.groundspaceteam/closed-test-sdk)

Embed this library in **anyapp** — the APK you distribute to closed or internal testers — to report **session and participation signals** to the Dozenflow ingest API. Organizers monitor tests in the **[Dozenflow](https://dozenflow.com)** mobile app and web dashboard; testers usually install **only your app**, not Dozenflow.

| | |
|---|---|
| **Maven** | `com.groundspaceteam:closed-test-sdk` |
| **Package** | `io.closedtest.sdk` |
| **Ingest** | `https://api.groundspaceteam.com` (built into the SDK) |
| **Integration guide (web)** | [dozenflow.com/docs/sdk](https://dozenflow.com/docs/sdk) |
| **Current version** | See [`gradle/libs.versions.toml`](gradle/libs.versions.toml) (`closedTestSdk`) |

---

## What it does

- Tracks **sessions**, **foreground/background**, and **heartbeats** while the app is in use.
- Queues events locally (**Room**) and uploads them in **batches** with retry/backoff.
- Performs **`POST /v1/init`** handshake (Base or Advanced) and refreshes tokens on **401**.
- Optionally links a tester to a Dozenflow test (deep link / bind) and can show a **“open Dozenflow”** hint when the server returns a test id.
- Exposes a **discovery marker** so the Dozenflow organizer app can verify your package has this SDK installed.

The SDK is a **developer tool** for legitimate closed-test monitoring. It does **not** find testers, guarantee Play production approval, or bypass platform policies.

---

## Terminology

| Term | Meaning |
|------|---------|
| **anyapp** | Your app under test — where you add this SDK. Testers install this APK. |
| **Dozenflow** | Organizer product ([mobile app](https://dozenflow.com) + dashboard). Create tests, issue keys, view metrics. |
| **Base mode** | No `publishable_key`. The server matches ingest using an allowlisted build tuple (`package`, `buildType`, `versionName`, `versionCode`). |
| **Advanced mode** | Uses `pk_live_…` from Dozenflow or the [web dashboard](https://dozenflow.com/dashboard). Server applies key-based policy. |

---

## Quick start

1. **Create a test** in Dozenflow and copy a **`publishable_key`** (or configure Base allowlist for your build tuple on the server).
2. **Add the dependency** and optional manifest meta-data (below).
3. **Ship your test build** to testers. Events appear in Dozenflow after a successful handshake.

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.groundspaceteam:closed-test-sdk:0.2.8")
}
```

```xml
<!-- AndroidManifest.xml — inside <application> -->
<!-- Omit for Base mode if your build tuple is allowlisted on the server. -->
<meta-data
    android:name="io.closedtest.sdk.publishable_key"
    android:value="pk_live_YOUR_KEY_HERE" />
```

The SDK **auto-starts** via [AndroidX App Startup](https://developer.android.com/topic/libraries/app-startup). You do **not** configure the ingest URL in app code.

---

## Initialization

### Auto-init (recommended)

| Manifest meta-data | Effect |
|--------------------|--------|
| `io.closedtest.sdk.publishable_key` | Non-empty → **Advanced**. Missing/empty → **Base** (if allowlisted). |
| `io.closedtest.sdk.auto_init_enabled` | Set `false` to disable Startup and call `ClosedTest.initialize` yourself. |
| `io.closedtest.sdk.proofflow_hint_enabled` | Set `false` to disable the optional Dozenflow app hint dialog. |
| `io.closedtest.sdk.discovery_enabled` | Set `false` to disable the discovery ContentProvider. |

Duplicate `ClosedTest.initialize(...)` calls with the same key are ignored.

### Manual init

```kotlin
ClosedTest.initialize(
    context = applicationContext,
    publishableKey = "pk_live_YOUR_KEY_HERE", // optional for Base
    options = ClosedTestOptions(/* see below */),
)
```

---

## Keep the publishable key out of git

Do not commit `pk_live_…` in `AndroidManifest.xml`. Inject at build time.

**Gradle property** (e.g. `~/.gradle/gradle.properties` or a **gitignored** project `gradle.properties`):

```properties
closedTest.publishableKey=pk_live_YOUR_KEY_HERE
```

**`app/build.gradle.kts`:**

```kotlin
android {
    defaultConfig {
        val pk = (project.findProperty("closedTest.publishableKey") as String?)
            ?: System.getenv("CLOSED_TEST_PUBLISHABLE_KEY")
            ?: ""
        manifestPlaceholders["closedTestPublishableKey"] = pk
    }
}
```

```xml
<meta-data
    android:name="io.closedtest.sdk.publishable_key"
    android:value="${closedTestPublishableKey}" />
```

**CI:** set secret `CLOSED_TEST_PUBLISHABLE_KEY` — never commit the key.

A working sample with `manifestPlaceholders` lives in [`examples/sample`](examples/sample).

---

## ClosedTestOptions

| Option | Purpose |
|--------|---------|
| `heartbeatIntervalMs` | Foreground heartbeat interval |
| `backgroundSessionEndDelayMs` | Delay before `session_end` after background |
| `collectInDebuggableBuilds` | Collect telemetry in debug builds (default off) |
| `okHttpClient` | Custom OkHttp client |
| `maxQueuedEvents` | Room queue cap |
| `eventsBatchSize` | Batch upload size |
| `uploadBackoffInitialMs` / `uploadBackoffMaxMs` | Retry backoff |
| `proofFlowHintEnabled` | Show “open Dozenflow” dialog after init (default `true`) |
| `proofFlowHintMaxShows` | Max dialog shows per install (default `3`) |
| `proofFlowHintCooldownMs` | Cooldown after “Later” (default 7 days) |
| `proofFlowPackageNames` | Packages checked for the installed Dozenflow app |

---

## Manual events

```kotlin
ClosedTest.trackScreen("Home")
ClosedTest.trackInteraction("tap")
ClosedTest.trackEvent("onboarding_done", mapOf("step" to "2"))
ClosedTest.flush() // force upload
```

Event names and `props` must follow the **PII policy** in [`spec.md`](spec.md) §8 (no email, phone, precise location, etc. in `props`).

---

## Tester binding (optional)

When your flow provides tester/session ids (e.g. from a Dozenflow invite):

```kotlin
ClosedTest.handleDeepLink(intent?.data)

// or
ClosedTest.bindTester(
    testerId = "…",
    testSessionId = "…",
)
```

---

## Organizer app hint

When `POST /v1/init` returns `proofflow_test_id` and hints are enabled, the SDK may show a dialog to open Dozenflow:

`proofflow://test/{proofflow_test_id}`

Testers can dismiss, snooze, or opt out. Your app is not blocked if Dozenflow is not installed.

Disable via manifest:

```xml
<meta-data
    android:name="io.closedtest.sdk.proofflow_hint_enabled"
    android:value="false" />
```

---

## Discovery marker

The SDK merges an exported **ContentProvider** so Dozenflow can verify a specific package has this SDK.

| Item | Value |
|------|--------|
| **Authority** | `{applicationId}.closedtest.discovery` — `ClosedTest.discoveryAuthority(applicationId)` |
| **Columns** | `sdk_version`, `host_package` |
| **Non-whitelisted callers** | Empty cursor (no secrets) |

Optional implicit intent: action `io.closedtest.sdk.DISCOVERY` (`ClosedTest.DISCOVERY_INTENT_ACTION`). On Android 11+, clients need `<queries>` for the intent. For a **known** package, prefer ContentProvider verification.

---

## Automatic behavior

- Lifecycle: `session_start`, `session_end`, `app_foreground`, `app_background`
- Foreground **heartbeat**
- **Room** queue + batched upload
- Token **refresh** on HTTP **401**

---

## Organizer workflow

1. Create a test in the **Dozenflow** app (package name, quotas, policy).
2. Copy **`publishable_key`** from Dozenflow or [dozenflow.com/dashboard](https://dozenflow.com/dashboard) (when signed in).
3. Add this SDK to **anyapp** with that key (or use Base if allowlisted).
4. Distribute your test build; testers use **anyapp** only.
5. Monitor progress in Dozenflow or the web dashboard.

---

## Troubleshooting

| Symptom | What to check |
|---------|----------------|
| No events on server | Network; `POST /v1/init` 401/403; wrong package in policy; Base tuple not allowlisted; key typo |
| 403 on init (Advanced) | Key disabled/rotated; wrong `package_name` in server policy |
| No organizer-app dialog | `proofflow_test_id` not returned; hint disabled; max shows reached |
| Organizer cannot detect SDK | Wrong package; `discovery_enabled=false`; missing `<queries>` on Android 11+ |

---

## Sample app

Module **[`examples/sample`](examples/sample)** demonstrates `trackScreen`, `trackEvent`, `trackInteraction`, deep link / `bindTester`, and `flush`.

---

## Reference

| Document | Description |
|----------|-------------|
| [**dozenflow.com/docs/sdk**](https://dozenflow.com/docs/sdk) | Primary integration guide (aligned with this README) |
| [`spec.md`](spec.md) | Ingest API and event contract (Phase 0) |
| [`openapi/ingest.yaml`](openapi/ingest.yaml) | Machine-readable OpenAPI |
| [`docs/SDK_USAGE.md`](docs/SDK_USAGE.md) | Extended guide (Russian) |
| [`ROADMAP.md`](ROADMAP.md) | Roadmap and phases |
| [`CHANGELOG.md`](CHANGELOG.md) | Release notes |
| [`docs/PUBLISHING_MAVEN_CENTRAL.md`](docs/PUBLISHING_MAVEN_CENTRAL.md) | Publishing the library |

---

## Requirements

- **minSdk 24**
- Kotlin / Android Gradle Plugin versions: see root [`gradle/libs.versions.toml`](gradle/libs.versions.toml)

---

## Privacy and Play Data safety

- The SDK sends **technical session and usage signals**, not end-user identity fields you control in `track_event.props`.
- Declare closed-test monitoring in your **anyapp** Play Console **Data safety** form; organizers use Dozenflow separately.
- Policy details: [`spec.md`](spec.md) §8 and [Dozenflow privacy policy](https://dozenflow.com/privacy).
