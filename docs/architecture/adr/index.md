---
title: Кущ ADR
owner: Yehor Ishchuk
status: published
review_date: 2027-05-20
tags: [architecture]
---

# Architecture Decision Records

Канон ведення куща рішень:

- **Наскрізна нумерація** `ADR-0001, 0002, …`; номер ніколи не перевикористовується, файл `NNNN-коротка-назва.md`.
- **Життєвий цикл статусу:** `Proposed` → `Accepted` → `Superseded by ADR-NNNN` (або `Deprecated`).
  Статус записано в тілі ADR і в front matter (`decision:`).
- **Незмінюваність:** прийнятий запис не редагується по суті. Нове рішення = новий ADR з наступним номером,
  у старому змінюється лише статус. Шаблон: номер, статус, дата, контекст, розглянуті альтернативи, рішення, наслідки.

| № | Рішення | Статус | Дата |
|---|---|---|---|
| [0001](0001-hexagonal-ports-and-adapters.md) | Порти та адаптери з ручним DI замість God Object | Accepted | 2026-11-15 |
| [0002](0002-in-memory-storage-with-csv-export.md) | Сховище в пам'яті процесу + CSV-експорт замовлень | Accepted | 2026-11-15 |
| [0003](0003-embedded-jdk-http-server.md) | HTTP API на вбудованому `HttpServer` JDK | Accepted | 2026-11-21 |
| [0004](0004-javalang-java8-syntax-limit.md) | OO-гейти на javalang і обмеження синтаксису Java 8 + план міграції | Accepted | 2026-11-22 |
