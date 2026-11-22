---
title: Регламент супроводу документації
owner: Yehor Ishchuk
status: published
review_date: 2027-02-19
tags: [process, developer]
type: process / regulation
review_cycle_days: 90
last_reviewed: 2026-11-21
---

# Регламент підтримки документації

> Застаріла документація небезпечніша за відсутню: вона створює ілюзію розуміння системи.
> Тому кожен документ паспорта має **власника**, **цикл перегляду** і **тригер оновлення в коді**.

## 1. Головне правило

**Зміна коду = зміна документації.** Жоден коміт/злиття в `main`, що змінює поведінку, структуру або
розгортання, не приймається без оновлення зачеплених документів **у тому самому коміті / PR**.
Перевірка автоматична: `python3 tools/doc_audit.py --gate` має дати `DOC CURRENCY GATE: PASS`
(Behind = 0 для всіх компонентів, жодної провалу структурних перевірок). Відповідність «код → документ»
задано в [doc-map.json](https://github.com/YehorIshchuk/shop-quality/blob/main/tools/doc-map.json); чекліст — у [шаблоні PR](https://github.com/YehorIshchuk/shop-quality/blob/main/.github/pull_request_template.md).

## 2. Таблиця власників і циклів перегляду

Цикл за критичністю (пороги Завдання 1 ЛР10): **експлуатаційна — щонайменше раз на 90 днів**, **архітектурна — раз на 180 днів**.

| Документ | Категорія | Власник | Цикл перегляду | Дата останнього перегляду | Наступний перегляд | Тригер у коді (оновити, якщо змінено) |
|---|---|---|---|---|---|---|
| [README.md](../index.md) | експлуатаційна | Yehor Ishchuk | 90 днів | 2026-11-21 | 2027-02-19 | `ShopApplication.java`, публічна поведінка, команди запуску |
| [sdd.md](../architecture/sdd.md) | архітектурна | Yehor Ishchuk | 180 днів | 2026-11-21 | 2027-05-20 | будь-що в `src/main/java` (новий пакет, адаптер, порт, маршрут) |
| [isd.md](../architecture/isd.md) | експлуатаційна | Yehor Ishchuk | 90 днів | 2026-11-21 | 2027-02-19 | `ShopApplication.java` (режим запуску), `pom.xml`, `scripts/`, `tools/`, нові змінні середовища / порти |
| [ADR-0001](../architecture/adr/0001-hexagonal-ports-and-adapters.md) | архітектурна | Yehor Ishchuk | 180 днів | 2026-11-21 | 2027-05-20 | зміна шарів / способу DI → **новий ADR**, старий `Superseded` |
| [ADR-0002](../architecture/adr/0002-in-memory-storage-with-csv-export.md) | архітектурна | Yehor Ishchuk | 180 днів | 2026-11-21 | 2027-05-20 | зміна сховища → **новий ADR**, старий `Superseded` |
| [ADR-0003](../architecture/adr/0003-embedded-jdk-http-server.md) | архітектурна | Yehor Ishchuk | 180 днів | 2026-11-21 | 2027-05-20 | зміна веб-стеку / відкриття API назовні → **новий ADR** |
| [handover.md](handover.md) | процес | Yehor Ishchuk | 90 днів | 2026-11-21 | 2027-02-19 | зміна власника, новий компонент, зміна онбордингу |
| [CONTRIBUTING](CONTRIBUTING.md), [local-setup](local-setup.md), [coding-standards](coding-standards.md) | експлуатаційна | Yehor Ishchuk | 90 днів | 2026-11-22 | 2027-02-19 | зміна процесу внесення змін, інструментів, правил якості |
| `mkdocs.yml`, `requirements-docs.txt` | експлуатаційна | Yehor Ishchuk | з кожною зміною | 2026-11-22 | 2027-02-19 | нова сторінка (nav), новий плагін |
| [maintenance.md](maintenance.md) | процес | Yehor Ishchuk | 90 днів | 2026-11-21 | 2027-02-19 | новий документ у паспорті, зміна власника |

**Журнал позапланових переглядів:** 2026-11-21 — README, SDD, ISD, ADR-0003 оновлено після додавання HTTP API
(коміт `docs(maintenance): restore documentation currency, PASS`; FAIL-стан — тег `lab10-decay-fail`).

Документи процесу (`docs/reports/LAB*_REPORT.md`, `docs/reports/lab8/*.log`, `reports/**`, повідомлення комітів) — історичні записи,
**не переглядаються і не переписуються**. `DEMO.md` і `lab8/architecture-blueprint.md` — `deprecated`.

## 3. Процедура

1. **Під час зміни коду.** Автор визначає зачеплені документи за колонкою «Тригер», оновлює їх у тому ж коміті,
   запускає `doc_audit.py --gate`. При FAIL злиття блокується.
2. **Плановий перегляд.** У дату «Наступний перегляд» власник перечитує документ проти коду, виправляє розбіжності,
   оновлює `last_reviewed` у front matter і в таблиці вище. Коміт `docs(<doc>): periodic review`.
3. **Архітектурне рішення.** Новий ADR з наступним номером; старий ніколи не редагується по суті, лише статус.
4. **Вивід з обігу.** Документ без споживача (Dead Documentation) — видалити; документ, що описує минулий стан,
   але потрібен для історії — банер `DEPRECATED` з посиланням на актуальне джерело.
5. **Аудит.** Раз на цикл зберегти звіт `python3 tools/doc_audit.py --stage <дата> --out reports/docs/<дата>.md`.

## Пов'язані документи

[[CONTRIBUTING]] · [[handover]] · [[sdd]] · [[isd]]
