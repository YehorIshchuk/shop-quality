---
title: "ADR-0001: Порти та адаптери"
owner: Yehor Ishchuk
status: published
review_date: 2027-05-20
adr: "0001"
decision: Accepted
tags: [architecture]
---

# ADR-0001: Шарова архітектура «порти та адаптери» з ручним DI замість God Object

- **Номер:** 0001
- **Статус:** Accepted
- **Дата рішення:** 2026-11-15 (коміт `2afe305`, тег `lab8-godobject-pass`)
- **Автор / власник:** Yehor Ishchuk
- **Зачеплені документи:** [SDD §3–§6](../sdd.md), [LAB8_REPORT](../../reports/LAB8_REPORT.md) (історичний контекст)

## Контекст

Уся бізнес-логіка жила в одному класі `service/StoreManager` (29 методів, WMC 67, CBO 19, LCOM-HS 0.855).
Контрольована зміна сигнатури `InventoryStore` (тег `lab8-godobject-fail`) зламала компіляцію у 7 місцях
`StoreManager` (Shotgun Surgery). Будь-яка зміна сховища, оплати чи сповіщень зачіпала один клас,
тестувати сценарії окремо було неможливо.

## Розглянуті альтернативи

| # | Варіант | Плюси | Мінуси |
|---|---|---|---|
| A | Залишити `StoreManager`, розбити на приватні методи | мінімум змін | CBO і LCOM не змінюються; Shotgun Surgery лишається |
| B | Spring Boot + анотаційний DI (`@Service`, `@Autowired`) | стандарт індустрії, готовий контейнер | важка runtime-залежність для навчального ядра; DI «магічний», метрики CBO рахуються гірше; не потрібен вебсервер |
| C | **Use-case сервіси + інтерфейси-порти + ручний DI у Composition Root** | залежності на абстракції (DIP), кожен клас CBO ≤ 8, тести з фейками без фреймворку | більше файлів (11 сервісів, 9 портів-інтерфейсів + `PaymentResult`); зв'язування вручну в `ShopApplication` |

## Рішення

Обрано **C**. Пакети `domain/` → `application/` → `application/port/` ← `infrastructure/`.
Сервіси отримують лише інтерфейси портів через конструктор; конкретні адаптери створюються тільки в
`ShopApplication` (Composition Root). Межі контролюються гейтом `scripts/oo-gate-god-object.sh`.

## Наслідки

- ✅ CBO сервісів ≤ 8, LCOM-HS = 0; та сама зміна `InventoryStore` тепер зачіпає 1 адаптер і 1 порт.
- ✅ Сценарії тестуються ізольовано (`ShopServicesTest` з `ShopFixture`).
- ⚠️ Новий адаптер (наприклад, справжня БД або HTTP API) обов'язково підключається в `ShopApplication` і описується в SDD §4.3.
- ⚠️ Якщо з'явиться потреба у веб-контейнері, варіант B переглядається **новим ADR**, цей отримає статус `Superseded`.

## Пов'язані документи

[[sdd]] · [[coding-standards]] · [[0002-in-memory-storage-with-csv-export|ADR-0002]] · [[0003-embedded-jdk-http-server|ADR-0003]]
