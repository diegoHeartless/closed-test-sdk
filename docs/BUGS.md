# SDK — отслеживаемые баги и инциденты

Внутренний бэклог до переноса в трекер (GitHub Issues и т.д.). Формат: кратко симптомы, окружение, что проверить.

---

## OPEN

### BUG-SDK-001 — anyapp не падает, на ingest ничего не уходит, баннера ProofFlow нет

**Статус:** open (2026-05-14)  
**Окружение (пример):** `com.ground.detoxrpg`, Samsung SM-A525F (Android 12), closed-test-sdk **≥ 0.2.6** (после фикса PRAGMA / Room); в logcat возможны `avc: denied { ioctl }` на `closed_test_sdk.db` / `-journal` (`ioctlcmd=0xf522`), **приложение при этом живёт**.

**Симптомы**

- Телеметрия **не доходит** до сервера (нет ожидаемого трафика `POST /v1/init`, сессий, батчей).
- **Нет** диалога подсказки ProofFlow после init (баннер/hint).

**Гипотезы / что проверить при расследовании**

1. **Handshake:** `POST /v1/init` — 403/401 (keyless не включён для пакета, другой хост ingest vs ProofFlow PATCH, неверный `package_name` в политике). Сеть / прокси.
2. **SDK в noop:** debuggable-сборка и `ClosedTestOptions.collectInDebuggableBuilds == false`.
3. **Баннер:** сервер не отдаёт `proofflow_test_id`; ProofFlow не установлен; `proofFlowHintEnabled` / meta-data; лимиты `proofFlowHintMaxShows` / cooldown; нет подходящей Activity для показа.
4. **Очередь:** исключения при записи в Room (отдельно от audit); токены не сохраняются.

**Не считать закрытым**, пока не воспроизведён минимальный happy-path: успешный `init` → события на сервере → при наличии политики и `proofflow_test_id` — сценарий hint по спецификации.

**Связанные материалы:** `docs/SDK_USAGE.md`, `CHANGELOG.md`, ProofFlow `STATS_AND_DEEPLINKS_DRAFT.md` §3.1.1, AndroidServer closed-test-ingest + keyless policy.

---

## CLOSED

_(пока пусто)_
