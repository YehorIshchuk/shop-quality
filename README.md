# Shop Quality

[![docs](https://github.com/YehorIshchuk/shop-quality/actions/workflows/docs.yml/badge.svg)](https://github.com/YehorIshchuk/shop-quality/actions/workflows/docs.yml)

**Що це і для кого.** Ядро інтернет-магазину на Java 17: каталог і склад, оформлення, оплата, відвантаження
та скасування замовлень, знижки для постійних клієнтів (SILVER / GOLD / PLATINUM), безкоштовна доставка від 2000 грн,
місячні звіти продажів, аналітика клієнта і **read-only HTTP API замовлень**. Платіжний провайдер і пошта
емулюються, дані живуть у пам'яті процесу.

Проєкт — навчальний об'єкт інженерії якості: ЛР1 (SonarQube), ЛР8 (ООП-аудит), ЛР10 (архітектурний паспорт),
ЛР11 (Documentation as Code).

## Швидкий старт (≈ 5 хв)

Потрібно: JDK 17+, Maven 3.9+, Git.

```bash
git clone https://github.com/YehorIshchuk/shop-quality.git && cd shop-quality
mvn -B clean verify                                    # збірка + тести
java -cp target/classes com.kira.shop.ShopApplication  # демо-сценарій + HTTP API на :8080, зупинка Ctrl+C
curl -s localhost:8080/api/health                      # {"status":"UP"}
curl -s localhost:8080/api/orders                      # замовлення у JSON
```

Інший порт: `SHOP_HTTP_PORT=9090 java -cp target/classes com.kira.shop.ShopApplication`.

## База знань

Сайт: **<https://yehorishchuk.github.io/shop-quality/>** — збирається з `docs/` конвеєром `docs.yml` при кожному push у `main`.

| Рівень | Питання | Де |
|---|---|---|
| 1 · README | Що це і як запустити? | цей файл |
| 2 · Developer Docs | Як приєднатися до розробки? | [docs/developer/](docs/developer/index.md) — [CONTRIBUTING](docs/developer/CONTRIBUTING.md), [локальне середовище](docs/developer/local-setup.md), [стандарти коду](docs/developer/coding-standards.md) |
| 3 · API Docs | Як інтегруватися? | [docs/api/](docs/api/index.md) — контракт [openapi.yaml](docs/api/openapi.yaml), Swagger UI, SDK |
| 4 · Architecture Docs | Чому система влаштована саме так? | [docs/architecture/](docs/architecture/index.md) — [SDD](docs/architecture/sdd.md), [ISD](docs/architecture/isd.md), [ADR](docs/architecture/adr/index.md) |

Історія змін — [CHANGELOG.md](https://github.com/YehorIshchuk/shop-quality/blob/main/CHANGELOG.md).

## Definition of Done для документації

Кожен pull request проходить автоматичні перевірки конвеєра `docs.yml`
(чек-лист «docs-or-don't-merge» — у [CONTRIBUTING](docs/developer/CONTRIBUTING.md#definition-of-done-docs-or-dont-merge)):

| Що перевіряється | Інструмент | Критерій |
|---|---|---|
| Контракт API | Spectral (`.spectral.yaml`) | 0 errors |
| Відповідність контракту живому коду | Schemathesis проти запущеного API | 0 failures |
| Архітектурні пороги ЛР8 | `scripts/oo-gate-*.sh` (job `oo-gates`, RFC-001) | OO GATE: PASS (CBO ≤ 8, LCOM ≤ 0.2, DIT ≤ 1, NMO = 0) |
| Розмітка Markdown | markdownlint (`.markdownlint.yaml`) | 0 errors |
| Посилання, навігація, синтаксис | `mkdocs build --strict` | Exit Code 0 |
| Актуальність | `review_date` із front matter (`tools/review_audit.py`) | прострочені сторінки — у звіті конвеєра |
| Зміна коду = зміна документації | `tools/doc_audit.py --gate` (ЛР10) | DOC CURRENCY GATE: PASS |
