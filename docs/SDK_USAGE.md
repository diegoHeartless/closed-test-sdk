# Руководство по использованию `closed-test-sdk`

SDK отправляет события в ingest **Ground Space Team** (`https://api.groundspaceteam.com`). Кабинет и приложение организатора — **ProofFlow** (отдельно от вашего приложения).  
Публичный API: `io.closedtest.sdk`.

## Продуктовый контекст: anyapp и ProofFlow

- **anyapp** — это **ваше** приложение, куда вы встраиваете SDK. Тестеры ставят именно его. Это не ProofFlow.
- **ProofFlow** — приложение **организатора**: завести тест, указать пакет anyapp, получить **`publishable_key`**, смотреть метрики. Тестерам ProofFlow **не обязателен**: телеметрия идёт из anyapp по ключу. Дополнительная детализация по тестеру возможна, если продукт использует привязку тестера (см. раздел «Привязка тестера»).
- **`publishable_key`** — выдаётся в **ProofFlow** (или связанном с ним веб-кабинете / API) после регистрации приложения; вставляется в манифест или в `ClosedTest.initialize`.

## Подключение

```kotlin
dependencies {
    implementation("com.groundspaceteam:closed-test-sdk:<version>")
}
```

## Инициализация (авто-init, по умолчанию)

SDK автоматически инициализируется через AndroidX Startup.

Добавьте в `AndroidManifest.xml` приложения:

```xml
<application ...>
    <meta-data
        android:name="io.closedtest.sdk.publishable_key"
        android:value="pk_live_..." />
</application>
```

Опционально можно отключить авто-init:

```xml
<meta-data
    android:name="io.closedtest.sdk.auto_init_enabled"
    android:value="false" />
```

Важно:
- URL backend **не нужно** передавать: он зашит внутри SDK (`https://api.groundspaceteam.com`).
- `publishableKey` передаётся через `AndroidManifest` (или вручную в `ClosedTest.initialize`).
- Ручной `initialize(...)` остаётся доступным, повторный вызов безопасно игнорируется.

### Ключ без хардкода в git (рекомендуется)

Чтобы не копировать ключ с телефона в репозиторий и не править закоммиченный манифест, держите значение **вне git** и подставляйте при сборке.

**1. Локально — свойство Gradle**, например в **`%USERPROFILE%\.gradle\gradle.properties`** (не в репозитории) или в **`gradle.properties` корня проекта**, если файл **в `.gitignore`**:

```properties
closedTest.publishableKey=pk_live_...
```

`project.findProperty("closedTest.publishableKey")` подхватит такие файлы. Пары ключ/значение из **`local.properties`** Gradle **сам не подмешивает** в `findProperty`; если хотите хранить ключ только в `local.properties`, прочитайте его в `build.gradle.kts` через `java.util.Properties()` и передайте в `manifestPlaceholders` так же.

**2. В `app/build.gradle.kts`** — проброс в манифест (пример; имена можно поменять):

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

В `AndroidManifest.xml` приложения:

```xml
<meta-data
    android:name="io.closedtest.sdk.publishable_key"
    android:value="${closedTestPublishableKey}" />
```

**3. CI** — задайте секрет `CLOSED_TEST_PUBLISHABLE_KEY` (GitHub Actions, TeamCity и т.д.) вместо коммита ключа.

Рабочий пример с `manifestPlaceholders` есть в модуле **`examples/sample`** репозитория SDK.

## ClosedTestOptions

Доступные настройки:
- `heartbeatIntervalMs`
- `backgroundSessionEndDelayMs`
- `collectInDebuggableBuilds`
- `okHttpClient`
- `maxQueuedEvents`
- `eventsBatchSize`
- `uploadBackoffInitialMs`
- `uploadBackoffMaxMs`

## Ручные события

```kotlin
ClosedTest.trackScreen("Home")
ClosedTest.trackInteraction("tap")
ClosedTest.trackEvent("onboarding_done", mapOf("step" to "2"))
ClosedTest.flush()
```

## Привязка тестера

```kotlin
ClosedTest.handleDeepLink(intent?.data)
// или
ClosedTest.bindTester(testerId = "...", testSessionId = "...")
```

## Что SDK делает автоматически

- lifecycle-события: `session_start`, `session_end`, `app_foreground`, `app_background`
- `heartbeat` в foreground
- очередь в Room и batched upload
- refresh токенов при `401`

