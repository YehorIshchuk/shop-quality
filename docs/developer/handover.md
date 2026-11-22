---
title: Handover-пакет
owner: Yehor Ishchuk
status: published
review_date: 2027-02-19
tags: [process, developer]
type: process / handover
audience: команда супроводу
last_reviewed: 2026-11-21
---

# Пакет передачі знань (Handover Package)

Отримувач: умовна команда супроводу. Стан системи на передачу: тег `lab10-docs-pass`,
`DOC CURRENCY GATE: PASS` ([reports/docs/04-restored-pass.md](https://github.com/YehorIshchuk/shop-quality/blob/main/reports/docs/04-restored-pass.md)).

## 1. Реєстр документів паспорта

| Документ | Що відповідає | Власник | Цикл | Останній перегляд | Статус актуальності |
|---|---|---|---|---|---|
| [README.md](../index.md) | що робить система, запуск, HTTP API | Yehor Ishchuk | 90 дн | 2026-11-21 | ✅ актуальний (Behind 0) |
| [sdd.md](../architecture/sdd.md) | C4, компоненти, модель даних, контракти | Yehor Ishchuk | 180 дн | 2026-11-21 | ✅ актуальний (22/22 перевірки) |
| [isd.md](../architecture/isd.md) | середовища, змінні, конвеєр, RTO/RPO | Yehor Ishchuk | 90 дн | 2026-11-21 | ✅ актуальний (Behind 0) |
| [ADR-0001…0003](../architecture/adr/index.md) | чому так: шари, сховище, HTTP-сервер | Yehor Ishchuk | 180 дн | 2026-11-21 | ✅ Accepted, без Superseded |
| [maintenance.md](maintenance.md) | власники, цикли, правило «код = документація» | Yehor Ishchuk | 90 дн | 2026-11-21 | ✅ актуальний |
| [doc-map.json](https://github.com/YehorIshchuk/shop-quality/blob/main/tools/doc-map.json) + `tools/doc_audit.py` | автоматична перевірка актуальності | Yehor Ishchuk | з кожним комітом | 2026-11-21 | ✅ PASS |
| [DEMO.md](../reports/DEMO.md), [lab8/architecture-blueprint.md](../architecture/blueprint-lab8.md) | відтворення станів ЛР1/ЛР8 | Yehor Ishchuk | — | 2026-11-21 | ⚠️ deprecated |
| `docs/reports/LAB*_REPORT.md`, `docs/reports/lab8/*.log`, `reports/**` | історія процесу | — | не переглядаються | — | 🗄️ архів |

## 2. Онбординг нового розробника (від README до локального запуску)

1. **Зрозуміти, що це.** Прочитати перший абзац [README](../index.md) (≈ 2 хв).
2. **Підготувати станцію.** JDK 17+, Maven 3.9+, Git; `git clone https://github.com/YehorIshchuk/shop-quality.git`.
3. **Зібрати і перевірити.** `mvn -B clean verify` → `Tests run: 23, Failures: 0`.
4. **Запустити.** `java -cp target/classes com.kira.shop.ShopApplication` → в іншому терміналі
   `curl -s localhost:8080/api/health` → `{"status":"UP"}`, `curl -s localhost:8080/api/orders`.
5. **Зорієнтуватися і зробити першу зміну.** C4 у [SDD §3](../architecture/sdd.md) → свій компонент у §4 → контракти §6 →
   зміна + тест → оновити документи за [maintenance.md](maintenance.md) → `python3 tools/doc_audit.py --gate` → PASS.

Цільовий час від клонування до працюючого API: ≤ 15 хв (без SonarQube). SonarQube потрібен лише для конвеєра якості ([ISD D4](../architecture/isd.md#d4)).

## 3. Декларація Bus Factor

Метод: `git shortlog -sn HEAD -- <компонент>` (лог — у [звіті ЛР10](../reports/LAB10_REPORT.md)). Для **кожного** компонента
єдиний автор — Yehor Ishchuk, тобто **Bus Factor системи = 1**. Ризик втрати знань оцінено як
*критичність компонента × повнота документації*.

| Компонент | Хто розуміє | Що знає лише автор («темна зона») | Документовано зараз | Ризик |
|---|---|---|---|---|
| `tools/oo_metrics.py` (683 рядки) | 1 | формули LCOM-HS / LCOM4 / MOOD, виключення accessors, обмеження javalang (лише синтаксис ≤ Java 8) | docstring + коментарі в коді | 🔴 високий |
| Розгортання SonarQube на macOS (D4) | 1 | Java 21 через `SONAR_JAVA_PATH`, ≥ 23 ГБ вільного місця, Full Disk Access, автопереклад Chrome ламає сторінку зміни пароля | ISD D4 (частково) | 🟠 середній |
| Quality Gate (`setup-quality-gate.sh`) | 1 | fallback `new_bugs` → `new_reliability_rating` у режимі MQR | коментарі скрипта + ISD D3 | 🟠 середній |
| Бізнес-правила `StockAllocator`, `PricingService` | 1 | див. §4 — до цього пакета були лише в коді | handover §4 + тести | 🟡 нижче середнього |
| `ShopHttpApi` (новий) | 1 | ручний JSON, порт 0 у тестах | SDD §4.4, §6.2, ADR-0003, ISD D1 | 🟢 низький |
| Домен, сервіси, адаптери, звіти | 1 | — | SDD §4–§6, ADR-0001/0002, тести | 🟢 низький |
| Навмисно некомпільовані теги `lab8-*-fail` | 1 | на них не можна відкочуватися | README, ISD §2 | 🟢 низький |

### План усунення single points of knowledge

| # | Захід | Компонент | Термін | Критерій виконання |
|---|---|---|---|---|
| 1 | **Парне володіння:** призначити другого власника (напарник із групи) у `maintenance.md`; разом виконати онбординг §2 і один FAIL → PASS цикл | усі | до ЛР11 | у таблиці регламенту 2 власники; напарник самостійно отримав `DOC CURRENCY GATE: PASS` |
| 2 | Документувати темну зону: `docs/oo-metrics.md` з формулами, прикладом розрахунку для одного класу та обмеженнями javalang | `tools/oo_metrics.py` | до ЛР11 | новий учасник відтворює CBO/LCOM `StoreManager` з `lab8-baseline` за документом |
| 3 | Додати в ISD D4 розділ «Відомі проблеми» (усі пастки встановлення) | SonarQube D4 | 1 тиждень | чисте встановлення з нуля за ISD без сторонньої допомоги |
| 4 | Перенести doc-гейт у хмарний CI (GitHub Actions) — правило перестане залежати від дисципліни однієї людини | конвеєр D3 | ЛР11 (Docs as Code) | PR без оновлення документів отримує червоний статус |
| 5 | Ревізія Bus Factor на кожному плановому перегляді регламенту | усі | кожні 90 днів | у кожного компонента з ризиком 🔴/🟠 ≥ 2 людини, що розуміють |

## 4. Темні зони, винесені з голови автора

**Резервування складу (`StockAllocator.allocate`).** Якщо на складі вистачає — видається запитана кількість.
Якщо не вистачає: REGULAR/SILVER — відмова `Not enough stock`; GOLD/PLATINUM — видається **залишок** (часткове
виконання), при нульовому залишку — `Out of stock`. Кількість ≤ 0 або `null` — `IllegalArgumentException`.

**Ціноутворення (`PricingService`).** Знижка = % рівня (SILVER 3, GOLD 5, PLATINUM 10) + 2 %, якщо в замовленні є книги
і загалом ≥ 3 одиниць; рахується від subtotal з округленням HALF_UP. Доставка: по Україні безкоштовно від 2000 грн, інакше
70 грн + 10 грн/кг; за кордон 450 грн + 120 грн/кг; вага округлюється вгору до цілого кг.

**Оплата (`FakePaymentGateway`).** Транзакції понад 50 000 грн відхиляються (сценарій відмови для тестів);
id транзакцій `TX-1001…`, повернення приймається лише для id з префіксом `TX-`.

## 5. Фінальний вердикт: «Always Ready to Handover»?

**Так, з одним задекларованим ризиком.**

- ✅ Документація: паспорт повний (SDD, ISD, 3 ADR, регламент), матриця аудиторій без прогалин,
  `DOC CURRENCY GATE: PASS`; новий розробник доходить до працюючого API за 5 кроків.
- ✅ Механізм: правило «зміна коду = зміна документації» перевіряється автоматично, а краш-тест показав, що дрейф
  ловиться одразу (FAIL на першому ж коміті).
- ⚠️ Організаційно: Bus Factor = 1. Знання передаване через документи, але поки немає другої людини, яка пройшла цикл
  самостійно. Стан стане беззастережним «Always Ready to Handover» після заходів 1–2 плану.

## Пов'язані документи

[[local-setup]] · [[maintenance]] · [[sdd]] · [[isd]] · [[CONTRIBUTING]]
