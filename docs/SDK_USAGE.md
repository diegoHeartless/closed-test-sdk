# Руководство по использованию `closed-test-sdk`

SDK отправляет события в ingest **Ground Space Team** (`https://api.groundspaceteam.com`). Кабинет и приложение организатора — **ProofFlow** (отдельно от вашего приложения).  
Публичный API: `io.closedtest.sdk`.

## Продуктовый контекст: anyapp и ProofFlow

- **anyapp** — это **ваше** приложение, куда вы встраиваете SDK. Тестеры ставят именно его. Это не ProofFlow.
- **ProofFlow** — приложение **организатора**: завести тест, указать пакет anyapp, (опционально) получить **`publishable_key`** для Advanced-политики, смотреть метрики. Тестерам ProofFlow **не обязателен**: телеметрия идёт из anyapp. Дополнительная детализация по тестеру возможна, если продукт использует привязку тестера (см. раздел «Привязка тестера»).
- **Base vs Advanced:** **Base** — без ключа, сервер сопоставляет ingest с allowlist по кортежу `package` + `buildType` + `versionName` + `versionCode` (см. `spec.md` / OpenAPI). **Advanced** — **`publishable_key`** выдаётся в **ProofFlow** (или веб-кабинете / API) и вставляется в манифест или `ClosedTest.initialize` для управляемой политики на сервере.

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
    <!-- Опционально: для Advanced ingest. Без этого блока работает Base (если сервер знает ваш кортеж сборки). -->
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
- **`publishable_key`** в манифесте опционален: при отсутствии или пустом значении handshake идёт в **Base**; иначе — **Advanced** с ключом (через манифест или `ClosedTest.initialize`).
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

## Опционально: подсказка открыть ProofFlow (план)

Если на устройстве установлено приложение **ProofFlow**, SDK может (после явного включения издателем) показать **внутри anyapp** ненавязчивый баннер с предложением открыть ProofFlow для режима тестера. Контракт проверки пакета, UX и deep link — **`ProofFlow/docs/STATS_AND_DEEPLINKS_DRAFT.md`** §3.1.1.

По умолчанию фича **выключена**. Рекомендуется не использовать системный overlay поверх чужих приложений.

## Маркер discovery для ProofFlow (ContentProvider)

Библиотека мержит экспортированный `ContentProvider`, чтобы приложение **ProofFlow** могло убедиться, что в указанном пакете установлена сборка **с этим SDK**, без сканирования всех приложений на устройстве.

| Поле | Значение |
|------|----------|
| **Authority** | `<ваш applicationId>` + `ClosedTest.DISCOVERY_AUTHORITY_SUFFIX` (= суффикс `.closedtest.discovery`). Пример: `com.example.game.closedtest.discovery`. В коде: `ClosedTest.discoveryAuthority(BuildConfig.APPLICATION_ID)` или ту же строку из Gradle `applicationId`. |
| **Проверка из ProofFlow** | `PackageManager.resolveContentProvider("$packageName.closedtest.discovery", 0)` или запрос `ContentResolver.query` к `content://$authority/` (на Android 11+ может понадобиться элемент `<queries>` на известные пакеты anyapp — см. **`ProofFlow/docs/TEST_DISCOVERY.md`**). |
| **Курсор `query`** | Колонки: `sdk_version` (SemVer SDK из сборки), `host_package` (`applicationId` anyapp). Для вызывающих процессов **не** из whitelist пакетов ProofFlow возвращается **пустой** курсор. |
| **Отключить маркер** | В `<application>`: `<meta-data android:name="io.closedtest.sdk.discovery_enabled" android:value="false" />` — ответы останутся пустыми. |

Секреты, PII и `publishable_key` в этот провайдер **не** попадают.

### Неявный Intent (дополнительно)

Для клиентов вроде **ProofFlow**, которым нужен список кандидатов без заранее известных пакетов, SDK мержит невидимую activity с фильтром:

| Поле | Значение |
|------|----------|
| **Action** | `ClosedTest.DISCOVERY_INTENT_ACTION` (= `io.closedtest.sdk.DISCOVERY`). |
| **Проверка** | `PackageManager.queryIntentActivities(Intent(action).addCategory(Intent.CATEGORY_DEFAULT), …)` возвращает пакеты, где установлено приложение с этим SDK. |
| **Подделка** | Любое приложение может повесить такой же фильтр без SDK — для доверия используйте по-прежнему проверку **ContentProvider** по authority для конкретного пакета. |

На Android 11+ у клиента обычно нужен `<queries><intent><action android:name="io.closedtest.sdk.DISCOVERY"/></intent></queries>`.

## Что SDK делает автоматически

- lifecycle-события: `session_start`, `session_end`, `app_foreground`, `app_background`
- `heartbeat` в foreground
- очередь в Room и batched upload
- refresh токенов при `401`

