# Руководство по использованию `closed-test-sdk`

> **English (canonical for GitHub / Maven):** see the repository **[README.md](../README.md)**.  
> **Web guide (aligned with README):** [dozenflow.com/docs/sdk](https://dozenflow.com/docs/sdk).

SDK отправляет события в ingest **Ground Space Team** (`https://api.groundspaceteam.com`). Кабинет и приложение организатора — **Dozenflow** (бренд; пакет `com.ground.proofflow`, в коде иногда ProofFlow).  
Публичный API: `io.closedtest.sdk`.

## Продуктовый контекст: anyapp и Dozenflow

- **anyapp** — это **ваше** приложение, куда вы встраиваете SDK. Тестеры ставят именно его. Это не Dozenflow.
- **Dozenflow** — приложение и сайт **организатора**: завести тест, указать пакет anyapp, (опционально) получить **`publishable_key`** для Advanced-политики, смотреть метрики. Тестерам Dozenflow **не обязателен**: телеметрия идёт из anyapp. Дополнительная детализация по тестеру возможна, если продукт использует привязку тестера (см. раздел «Привязка тестера»).
- **Base vs Advanced:** **Base** — без ключа, сервер сопоставляет ingest с allowlist по кортежу `package` + `buildType` + `versionName` + `versionCode` (см. `spec.md` / OpenAPI). **Advanced** — **`publishable_key`** выдаётся в **Dozenflow** (или [dozenflow.com/dashboard](https://dozenflow.com/dashboard)) и вставляется в манифест или `ClosedTest.initialize` для управляемой политики на сервере.

## Подключение

```kotlin
dependencies {
    implementation("com.groundspaceteam:closed-test-sdk:<version>")
}
```

Актуальная версия — в [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) (`closedTestSdk`, сейчас **0.2.11**).

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
- `dailyReminderEnabled` — локальное напоминание, если приложение **ещё не открывали сегодня** (по локальному времени устройства). По умолчанию **true**.
- `dailyReminderHourLocal` / `dailyReminderMinuteLocal` — время напоминания (по умолчанию **15:00**).
- `dailyPingEnabled` — фоновый **`daily_ping`** на ingest не чаще **раза в календарные сутки** (локальное время; момент отправки приблизительный, WorkManager). По умолчанию **true**.

## Фоновый daily_ping

Отдельно от локального уведомления: SDK ставит периодическую фоновую задачу (~24 ч), которая при наличии сети отправляет событие `daily_ping` (не чаще одного раза за локальный день). Точное время не гарантируется — важно, что сигнал приходит **в течение суток**.

```kotlin
ClosedTestOptions(dailyPingEnabled = false)
```

```xml
<meta-data android:name="io.closedtest.sdk.daily_ping_enabled" android:value="false" />
```

## Локальное напоминание (не FCM)

SDK может показать **локальное** уведомление в заданное время, если пользователь **ещё не выводил приложение на передний план** в текущий календарный день. Это не push из Dozenflow и не требует Firebase в anyapp.

```kotlin
ClosedTest.initialize(
    context,
    publishableKey,
    ClosedTestOptions(
        dailyReminderEnabled = true,
        dailyReminderHourLocal = 15,
        dailyReminderMinuteLocal = 0,
    ),
)
```

Отключить:

```kotlin
ClosedTestOptions(dailyReminderEnabled = false)
```

Через манифест (авто-init):

```xml
<meta-data android:name="io.closedtest.sdk.daily_reminder_enabled" android:value="false" />
<meta-data android:name="io.closedtest.sdk.daily_reminder_hour" android:value="18" />
<meta-data android:name="io.closedtest.sdk.daily_reminder_minute" android:value="30" />
```

**Android 13+:** добавьте `POST_NOTIFICATIONS` в манифест приложения и запросите разрешение у пользователя — без него уведомление не покажется. Тап по уведомлению открывает launcher activity anyapp.

## Ручные события

```kotlin
ClosedTest.trackScreen("Home")
ClosedTest.trackInteraction("tap")
ClosedTest.trackEvent("onboarding_done", mapOf("step" to "2"))
ClosedTest.flush()
```

### Автотрекинг экранов (Navigation Compose)

Опциональный артефакт — не тянет Compose в основной SDK:

```kotlin
dependencies {
    implementation("com.groundspaceteam:closed-test-sdk:<version>")
    implementation("com.groundspaceteam:closed-test-sdk-navigation-compose:<version>")
}
```

```kotlin
import io.closedtest.sdk.navigation.ClosedTestScreenTracking

@Composable
fun AppNav(navController: NavHostController) {
    navController.ClosedTestScreenTracking()
    NavHost(navController, startDestination = "home") {
        composable("home") { HomeScreen() }
        composable("settings") { SettingsScreen() }
    }
}
```

По умолчанию в `screen_view` уходит **route** из графа (например `profile/{userId}`), без query и без подстановки аргументов в путь — так снижается риск PII в `screen_name` (`spec.md` §8). Для кастомных имён:

```kotlin
navController.ClosedTestScreenTracking { destination ->
    when (destination.route) {
        "profile/{userId}" -> "profile"
        else -> ClosedTestNavScreenNames.fromRoute(destination.route)
    }
}
```

Вне Compose: `navController.installClosedTestScreenTracking()` → `ClosedTestScreenTrackingHandle.close()` при уничтожении Activity.

**Не покрывает:** одиночные Activity без Navigation, legacy Fragments (нужен отдельный helper на `FragmentManager` — см. [`docs/CROSS_PLATFORM_SCREEN_GRAPH.md`](CROSS_PLATFORM_SCREEN_GRAPH.md)).

**Другие стеки** (Flutter, React Native, Unity): отдельные bridge-пакеты, тот же ingest-контракт `screen_view` — см. [`docs/CROSS_PLATFORM_SCREEN_GRAPH.md`](CROSS_PLATFORM_SCREEN_GRAPH.md).

## Привязка тестера

```kotlin
ClosedTest.handleDeepLink(intent?.data)
// или
ClosedTest.bindTester(testerId = "...", testSessionId = "...")
```

## Опционально: подсказка открыть ProofFlow

Если сервер возвращает **`proofflow_test_id`** в ответе **`POST /v1/init`** (связка теста ProofFlow с ingest) и подсказка **не отключена** издателем, SDK после успешного handshake может показать **диалог** на текущей Activity с кнопкой открыть ProofFlow по ссылке **`proofflow://test/{proofflow_test_id}`** (PF-TEST).

- По умолчанию фича **включена** (`ClosedTestOptions.proofFlowHintEnabled = true`).
- Выключение программно: `ClosedTest.initialize(context, publishableKey, ClosedTestOptions(proofFlowHintEnabled = false))`.
- При автозапуске через AndroidX Startup: чтобы отключить, в `<application>` добавить  
  `<meta-data android:name="io.closedtest.sdk.proofflow_hint_enabled" android:value="false" />`.
- Установка ProofFlow проверяется по пакетам из `ClosedTestOptions.proofFlowPackageNames` (по умолчанию `com.ground.proofflow` и `.debug`). При необходимости передайте свой список.
- Лимиты: не более **`proofFlowHintMaxShows`** показов за установку (по умолчанию 3), интервал **`proofFlowHintCooldownMs`** между показами после «Later» (по умолчанию 7 дней). Кнопка «Don't ask again» отключает подсказки.

Документ продукта: **`ProofFlow/docs/STATS_AND_DEEPLINKS_DRAFT.md`** §3.1.1.

## Roster contact (Telegram self-report)

Опциональный диалог **один раз** после первого `session_start` (`cold_start`): тестер может передать Telegram username организатору (`POST /v1/tester-contact`, тот же session token, что и для `/v1/events`).

- По умолчанию **выключено** (`ClosedTestOptions.rosterContactPromptEnabled = false`).
- Включение: `ClosedTest.initialize(context, publishableKey, ClosedTestOptions(rosterContactPromptEnabled = true))`.
- Авто-init: `<meta-data android:name="io.closedtest.sdk.roster_contact_prompt_enabled" android:value="true" />`.
- Username не попадает в `track_event.props` — только в dedicated endpoint. Организатор видит контакт в ProofFlow (roster / stats).

Документ продукта: **`ProofFlow/docs/UNIFIED_API_DRAFT.md`** §5.1.

## Screenshot feedback (Telegram → organizer, SDK ≥ 0.2.16)

Если организатор привязал Telegram в Dozenflow, сервер возвращает **`organizer_telegram`** в ответе **`POST /v1/init`**. SDK может после скриншота показать диалог «Отправить организатору в Telegram».

- По умолчанию **включено** (`ClosedTestOptions.screenshotFeedbackEnabled = true`).
- Выключение: `ClosedTestOptions(screenshotFeedbackEnabled = false)` или manifest  
  `<meta-data android:name="io.closedtest.sdk.screenshot_feedback_enabled" android:value="false" />`.
- **По умолчанию (SDK ≥ 0.2.17, reduced):** SDK **не** добавляет в anyapp разрешения на фото — не нужна декларация Google Play «Photo and video». После скриншота: диалог и открытие `https://t.me/{organizer_telegram}` (без прикрепления файла).
- **Полный режим (опционально):** объявите в **манифесте anyapp** `READ_MEDIA_IMAGES` (API 33+) и/или `READ_EXTERNAL_STORAGE` (maxSdk 32), запросите runtime-разрешение при необходимости и заполните декларацию в Play Console. Тогда кнопка **Share** может приложить последний скриншот к chooser.
- **Android 14+ (API 34):** детект через `ScreenCaptureCallback` (без runtime-разрешений).
- **API 24–33:** детект через `MediaStore` observer (менее надёжно, чем на API 34+).
- Кнопка **Share:** с разрешениями — chooser с картинкой; без — deep link в Telegram.
- Cooldown между подсказками: **`screenshotFeedbackCooldownMs`** (по умолчанию 2 мин). «Don't ask again» отключает фичу.
- События ingest: `screenshot_feedback_prompt_shown`, `screenshot_feedback_shared` (`props.with_image`: `true` / `false` / `failed`).

Организатор должен привязать Telegram в Dozenflow (карточка на экране тестов). Без `organizer_telegram` в init фича не активируется.

## Tracked Play Install Referrer (SDK ≥ 0.2.11)

Если тестер установил anyapp по **отслеживаемой** ссылке организатора (DozenFlow → Play с `referrer=df_{token}`), SDK один раз читает **Play Install Referrer** и передаёт строку в **`install_referrer`** на `POST /v1/init`. Ошибка чтения не блокирует handshake. Сервер сопоставляет установку с кликом по invite.

- Контракт: `spec.md` §8.1, `openapi/ingest.yaml` (`InitRequest.install_referrer`).
- Продукт: **`ProofFlow/docs/TRACKED_PLAY_INVITE.md`** (фаза 3).
- Настройка в anyapp **не требуется** — достаточно обновить SDK до **0.2.11+**.

### Уже установленное приложение (SDK ≥ 0.2.12)

Play Install Referrer **не обновляется** при повторном открытии Play, если anyapp уже стоял на устройстве. Для этого сценария DozenFlow отдаёт ссылку **`GET /open/app/{token}`** → `intent://…closedtest://bind?referrer=df_{token}`.

В anyapp:

1. SDK **≥ 0.2.12**
2. `intent-filter` на launcher activity (как в `examples/sample`):

```xml
<data android:scheme="closedtest" android:host="bind" />
```

3. В `onCreate` / `onNewIntent`: `ClosedTest.handleDeepLink(intent?.data)`

SDK сохранит `referrer` / `install_referrer` из query и отправит на следующем `init` — сервер матчит клик и привязывает `device_id` к roster.

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
- `daily_ping` в фоне (не чаще раза в локальные сутки, если включено)
- очередь в Room и batched upload
- refresh токенов при `401`

