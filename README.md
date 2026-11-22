# Shop Quality

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

Сайт: збирається з `docs/` командою `mkdocs serve` (публікація на GitHub Pages — ЛР11, Завдання 3).

| Рівень | Питання | Де |
|---|---|---|
| 1 · README | Що це і як запустити? | цей файл |
| 2 · Developer Docs | Як приєднатися до розробки? | [docs/developer/](docs/developer/index.md) — [CONTRIBUTING](docs/developer/CONTRIBUTING.md), [локальне середовище](docs/developer/local-setup.md), [стандарти коду](docs/developer/coding-standards.md) |
| 3 · API Docs | Як інтегруватися? | [docs/api/](docs/api/index.md) |
| 4 · Architecture Docs | Чому система влаштована саме так? | [docs/architecture/](docs/architecture/index.md) — [SDD](docs/architecture/sdd.md), [ISD](docs/architecture/isd.md), [ADR](docs/architecture/adr/index.md) |

Історія змін — [CHANGELOG.md](https://github.com/YehorIshchuk/shop-quality/blob/main/CHANGELOG.md).
