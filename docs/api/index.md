---
title: API — контракт і Swagger UI
owner: Yehor Ishchuk
status: published
review_date: 2027-02-19
tags: [api]
---

# Рівень 3 · API Docs

> **Питання рівня:** як інтегруватися?

**Єдине джерело істини** — машинно-читний контракт [`openapi.yaml`](openapi.yaml) (OpenAPI 3.0.3, версія API за SemVer).
Довідка нижче **не копіює** контракт: плагін `swagger-ui-tag` рендерить її з того самого файлу під час збірки сайту.
Змінили `openapi.yaml` — змінилась довідка; розсинхронізуватися фізично нема чому.

| Перевірка | Чим | Де в конвеєрі |
|---|---|---|
| якість контракту | Spectral, правила `.spectral.yaml` | стадія `api-contract` |
| відповідність живому коду (контрактний дрейф) | Schemathesis проти запущеного API: 470 тест-кейсів, 0 розбіжностей | стадія `api-contract` |
| клієнтський SDK | OpenAPI Generator (`-g java --library native`) | стадія `api-contract`, артефакт `sdk-client` |

## Інтерактивна довідка (Swagger UI)

Щоб «Try it out» працював, запустіть API локально (`java -cp target/classes com.kira.shop.ShopApplication`) —
сервер у контракті `http://localhost:8080` — і відкрийте цю сторінку з локального сайту `mkdocs serve`
(<http://127.0.0.1:8000/shop-quality/api/>). API віддає `Access-Control-Allow-Origin: *`, тож запит між портами 8000 і 8080
дозволено. З GitHub Pages «Try it out» до `localhost` не працює: Chrome блокує запити публічного сайту до локальної
мережі (Local Network Access) — довідка там лише для читання.

<swagger-ui src="openapi.yaml"/>

## Правила контракту

- Лише читання: усі операції — `GET`; інший метод → `405` із заголовком `Allow: GET`.
- Усі помилки 4xx / 5xx — спільний об'єкт `Error` (`error` обов'язкове, `orderId` — лише для 404).
- Суми — рядок з двома знаками після крапки (`"31573.50"`), щоб не втрачати точність `BigDecimal`.
- Зміна контракту = нова версія в `info.version` за SemVer + запис у [Історії змін](../changelog.md).

## Пов'язані документи

[[reference]] · [[sdd]] · [[0003-embedded-jdk-http-server|ADR-0003]] · [[isd]]
