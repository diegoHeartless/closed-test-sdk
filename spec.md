# Спецификация ingest API и событий SDK (Фаза 0)

**Версия документа:** 0.1.0  
**Машинный контракт:** `openapi/ingest.yaml`

Базовый URL задаётся продуктом, пути API — с префиксом **`/v1`**.

## 1. Назначение

Контракт между Android SDK и бэкендом: handshake после `init`, краткоживущий `session_token`, `refresh_token`, приём батчей событий. Семантика «валидной сессии» и анти-абуз — на сервере (см. `ROADMAP.md`).

## 2. Версионирование

- Мажор API в URL: `/v1/...`
- Новые **опциональные** поля — обратно совместимы; ломающие изменения — новый мажор или явная deprecation-политика
- `sdk_version` — SemVer строкой

## 3. Эндпоинты (кратко)

| Метод и путь | Назначение |
|--------------|------------|
| `POST /v1/init` | `publishable_key`, `device_id`, версии; ответ — токены и сроки жизни |
| `POST /v1/session/refresh` | Обмен `refresh_token` на новую пару токенов |
| `POST /v1/events` | Батч событий, `Authorization: Bearer <session_token>` |

Рекомендуется заголовок `X-Request-Id` (UUID). Детали тел и ответов — в `openapi/ingest.yaml`.

## 4. Каталог `type` событий

| `type` | Описание |
|--------|----------|
| `session_start` | Начало логической сессии использования |
| `session_end` | Конец сессии |
| `app_foreground` / `app_background` | Передний план / фон (отсекают «открыл и свернул») |
| `heartbeat` | Периодический сигнал только в активном приложении (15–30 с) |
| `screen_view` | Экран; имя задаёт разработчик |
| `track_interaction` | Факт действия без детализации UI (опционально) |
| `track_event` | Именованное событие + опциональные `props` |

## 5. Обязательные поля каждого события

`type`, `occurred_at` (RFC 3339), `monotonic_ms`, `device_id` (UUID в хранилище приложения), `sdk_version`, `app_version`, `os` (`android`), `os_version`.

Опционально: `event_id` (дедуп), `session_id`, `test_session_id`, `tester_id` (после binding).

## 6. Поля по типам

- `session_start` / `session_end`: опционально `reason` (`cold_start`, `timeout`, `user_quit`, …)
- `heartbeat`: обязательно `interval_ms`
- `screen_view`: обязательно `screen_name`
- `track_interaction`: опционально `category`
- `track_event`: обязательно `name`; опционально `props` — только без PII (§8)

Пример батча см. в OpenAPI (`EventsBatchRequest` / документация SDK).

## 7. Коды ошибок (`error.code`)

| HTTP | Код | Смысл |
|------|-----|--------|
| 400 | `invalid_request` | Формат / обязательные поля |
| 401 | `unauthorized` | Неверный/истёкший session token |
| 401 | `invalid_publishable_key` | Ключ в `init` |
| 403 | `forbidden` | Операция запрещена для ключа/теста |
| 409 | `duplicate` | Дедуп по `event_id` / `batch_id` |
| 422 | `invalid_event_payload` | Семантика или запрещённые `props` |
| 429 | `rate_limited` | Лимиты |
| 500 | `internal_error` | Повтор с backoff |

Тело: `{ "error": { "code", "message", "request_id?" } }`.

## 8. PII и `track_event.props`

**Не передавать** PII в `props`; `screen_name` и `name` не должны нести email/телефон/ФИО.

**Запрещённые ключи** верхнего уровня в `props` (без учёта регистра; сервер отвечает 422 или по политике продукта):

email, mail, phone, telephone, msisdn, password, passwd, secret, token, access_token, refresh_token, api_key, authorization, cookie, ssn, credit_card, card_number, iban, gps, latitude, longitude, lat, lng, precise_location, street_address, address_line, full_name, first_name, last_name, birthdate, dob, government_id, passport, driver_license, health, diagnosis, imei, serial, android_id

Сырой железный/рекламный идентификатор не подменяет `device_id` из SDK.

**Data safety:** в документации SDK указать: время сессии, имена экранов и именованные события — для подтверждения участия в closed testing.

## 9. Следующие шаги

Согласовать с бэкендом базовый URL, лимиты батча и RPS, отдельный ли нужен эндпоинт `bind` или достаточно полей в `init`/событиях.

---

*Выход Фазы 0 из `ROADMAP.md`.*