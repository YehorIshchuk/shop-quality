---
title: "ADR-0003: HTTP API на JDK HttpServer"
owner: Yehor Ishchuk
status: published
review_date: 2027-05-20
adr: "0003"
decision: Accepted
tags: [architecture, api]
---

# ADR-0003: HTTP API замовлень на вбудованому `HttpServer` JDK

- **Номер:** 0003
- **Статус:** Accepted
- **Дата рішення:** 2026-11-21 (код — коміт `docs(audit): simulate documentation decay, FAIL`, тег `lab10-decay-fail`)
- **Автор / власник:** Yehor Ishchuk
- **Зв'язок:** не скасовує [ADR-0001](0001-hexagonal-ports-and-adapters.md) (ручний DI зберігається) і
  [ADR-0002](0002-in-memory-storage-with-csv-export.md) (API читає те саме in-memory сховище)
- **Зачеплені документи:** [SDD §3, §4.4, §6.2](../sdd.md), [ISD §1, §3, D1, §5](../isd.md#d1), [README](../../index.md)

## Контекст

Потрібно віддавати замовлення зовнішнім клієнтам (curl, майбутній фронтенд) у JSON. Система має нуль
runtime-залежностей (ADR-0002) і ручний DI (ADR-0001). Обсяг: 3 read-only маршрути, локальне демо, без автентифікації.

## Розглянуті альтернативи

| # | Варіант | Плюси | Мінуси |
|---|---|---|---|
| A | Spring Boot (spring-boot-starter-web) | стандарт, валідація, Actuator, OpenAPI | ~20 транзитивних залежностей, DI-контейнер конфліктує з ADR-0001, довший старт і сканування Sonar |
| B | Javalin / SparkJava | легкі, зручний роутинг | зовнішня залежність заради 3 маршрутів; Jetty у класпасі |
| C | **`com.sun.net.httpserver.HttpServer` (модуль `jdk.httpserver`)** | вже в JDK 17, нуль залежностей, вписується в Composition Root | ручний роутинг і JSON; немає вбудованих метрик/безпеки |

## Рішення

Обрано **C**: адаптер `api/ShopHttpApi` залежить лише від порту `OrderStore` і `OrderMapper`, створюється в
`ShopApplication` після демо-сценарію; порт задає `SHOP_HTTP_PORT` (за замовч. 8080). Процес стає довгоживучим.

## Наслідки

- ✅ `pom.xml` без змін, тести API без фреймворку (`ShopHttpApiTest`, порт 0).
- ⚠️ D1 тепер сервіс: змінено порядок запуску, з'явився TCP-порт і health-check (ISD §1, D1, §5).
- ⚠️ JSON формується вручну — при рості API (> 5 маршрутів, POST з валідацією) переглянути варіант A/B новим ADR.
- ⚠️ Без автентифікації: порт не публікувати за межі локальної машини.

## Пов'язані документи

[[sdd]] · [[isd]] · [[0001-hexagonal-ports-and-adapters|ADR-0001]] · [[0002-in-memory-storage-with-csv-export|ADR-0002]]
