# closed-test-sdk (Android)

Библиотека для **доказательства активности** в закрытом тестировании: сессии, foreground/background, heartbeat, очередь и отправка событий на ingest.

## Быстрый старт

- **Интеграция:** [`docs/SDK_USAGE.md`](docs/SDK_USAGE.md) (Maven, `publishable_key`, auto-init).
- **Контракт API:** [`spec.md`](spec.md) + машинный вид [`openapi/ingest.yaml`](openapi/ingest.yaml).
- **План и фазы:** [`ROADMAP.md`](ROADMAP.md).

## Maven

```kotlin
implementation("com.groundspaceteam:closed-test-sdk:<version>")
```

Версия: `gradle/libs.versions.toml` → `closedTestSdk`.

## Пример в репозитории

Модуль **`examples:sample`** — демо `trackScreen`, `trackEvent`, `trackInteraction`, deep link / `bindTester`, `flush`.

## Продуктовый контекст

Организатор и кабинет — **ProofFlow**; приложение с SDK — **anyapp** (см. `docs/SDK_USAGE.md`, раздел «anyapp и ProofFlow»).

## Ingest и Maven

- Клиент по умолчанию шлёт на **`https://api.groundspaceteam.com/`**; контракт — **`spec.md`** + **`openapi/ingest.yaml`**.
- Реализация **ingest на сервере** в работе: см. репозиторий **AndroidServer**, файл `docs/closed-test-ingest-status.md`.
- Артефакт **в Maven Central** — настроено (см. `docs/PUBLISHING_MAVEN_CENTRAL.md`).
