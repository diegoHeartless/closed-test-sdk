# Публикация `closed-test-sdk` в Maven Central

Инструкция ориентирована на **Sonatype Central Portal** ([central.sonatype.com](https://central.sonatype.com/)) — основной путь для новых проектов с 2024 года. Старый **OSSRH / Nexus** для новых регистраций по сути вытеснён; если у вас уже есть только OSSRH, см. [миграцию](https://central.sonatype.org/news/20250326_ossrh_sunset/) в документации Sonatype.

---

## 1. Что нужно заранее

| Требование | Зачем |
|------------|--------|
| **Namespace (groupId)** | Должен быть **подтверждён** в Central (например `io.github.yourname` или `com.yourcompany` при владении доменом). |
| **GPG-ключ** | Подпись **всех** артефактов (`.aar`, `.pom`, `-sources.jar`, `-javadoc.jar`, и их `.asc`). |
| **Учётная запись Central** | Логин на [central.sonatype.com](https://central.sonatype.com/), генерация **User Token** (не пароль от сайта в CI). |
| **Лицензия и SCM в репозитории** | В POM обязательны поля, согласованные с реальным `LICENSE` и URL репозитория. |

Gradle-настройки публикации и POM лежат в **`closed-test-sdk/build.gradle.kts`** (подпись, `withJavadocJar`, блок `pom`).

В модуле `closed-test-sdk` заданы координаты публикации:

- **groupId:** `com.groundspaceteam` (совпадает с подтверждённым namespace в Central)
- **artifactId:** `closed-test-sdk`
- **version:** из `gradle/libs.versions.toml` → `closedTestSdk`

**Важно:** для Maven Central важен **Maven `groupId`** (координата артефакта). Пакеты Kotlin/Java внутри AAR остаются **`io.closedtest.sdk`** — это другое; в приложении в Gradle одна строка зависимости, в коде импорты как раньше: `import io.closedtest.sdk.ClosedTest`.

### У вас namespace уже verified — дальше по порядку

1. **SCM в POM** — в `closed-test-sdk/build.gradle.kts` по умолчанию указан репозиторий **[diegoHeartless/closed-test-sdk](https://github.com/diegoHeartless/closed-test-sdk)** (`scm:git:git://…` и `ssh://…`). При смене origin переопределите `POM_SCM_*` в `~/.gradle/gradle.properties`.
2. **`POM_URL`** — по умолчанию страница репозитория на GitHub; при необходимости переопределите через свойство `POM_URL` (например сайт **groundspaceteam.com**).
3. **`POM_DEVELOPER_*`** — по умолчанию в Gradle стоят `diegoHeartless` / `Ground Space Team`; при необходимости переопределите в `gradle.properties`.
4. **GPG** — ключ, `signing.key` / `signing.password` в `~/.gradle/gradle.properties`, проверка: `./gradlew :closed-test-sdk:publishToMavenLocal` и наличие **`.asc`** у каждого артефакта.
5. **User Token** на [central.sonatype.com](https://central.sonatype.com/) — для загрузки (JReleaser / ручной upload / API).
6. **Первая выгрузка** — JReleaser или UI **Deployments**; дождаться валидации → **Publish** в Central.
7. После появления на Maven Central — в приложениях: `implementation("com.groundspaceteam:closed-test-sdk:<version>")`.

---

## 2. Регистрация и подтверждение namespace

1. Зайдите на [https://central.sonatype.com/](https://central.sonatype.com/) и создайте учётную запись (или войдите).
2. В разделе **Namespaces** добавьте желаемый префикс, например:
   - **`io.github.<username>`** — обычно проще всего: привязка к GitHub и подтверждение по инструкции портала;
   - **`com.example.app`** — если есть домен `example.com`, часто просят DNS **TXT** на хосте вроде `_sonatype` или подтверждение владения доменом (см. актуальную подсказку в UI Central).
3. Дождитесь статуса **verified** для namespace.

Официальные шаги: [Register a namespace](https://central.sonatype.org/register/central-portal/#namespace) и раздел **Publishing** на [central.sonatype.org](https://central.sonatype.org/publish/publish-portal-gradle/).

---

## 3. GPG: ключ и keyserver

Нужен **один** долгоживущий ключ для подписи артефактов перед загрузкой в Maven Central. Пароль от ключа (passphrase) храните в менеджере паролей; **приватный ключ** — только в `~/.gradle/gradle.properties` локально или в **секретах CI**, не в git.

### 3.1. Установить GnuPG

| ОС | Как |
|----|-----|
| **Windows** | [Gpg4win](https://www.gpg4win.org/) (в составе — `gpg` в `cmd` / PowerShell после установки) или `gpg` из Git for Windows. |
| **macOS** | `brew install gnupg` |
| **Linux** | `sudo apt install gnupg` (Debian/Ubuntu) или пакет `gnupg` вашего дистрибутива. |

Проверка: `gpg --version`.

### 3.2. Создать ключ

```bash
gpg --full-generate-key
```

Рекомендуемые ответы мастера (для Maven Central обычно достаточно так):

1. **Тип ключа** — `RSA and RSA` (по умолчанию).
2. **Размер RSA** — `4096`.
3. **Срок действия** — `0` = без срока (удобно для CI; иначе не забудьте продлить до истечения).
4. **Имя и email** — реальные (email лучше совпадать с тем, что виден на GitHub / в Sonatype, но это не жёсткое правило Central).
5. **Passphrase** — надёжный пароль; он понадобится Gradle как `signing.password`.

### 3.3. Узнать идентификатор ключа

```bash
gpg --list-secret-keys --keyid-format=long
```

В строке вида `sec   rsa4096/XXXXXXXXXXXXXXXX 2026-04-22 [SC]` возьмите **`XXXXXXXXXXXXXXXX`** (16 hex-символов) — это **long key id**. Для `signing.keyId` в Gradle часто указывают **последние 8 символов** (short id), например `XXXXXXXX`.

### 3.4. Опубликовать публичный ключ

Maven Central проверяет подписи по **опубликованному** открытому ключу. Отправка на keyserver:

```bash
gpg --keyserver keys.openpgp.org --send-keys <KEY_ID_SHORT_OR_LONG>
```

Если `send-keys` не проходит, на [keys.openpgp.org](https://keys.openpgp.org/) можно **загрузить** экспортированный публичный ключ вручную:

```bash
gpg --armor --export <KEY_ID> > public-key.asc
```

### 3.5. Экспорт приватного ключа для Gradle (in-memory)

Файл **только для себя**, не в репозиторий:

```bash
gpg --armor --export-secret-keys <KEY_ID> > private-key.asc
```

Содержимое `private-key.asc` целиком (включая строки `BEGIN` / `END`) вставьте в **`signing.key`** в `~/.gradle/gradle.properties`. Если вставляете **одной строкой**, замените реальные переводы строк на литералы `\n` (как в примере в §4).

Альтернатива без хранения ключа в файле properties: оставить ключ только в **gpg-agent** и в Gradle включить `useGpgCmd()` (потребует правки `build.gradle.kts`; для headless CI чаще используют именно `signing.key` + `signing.password`).

### 3.6. Проверка, что Gradle подписывает

После настройки свойств из §4:

```bash
./gradlew :closed-test-sdk:publishToMavenLocal
```

В `~/.m2/repository/com/groundspaceteam/closed-test-sdk/<version>/` рядом с `.aar`, `.pom`, jar должны появиться файлы **`.asc`**. Если `.asc` нет — `signing.key` не подхватился (имя свойства, экранирование `\n`, кодировка).

**Не коммитьте** `private-key.asc`, `public-key.asc` с секретом и пароль в репозиторий.

---

## 4. Gradle: подпись и POM в этом репозитории

В `closed-test-sdk/build.gradle.kts` уже подключены `maven-publish` и расширенный **POM** (url, license, developers, scm). Подключён плагин **`signing`**: подпись включается только если заданы свойства (см. ниже), чтобы обычная сборка `assembleDebug` не требовала ключ.

### Свойства для подписи (локально)

В **`~/.gradle/gradle.properties`** (или в `gradle.properties` в корне репо **только у себя**, не в git) добавьте:

```properties
# ASCII-armored секретный ключ целиком (включая BEGIN/END), либо одна строка с \n
signing.key=<-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK----->
signing.password=ваш_passphrase_от_ключа
# Опционально: short key id, если нужно несколько ключей
signing.keyId=ABCDEF01
```

Альтернатива — **GnuPG agent** (`useGpgCmd()`): тогда ключи не в properties, а в агенте; для CI чаще используют in-memory ключ как выше.

Проверка локальной публикации (без Central):

```bash
./gradlew :closed-test-sdk:publishToMavenLocal
```

В `~/.m2/repository/com/groundspaceteam/closed-test-sdk/<version>/` должны появиться `.aar`, `.pom`, `-sources.jar`, `-javadoc.jar` и файлы **`.asc`** (`.asc` только если задан `signing.key`).

### Необязательные свойства для POM (в `~/.gradle/gradle.properties`)

| Свойство | Назначение |
|----------|------------|
| `POM_URL` | Сайт или страница проекта (поле `url` в POM). |
| `POM_SCM_URL` | URL репозитория в SCM (GitHub и т.п.). |
| `POM_SCM_CONNECTION` | `scm:git:git://...` (read-only). |
| `POM_SCM_DEVELOPER_CONNECTION` | `scm:git:ssh://...` |
| `POM_DEVELOPER_ID` / `POM_DEVELOPER_NAME` | Секция `developers` для Central. |

Если не заданы, для `url` / `scm` / `developers` используются значения по умолчанию из `closed-test-sdk/build.gradle.kts` (см. §1).

---

## 5. Токен Sonatype (Central Portal)

1. В [central.sonatype.com](https://central.sonatype.com/) откройте профиль → **View Account** → **Generate User Token**.
2. Сохраните **username** и **password** токена (password часто показывают один раз — храните только в менеджере секретов / CI).

Дальнейшая загрузка в Central идёт **через Portal API** или инструменты сообщества — **отдельного поддерживаемого Sonatype Gradle-плагина «нажал — улетело в Central» на апрель 2026 нет**; в [официальной заметке](https://central.sonatype.org/publish/publish-portal-gradle/) указаны **JReleaser** и другие варианты.

### Секреты в CI (например GitHub Actions)

Создайте в репозитории **Settings → Secrets and variables → Actions** секреты (имена можно свои, ниже — пример):

| Секрет | Содержимое |
|--------|------------|
| `MAVEN_CENTRAL_USERNAME` | Username из User Token Sonatype |
| `MAVEN_CENTRAL_PASSWORD` | Password из User Token Sonatype |
| `SIGNING_KEY` | ASCII-armored приватный GPG-ключ целиком (или отдельный секрет под пароль ключа) |
| `SIGNING_PASSWORD` | Passphrase от GPG-ключа |

В workflow перед `./gradlew …` задайте переменные окружения с префиксом **`ORG_GRADLE_PROJECT_`** — Gradle подставит их как [project properties](https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties):

```yaml
env:
  ORG_GRADLE_PROJECT_mavenCentralUsername: ${{ secrets.MAVEN_CENTRAL_USERNAME }}
  ORG_GRADLE_PROJECT_mavenCentralPassword: ${{ secrets.MAVEN_CENTRAL_PASSWORD }}
  ORG_GRADLE_PROJECT_signing.key: ${{ secrets.SIGNING_KEY }}
  ORG_GRADLE_PROJECT_signing.password: ${{ secrets.SIGNING_PASSWORD }}
```

Имена **`mavenCentralUsername`** / **`mavenCentralPassword`** должны совпасть с тем, что ожидает ваш скрипт публикации (JReleaser, кастомный `build.gradle` и т.д.). Пока в этом репозитории **нет** шага публикации в Central из Gradle — секреты понадобятся, когда добавите workflow или JReleaser; до этого для локальной проверки достаточно `~/.gradle/gradle.properties`.

Многострочный GPG-ключ в `env` в GitHub Actions неудобен: часто кладут ключ в секрет **одной строкой (Base64)** и в шаге job пишут во временный файл перед вызовом Gradle, либо используют **JReleaser** со своей схемой секретов (см. их документацию).

В этом репозитории:

- **`.github/workflows/sdk.yml`** — сборка на push/PR (без секретов).
- **`.github/workflows/verify-gpg.yml`** — **только** ручной запуск (**Actions → Verify GPG signing → Run workflow**). Читает **`SIGNING_KEY`** и **`SIGNING_PASSWORD`**, выполняет `publishToMavenLocal` и проверяет наличие **`.asc`**.

Отдельный workflow нужен потому, что job с условием `if: github.event_name == 'workflow_dispatch'` **не выполнится** при **Re-run** прогона, который изначально был от **push** (событие остаётся `push`). Для проверки подписи всегда запускайте именно workflow **Verify GPG signing**.

Локально (без CI) те же значения можно положить в **`~/.gradle/gradle.properties`**:

```properties
mavenCentralUsername=...
mavenCentralPassword=...
```

---

## 6. Загрузка в Maven Central

Общая схема (Central Portal):

1. Собрать и **подписать** набор артефактов: AAR, POM, `-sources.jar`, `-javadoc.jar` и для каждого файла **`.asc`** (у этого репозитория Gradle уже настроен `maven-publish` + `withSourcesJar()` / `withJavadocJar()` + подпись при наличии `signing.*`).
2. Передать их в Central одним из способов:
   - **[JReleaser](https://jreleaser.org/)** с [публикацией через Central Publisher Portal](https://jreleaser.org/guide/latest/examples/maven/maven-central.html#_portal_publisher_api) (рекомендуемый путь для CI из документации Sonatype);
   - **ручная загрузка** deployment bundle / файлов через UI **Deployments**, если объём релизов небольшой;
   - свой скрипт по [Portal API](https://central.sonatype.org/publish/publish-portal-api/).
3. В разделе **Deployments** дождаться валидации (POM, подписи, координаты, наличие sources/javadoc и т.д.).
4. Нажать **Publish** в UI (если не включён автоматический выпуск).

Ссылки Sonatype:

- [Publishing via Gradle](https://central.sonatype.org/publish/publish-portal-gradle/) — статус Gradle и список community-решений;
- [Portal API](https://central.sonatype.org/publish/publish-portal-api/).

**Если Central ругается на Javadoc:** стандартный `withJavadocJar()` иногда даёт «пустой» jar для Kotlin-only кода; тогда подключите **Dokka** и публикуйте `dokkaHtmlJar` как артефакт с классификатором `javadoc` (см. обсуждения вокруг Android library + Central).

---

## 7. Чеклист перед первым релизом

- [x] Namespace **`com.groundspaceteam`** в Central = **`groupId`** в Gradle.
- [ ] В POM совпадают **url**, **scm**, **license**, **developers** с реальностью (дефолты в Gradle заведены под [closed-test-sdk на GitHub](https://github.com/diegoHeartless/closed-test-sdk); при расхождении правьте свойства или `build.gradle.kts`).
- [ ] В репозитории есть файл **LICENSE** (тот же тип лицензии, что в POM).
- [ ] Версия в `libs.versions.toml` (`closedTestSdk`) обновлена; **CHANGELOG** обновлён.
- [ ] `./gradlew :closed-test-sdk:publishToMavenLocal` создаёт **`.asc`** рядом с каждым артефактом.
- [ ] Токен Sonatype не в git; только в `~/.gradle/gradle.properties` или секретах CI.

---

## 8. Подключение у потребителей после синхронизации

После того как пакет появился на `repo1.maven.org` (иногда до 30 минут и больше):

```kotlin
// settings.gradle.kts — репозиторий по умолчанию mavenCentral() достаточно

dependencies {
    implementation("com.groundspaceteam:closed-test-sdk:0.2.0") // версия из libs.versions.toml
}

// Импорты API (пакет внутри AAR не менялся):
// import io.closedtest.sdk.ClosedTest
```

---

## Полезные ссылки

- [Central Portal](https://central.sonatype.com/)
- [Официальная документация Central](https://central.sonatype.org/)
- [Gradle и Central Portal](https://central.sonatype.org/publish/publish-portal-gradle/) (в т.ч. JReleaser)
- [Требования к артефактам и POM](https://central.sonatype.org/publish/requirements/)

Если при валидации Central пришлют конкретную ошибку по POM или артефактам — правьте блок `pom { }` в `closed-test-sdk/build.gradle.kts` и состав `singleVariant` (sources/javadoc) под их текст.
