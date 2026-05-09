# Roadmap: closed-test SDK (Android)

Дорожная карта клиента **anyapp** и контракта **ingest**. Продуктовый контекст (ProofFlow, организатор) — в `ProofFlow/docs/`.

**Нормативный контракт:** `spec.md`, `openapi/ingest.yaml`.  
**Статус приёма на сервере:** `AndroidServer/docs/closed-test-ingest-status.md` (другой репозиторий).

---

## Продуктовое саммари (без изменений)

Мониторинг закрытого тестирования: SDK в приложении разработчика, прозрачность активности, без маркетплейса тестеров. См. предыдущие разделы исторического ROADMAP — философия и границы open SDK / серверный «интеллект» сохраняются.

---

## Состояние реализации (аудит SDK + связанный бэкенд)

| Область | Статус |
|---------|--------|
| **Фаза 0** — `spec.md`, PII, коды ошибок, OpenAPI ingest | **Готово** |
| **Фаза 1** — `ClosedTest.initialize`, lifecycle-сессии, heartbeat, `trackScreen`, flush, очередь, ретраи, deep link / `bindTester`, AndroidX Startup, sample | **Готово** |
| **Фаза 2** — backoff, лимиты очереди/батча, consumer ProGuard, SemVer, `CHANGELOG` | **Готово** |
| **Фаза 3** — `trackInteraction` / `trackEvent`, санитайз props | **Готово** |
| **Discovery** — `ContentProvider` для ProofFlow (`DISCOVERY_AUTHORITY_SUFFIX`) | **Готово** (см. CHANGELOG 0.2.1+) |
| **Base handshake** — опциональный `publishable_key`, поля `package_name`, `build_type`, `version_name`, `version_code` | **Готово** (SDK ≥ 0.2.2) |
| **AndroidServer ingest** — `POST /v1/init`, refresh, events, SQLite | **Частично** — MVP с env-ключами и env-Base-кортежами; нет флага keyless в БД, нет выдачи ключей из ProofFlow API |
| **Фаза 4** — Play Integrity, жёсткая анти-спуф без ключа | **Не делалось** |

Историческая строка «логика ingest не реализована» **снята**: модуль `closed-test-ingest` в AndroidServer реализован по MVP; дальнейшее — политика продуктов и multi-app (см. ниже).

---

## Что ещё не сделано (приоритет для SDK-репозитория)

### Контракт и совместимость

- [ ] Синхронизировать **Kotlin DTO** / сериализацию с каждым изменением `openapi/ingest.yaml` и Nest DTO (регресс — один PR на контракт).
- [ ] **Multi-app URL:** миграция с `/v1/...` на `/api/<ingest-slug>/v1/...` — потребует согласования с AndroidServer и, вероятно, **мажорной** версии SDK (см. `UNIFIED_API_DRAFT.md`).

### Поведение клиента

- [ ] Опциональный гайд: **один вызов** `trackScreen` из Compose / Navigation (отдельный doc или sample).
- [ ] При появлении на сервере **полей контекста теста** в ответе `init` — расширить `InitResponseDto` и документацию (связка с ProofFlow / TEST_DISCOVERY).

### Доверие к данным (Фаза 4)

- [ ] Опционально **Play Integrity** (или аналог): передача токена на сервер при политике продукта; клиентская эвристика только как подсказка.

### Публикация

- [ ] После значимых изменений — релиз в Maven, версия в `gradle/libs.versions.toml`, запись в `CHANGELOG.md`.

---

## Зависимости от продукта и сервера (не только этот репозиторий)

Реализуется в **ProofFlow / AndroidServer**, SDK лишь потребляет контракт:

- [ ] **Флаг keyless в БД:** приём Base только если организатор включил keyless для теста; до тех пор env-Base на сервере — временная мера.
- [ ] Выдача **publishable_key** через API, привязка к тесту/организации.
- [ ] Квоты хранения / «достаточно N подтверждений» — политика сервера (см. `PRODUCT_DRAFT` §2.6).
- [ ] Дедуп **`event_id`**, расширенная валидация переходов событий — сервер.

---

## Фазы работ (исторические якоря)

Фазы 0–3 по сути закрыты в коде; актуальные задачи перенесены в таблицы выше. Фаза 4 (Integrity и усиление доверия) остаётся **по необходимости** после стабилизации ingest и политики ключей.

---

## Риски (кратко)

Без изменений по смыслу: восприятие как «ещё одна аналитика», фейковые HTTP к ingest (см. `spec.md` и продуктовые меры — ключ, allowlist, Integrity).

---

## Ссылки

- Публикация: `docs/PUBLISHING_MAVEN_CENTRAL.md`
- Интеграция: `docs/SDK_USAGE.md`
- Продукт ProofFlow roadmap: `ProofFlow/docs/ROADMAP.md`

История обсуждения концепции (внешняя ссылка, может устареть): [архив чата](https://chatgpt.com/share/69e7e410-0198-832e-8d71-1463234495cb).
