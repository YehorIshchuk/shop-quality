---
title: Стандарти коду
owner: Yehor Ishchuk
status: published
review_date: 2027-02-19
tags: [developer]
---

# Стандарти коду

Стандарти виведено з ЛР1 (SonarQube) і ЛР8 (ООП-аудит). Кожне правило перевіряється автоматично —
порушення зупиняє конвеєр, а не залишається на совісті автора.

## 1. Quality Gate «Shop-Quality-Gate» (ЛР1)

| Умова | Поріг | Чому |
|---|---|---|
| New Bugs | 0 | новий код не вносить дефектів надійності |
| New Security Hotspots Reviewed | 100 % | кожне потенційно небезпечне місце переглянуто людиною |
| Duplicated Lines | ≤ 3 % | дубль бізнес-правила = розбіжність при зміні в одному місці |
| Maintainability Rating | A | технічний борг ≤ 5 % вартості розробки |

Перевірка: `./scripts/scan.sh` (падає, якщо гейт червоний). Налаштування відтворює `./scripts/setup-quality-gate.sh`.

## 2. Складність методів

- **Cognitive Complexity ≤ 15** на метод (правило `java:S3776`). Інструменти: Guard Clauses, Extract Method
  (ЛР1: `placeOrder` 49 → 5).
- Плаский `switch` дозволено; вкладені `if` глибше 2 рівнів — ні.

## 3. Архітектурні правила (ЛР8, [[0001-hexagonal-ports-and-adapters|ADR-0001]])

| Метрика | Поріг | Гейт |
|---|---|---|
| CBO (зв'язність класу) | ≤ 8 | `scripts/oo-gate-god-object.sh check` |
| LCOM (Henderson-Sellers) | ≤ 0.2 | там само |
| DIT (глибина успадкування) | ≤ 1 | `scripts/oo-gate-inheritance.sh check` |
| NMO (перевизначені методи) | 0 | там само |

- Сервіси залежать **лише від інтерфейсів-портів** (`application/port/`); конкретні адаптери створює тільки `ShopApplication`.
- Композиція замість успадкування (`ReportTable` — `final`).

## 4. Конвенції Java

- Гроші — `BigDecimal`, округлення `HALF_UP` до 0.01 через `MoneyUtils`; ніяких `double` для сум.
- Класи-утиліти і листові класи — `final`, приватний конструктор у утиліт.
- Не використовувати `switch`-вирази, `record` і text blocks: `tools/oo_metrics.py` (javalang) розбирає синтаксис лише до Java 8.
- Кожен use case покривається тестом у `src/test/java` (JUnit 5); тести без мережі, порт HTTP — `0`.

## 5. Коміти

[Conventional Commits](https://www.conventionalcommits.org/): `type(scope): subject` —
`feat`, `fix`, `refactor`, `test`, `docs`, `ci`, `chore`. Приклади з історії: `docs(architecture): …`, `chore(scripts): …`.

## Пов'язані документи

[[CONTRIBUTING]] · [[sdd]] · [[0001-hexagonal-ports-and-adapters|ADR-0001]]
