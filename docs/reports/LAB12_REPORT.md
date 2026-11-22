---
title: Звіт ЛР12
owner: Yehor Ishchuk
status: published
review_date: 2027-11-22
tags: [process, ops]
---

# Звіт ЛР12. Експлуатація, управління змінами і супровід (SRE · ITIL · SWEBOK)

- **Студент:** Yehor Ishchuk
- **Об'єкт:** `shop-quality` після ЛР11 (база знань MkDocs, конвеєр `docs.yml`). Репозиторій: <https://github.com/YehorIshchuk/shop-quality>
- **Сайт документації:** <https://yehorishchuk.github.io/shop-quality/> (розділ «5 · Експлуатація і супровід»)
- **Дата:** 2026-11-22 · усі часи — UTC

**Коміти ЛР12:**

| Коміт / тег | Повідомлення | Завдання |
|---|---|---|
| `5ef1e30` | `ops(sre): add runbook RB-001, alert catalog, SLO spec, backup policy and restore drill` | 1 |
| `bb81500` | `docs(change): RFC-001 with 7-level impact analysis, approved by CCB-001` | 2 |
| `b76df67` (тег `v1.1.0-rc.1`) | `ci(quality): run OO gates in GitHub Actions (RFC-001)` | 2 — реалізація RFC |
| `b6af025` (тег `v1.1.0`) | `chore(release): v1.1.0 - release checklist GO, RFC-001 closed` | 2 — go/no-go |
| `60f7c45` | `docs(legacy): reverse-engineer tools/oo_metrics.py …` | 3.1 |
| `f966559` | `refactor(port): make PaymentResult a Java 17 record` | 3.2 — тригер інциденту |
| `821c18f` (тег `lab12-incident-fail`) | `ops(incident): capture legacy module outage, FAIL` | 3.2 |
| `1beae41` (тег `lab12-capa-pass`) | `docs(maintenance): deliver capa actions and swebok plan, PASS` | 3.3–3.5 |
| `fda32db` | `fix(ops): restore drill strict build without -q, drill #3 PASS` | 1 (знахідка валідації) |
| `d095a26` | `fix(ops): restore drill refuses busy port and unhealthy restored API` | 1 (знахідка валідації) |
| наступний | `fix(ops): deploy-api.sh stops the real JVM and refuses a busy port` | 1 (знахідка валідації) |

---

## Завдання 1. SRE: runbook, моніторинг, SLO і резервне копіювання

### 1.1 Runbook RB-001 і рівень автоматизації

[RB-001](../runbooks/RB-001.md) — деплой, перезапуск і відкат HTTP API (D1). Канонічні розділи: тригер, передумови,
рівень автоматизації, кроки, верифікація, діагностика, відкат, ескалація. Рівень **1** (скрипти `deploy-api.sh
deploy|rollback|status`, рішення приймає людина); шлях до рівня 2 — автоматичний відкат при провалі health, рівня 3 —
самовідновлення супервізором. Відкат перевірено: `lab10-docs-pass → lab11-ci-pass → rollback`, health UP за 2 с
(доказ — `DELETE /api/health` = 200 на старій версії) — [лог](https://github.com/YehorIshchuk/shop-quality/blob/main/docs/reports/lab12/deploy-rollback.log).
Прогін на MacBook виявив, що скрипт не зупиняв справжню JVM (лог ШІ, п. 17) → виправлено, повторний сценарій з
перевіркою «одна JVM, порт слухає новий PID» — [лог](https://github.com/YehorIshchuk/shop-quality/blob/main/docs/reports/lab12/deploy-pid-fix.log).

> 📸 **Скріншот 1** — `./scripts/deploy-api.sh status` і `rollback` у терміналі.

### 1.2 Каталог алертів

[alerts.md](../runbooks/alerts.md): A-01 ShopApiDown (SEV-1), A-02 ShopApiErrorRate > 1 % (SEV-2), A-03 ShopApiLatencyP95
≥ 300 мс (SEV-3), A-04 ErrorBudgetBurn ≥ 50 % (SEV-3), **A-05 OOGateToolError** (SEV-2, додано за CAPA-1). Кожен
алерт має умову, серйозність, роль і посилання на runbook. Відсутність алерту для конвеєра до інциденту — свідома
знахідка RCA ([PM-001](../incidents/PM-001.md), гілка «Моніторинг»).

### 1.3 SLI / SLO / SLA і бюджет помилок

| Термін | Що це | У проєкті |
|---|---|---|
| **SLI** | виміряний показник якості з погляду користувача (частка «хороших» подій) | частка не-5xx відповідей; частка 10-секундних вікон з p95 < 300 мс; частка часу, коли конвеєр видав вердикт |
| **SLO** | внутрішня ціль для SLI на вікні | 99.5 % / 99.0 % / 95.0 % за 30 днів |
| **SLA** | зовнішнє зобов'язання з наслідками; завжди **м'якше** за SLO | 99.0 % / 97.0 % / 90.0 % — різниця = буфер реакції |

**Бюджет помилок** = (100 % − SLO) × 43 200 хв (30 днів):

| SLI | SLO | Бюджет SLO, хв | SLA | Поріг SLA, хв |
|---|---|---|---|---|
| `api-availability` | 99.5 % | 0.005 × 43 200 = **216** | 99.0 % | 432 |
| `api-latency` | 99.0 % | 0.01 × 43 200 = **432** | 97.0 % | 1 296 |
| `pipeline-verdict` | 95.0 % | 0.05 × 43 200 = **2 160** | 90.0 % | 4 320 |

**Політика:** < 50 % — нормальний режим; 50–100 % — обережний (кожен RFC з перевіреним відкатом і голосом SRE);
≥ 100 % — **change freeze** (лише термінові зміни стабільності/CAPA). Арифметику перевіряє `tools/slo_budget.py --check`
у CI. Виміряні значення (6 000 запитів, `sli_probe.py`): availability 100 %, p95 9.22 мс.

Лістинг `docs/slo.yaml`:

```yaml
{% include "../slo.yaml" %}
```

### 1.4 ISD: експлуатаційні процедури, RTO/RPO, 3-2-1, restore drill

[ISD §6–7](../architecture/isd.md): RTO коду/документації ≤ 15 хв (розрахунок 11 хв: виявлення 2 + розшифрування 1 +
clone 1 + `mvn verify` 3 + strict-збірка 3 + health 1), RPO ≤ 24 год; D1 RTO ≤ 5 хв. Backup 3-2-1: SSD + GitHub +
зашифрований (AES-256, PBKDF2 200 000) bundle на USB; повна копія щонеділі, інкрементна щодня, retention 14 дн / 8 тиж.
Порядок відновлення — за залежностями (D3 → D1 → D5 → D4). Upgrade із «міграціями» — контракт OpenAPI, конфігурація,
CHANGELOG.

**Restore drill:** #1 — ❌ FAIL (`No module named mermaid2`: відновлення неявно залежало від системного mkdocs) →
крок переписано на чисте venv; #2 — ✅ PASS, RTO 20 с; #3 — ✅ PASS після виправлення `--strict -q` → `--strict` (див. лог ШІ, п. 15).

> 📸 **Скріншот 2** — `restore-drill.sh`: рядок `RESTORE DRILL: PASS` і RTO.

---

## Завдання 2. ITIL: RFC, Impact Analysis, CCB, Release Checklist

- [RFC-001](../changes/RFC-001.md) — перенесення OO-гейтів ЛР8 у GitHub Actions: клас **normal**, ризик середній,
  вікно 19:00–19:30 UTC; ланцюг станів Draft → Assessment → Pending CCB → Approved → Implemented → Closed з часами.
- **Impact Analysis на 7 рівнів** (код, архітектура, дані, API, інфраструктура/CI, документація, люди/процес) і
  **Risk Exposure = P × I**: R1 «javalang не розбирає Java 9+ → хибний червоний» 2 × 3 = **6 (середній)**, R2 = 5, R3 = 3;
  план відкату — `git revert` або `if: false`, ≤ 5 хв.
- [CCB-001](../changes/CCB-001.md) — архітектор, QA, SRE, Product Owner, кворум 4/4; рішення «погоджено з умовами» У1–У5;
  регламент (щотижня + позачергово ≤ 2 год після SEV-1/2, change freeze за бюджетом помилок, звітність).
- [Release Checklist v1.1.0](../changes/release-checklist.md) — 7 критеріїв go/no-go з доказами (staging RC на
  порту 18091, Schemathesis 246/246, OO GATE PASS ×2, JUnit 25/25) → **GO**; ланцюг трасування RFC → CCB → коміт →
  тег → перевірки → реліз.

> 📸 **Скріншот 3** — Actions: запуск на `v1.1.0` — зелені `build`, `api-contract`, **`oo-gates`**, `deploy`.

---

## Завдання 3. Супровід: legacy, інцидент, RCA, CAPA, план супроводу

### 3.1 Документація legacy-компонента

[oo-metrics.md](../legacy/oo-metrics.md) — `tools/oo_metrics.py` (найгірший Bus Factor за аудитом ЛР10): граф
залежностей (4 точки виклику, включно з PowerShell — перша гіпотеза була хибною), формати вводу/виводу, коди виходу,
функції з номерами рядків, кожне твердження позначене **verified / assumed**; реєстр боргу A-1…D-2.

### 3.2 Інцидент INC-001 (FAIL)

Коректний рефакторинг `PaymentResult` → `record` (тести 25/25) → `oo_metrics.py` не розбирає файл (javalang ≤ Java 8)
→ job `oo-gates` exit 1 (той самий код, що порушення CBO) → `deploy` skipped. [INC-001](../incidents/INC-001.md):
SEV-2, хронологія UTC, обхід, стабілізація, вплив. Тривалість 7 хв 56 с = **0.4 % бюджету** `pipeline-verdict`,
SLO і SLA не порушено.

> 📸 **Скріншот 4** — Actions, запуск на `ops(incident)…FAIL`: `oo-gates` ❌, `deploy` skipped, рядок
> `ERROR: cannot parse …PaymentResult.java near line 4` у лозі.

### 3.3 Post-Mortem, 5 Whys, Ішікава

[PM-001](../incidents/PM-001.md) (blameless). Ланцюг: конвеєр без вердикту → javalang ≤ Java 8 → обмеження було лише
текстом, без перевірки і review → разовий обхід у ЛР8 без ADR, знання в однієї людини → **регламент CCB дозволив
залишковий ризик без власника, а ADR/тести не поширювались на інструменти конвеєра** (процесна першопричина).
Діаграма Ішікави на 6 гілок (люди, процес, технологія, дані, середовище, моніторинг) — Mermaid у PM-001.

### 3.4 CAPA

| ID | Тип | Суть | Статус | Перевірка ефективності |
|---|---|---|---|---|
| CAPA-1 | коригувальна | код 3 «OO TOOL ERROR» замість 1, перелік усіх файлів, 10 тестів, алерт A-05 + [RB-002](../runbooks/RB-002.md) | ✅ | повтор тригера → exit 3 ✅; 30 днів без збоїв без вердикту (2026-12-22) |
| CAPA-2 | коригувальна | `java_syntax_guard.py` у CI і скриптах, клас повернуто, CCB: залишковий ризик лише з власником, CODEOWNERS | ✅ | повтор тригера → `JAVA SYNTAX GUARD: FAIL … PaymentResult.java:4` ✅ |
| CAPA-3 | **запобіжна** | ADR-0004 з планом міграції на tree-sitter до 2027-02-28, щотижневий плановий запуск, реєстр інструментів | 🟡 | 4 планові запуски з вердиктом; Java 17 fixture розбирається до 2027-03-01 |

**Коригувальна** дія прибирає причину інциденту, що **вже стався** (щоб він не повторився); **запобіжна** — причину
**потенційної** проблеми, якої ще не було (інші інструменти конвеєра, майбутні залежності).

> 📸 **Скріншот 5** — Actions, запуск на `docs(maintenance)…PASS`: усі job зелені, у `oo-gates` видно кроки
> `Unit tests of OO tools` і `Java syntax guard`.
>
> 📸 **Скріншот 6** — термінал: `capa-effectiveness-replay.log` (exit 1 від guard і exit 3 від гейта).

### 3.5 План супроводу

[maintenance-plan.md](../maintenance-plan.md): лінії L1/L2/L3 з критеріями передачі, матриця ескалації за SEV (час
реакції, адресати, канали), Known Issues KI-1…KI-6 з обхідними рішеннями, пріоритети вдосконалення legacy, прив'язані до
реєстру боргу, реєстр інструментів конвеєра, посилання на runbooks і `slo.yaml`.

---

## Лог валідації ШІ-Техліда

**Декларація.** ЛР12 виконано в діалозі з ШІ-асистентом Claude (Anthropic). ШІ згенерував скрипти (`sli_probe.py`,
`slo_budget.py`, `deploy-api.sh`, `backup.sh`, `restore-drill.sh`, `java_syntax_guard.py`, зміни `oo_metrics.py`, тести),
тексти runbook, RFC, CCB, INC, **а також чернетки 5 Whys, Ішікави і карток CAPA**. Коміти ШІ мають трейлер
`Co-Authored-By: Claude`. Регламент передбачає **−3 бали, якщо RCA/CAPA виконав ШІ замість студента**, і −2, якщо
студент не пояснює SLI/SLO/SLA або плутає коригувальні й запобіжні дії. Тому студент особисто переглянув і
переформулював ланцюг 5 Whys та картки CAPA, звірив кожен доказ із кодом і логами (таблиця нижче) і має пояснити їх
на захисті своїми словами.

| # | Крок / твердження ШІ | Як перевірено | Результат |
|---|---|---|---|
| 1 | «javalang не оновлювався з 2018 року» | історія релізів PyPI | ❌ останній реліз **2020-03-28** → виправлено в усіх документах |
| 2 | «`oo_metrics.py` викликається лише через `oo-audit.sh`» | `grep oo_metrics scripts/*` | ❌ три `.ps1` викликають модуль напряму → граф залежностей виправлено |
| 3 | Restore drill пройде з першої спроби | запуск `restore-drill.sh` | ❌ drill #1 FAIL (`mermaid2`) → чисте venv, drill #2 PASS |
| 4 | Кириличні якорі `#3-матриця-ескалації` працюють | strict-збірка | ❌ slugify за замовчуванням ламав якорі → `pymdownx.slugs.slugify` |
| 5 | Wikilink з `\|` у таблиці | strict-збірка | ❌ `roamlinks` ламає таблицю → звичайне посилання |
| 6 | Лог `deploy-api.sh … \| tee` | прогін | ❌ фоновий процес тримав pipe, лог «завис» → KI-3 у плані супроводу |
| 7 | Коміт legacy-документації не зачепить ворота актуальності | `doc_audit.py --gate` | ❌ `tools/doc-map.json` без ISD → BEHIND; коміти перебудовано, щоб інцидент був «чистим» |
| 8 | Тригер ламає лише `oo-gates` | відтворення конвеєра | ✅ build і api-contract зелені, `oo-gates` exit 1, deploy skipped |
| 9 | Regex guard точно знаходить рядок | модульний тест | ❌ спершу звітував рядок 2 замість 3 (`^\s*` захоплював `\n`) → `[ \t]*` |
| 10 | Guard і парсер узгоджені | тест `test_guard_agrees_with_parser_on_fixtures` | ✅ 3 fixtures |
| 11 | Лог PASS-конвеєра коректний | перегляд логу | ❌ невірний аргумент `--base` у `sli_probe` і код виходу від `tail` замість гейта → рядки переміряно |
| 12 | Бюджет 8 хв = 0.4 % | `slo_budget.py --consumed pipeline-verdict=8` | ✅ |
| 13 | Часи тріажу 19:11–19:15 в INC-001 | — | ⚠️ це хвилини симуляції (ручні кроки), не машинні мітки; машинні — з логів |
| 15 | `restore-drill.sh` за замовчуванням збирає базу знань `mkdocs build --strict -q` | мутаційна перевірка: мертве посилання + `--strict -q` | ❌ **exit 0 — хибний PASS** (та сама пастка, що в ЛР11 п. 8) → без `-q`, drill #3 PASS |
| 16 | «Restore drill PASS на MacBook» (скріншот 2) | `curl localhost:18090/api/health` після drill | ❌ відповів API з деплою, що лишився запущеним: drill не перевіряв, що порт вільний і health дав **свій** процес → перевірки додано, негативний тест FAIL / позитивний PASS (drill #4) |
| 17 | «`deploy-api.sh` зупиняє стару версію перед запуском нової» (скріншот 1, «HEALTH OK» на кожному кроці) | `lsof -iTCP:18090` на MacBook після відкату | ❌ порт тримала **перша** JVM (PID 49427), а в `api.pid` був PID 49426 зайвого підшела: `( cd && java > log & )` фонить увесь список. Тож «розгортання v1.1.0» і «відкат» відповідала стара версія — хибні HEALTH OK. Та сама причина — зависання під `\| tee` (KI-3). Виправлено: `exec` + PID із порту + відмова на зайнятому порту, сценарій перевірено з лаунчером як на macOS |
| 14 | Конвеєр на GitHub: зелений → червоний → зелений | Actions після push (скріншоти 3–5) | заповнює студент |

**Висновок.** Одинадцять тверджень ШІ (1–7, 9, 11, 15–17) без перевірки дали б хибні документи. Найважливіше для ЛР12 — п. 7:
ШІ ледь не зробив інцидент «брудним» (червоним із двох причин), що зруйнувало б RCA.

## Пов'язані документи

[[INC-001]] · [[PM-001]] · [[maintenance-plan]] · [[RFC-001]] · [[slo]]
