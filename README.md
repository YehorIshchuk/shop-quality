# Shop Quality

**Що це і для кого.** Ядро інтернет-магазину на Java 17: каталог і склад, оформлення, оплата, відвантаження
та скасування замовлень, знижки для постійних клієнтів (SILVER / GOLD / PLATINUM), безкоштовна доставка від 2000 грн,
місячні звіти продажів і аналітика клієнта. Платіжний провайдер і пошта емулюються, дані живуть у пам'яті.

Проєкт — навчальний об'єкт інженерії якості: **ЛР1** (SonarQube, Quality Gate), **ЛР8** (CK / MOOD,
God Object, Inheritance Hell), **ЛР10** (аудит і супровід документації, архітектурний паспорт).

## Швидкий старт (≈ 5 хв, без SonarQube)

Потрібно: JDK 17+, Maven 3.9+, Git.

```bash
git clone https://github.com/YehorIshchuk/shop-quality.git && cd shop-quality
mvn -B clean verify                                    # збірка + 22 тести + покриття JaCoCo
java -cp target/classes com.kira.shop.ShopApplication  # демо-сценарій
```

Очікуваний результат: у лог виводиться лист про замовлення `ORD-1` на 31573.50 UAH (GOLD-клієнт, знижка),
підтвердження оплати `TX-…`, місячний звіт `*** MONTHLY SALES … ***` і резюме клієнта `Recommendation: keep GOLD`.
Програма завершується сама з кодом 0.

## Онбординг нового розробника

1. **Цей README**: що робить система і як її запустити (вище).
2. **[docs/sdd.md](docs/sdd.md)**: як система влаштована. C4-діаграми, компоненти, модель даних, контракти портів.
3. **[docs/isd.md](docs/isd.md)**: де вона працює. Середовища, змінні оточення, конвеєр якості, RTO/RPO.
4. **[docs/architecture/adr/](docs/architecture/adr/)**: чому рішення саме такі.
5. Перша зміна: тест у `src/test/java` → код → `mvn verify` → оновлення документів за правилом
   «зміна коду = зміна документації» ([docs/maintenance.md](docs/maintenance.md)) → `python3 tools/doc_audit.py --gate`.

## Конвеєр якості (SonarQube, OO-гейти, doc-гейт)

```bash
# SonarQube 26.x із ZIP-дистрибутиву на JDK 21 (докладно: docs/isd.md, вузол D4)
export SONAR_JAVA_PATH=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java
~/sonarqube-26.x/bin/macosx-universal-64/sonar.sh start      # http://localhost:9000
export SONAR_TOKEN=squ_...                 # My Account -> Security -> Generate Token (не комітити!)
./scripts/setup-quality-gate.sh            # кастомний Quality Gate "Shop-Quality-Gate"
./scripts/scan.sh                          # build + tests + JaCoCo + SonarQube (падає на червоному гейті)

pip install -r tools/requirements.txt
./scripts/oo-audit.sh current              # CK / MOOD / Coupling Matrix -> reports/oo/current/
python3 tools/doc_audit.py --gate          # актуальність документації -> DOC CURRENCY GATE
```
Альтернатива для Linux/Windows із Docker: `docker compose up -d` ([docker-compose.yml](docker-compose.yml)).
Windows (PowerShell): ті самі скрипти з розширенням `.ps1`.

## Документація

| Документ | Тип | Для кого | Власник |
|---|---|---|---|
| README.md | продукт · експлуатаційна | новачок, бізнес | Yehor Ishchuk |
| [docs/sdd.md](docs/sdd.md) | продукт · архітектурна | розробники, QA | Yehor Ishchuk |
| [docs/isd.md](docs/isd.md) | продукт · експлуатаційна | DevOps | Yehor Ishchuk |
| [docs/architecture/adr/](docs/architecture/adr/) | продукт · архітектурна | розробники, архітектор | Yehor Ishchuk |
| [docs/maintenance.md](docs/maintenance.md) | процес · регламент | уся команда | Yehor Ishchuk |
| [docs/LAB1_REPORT.md](docs/LAB1_REPORT.md), [LAB8](docs/LAB8_REPORT.md) | процес (історичні звіти) | викладач | Yehor Ishchuk |
| [docs/DEMO.md](docs/DEMO.md) | процес · **deprecated** | відтворення станів ЛР1/ЛР8 | Yehor Ishchuk |

## Стани репозиторію (git tags)

| Тег | Стан |
|---|---|
| `lab1-baseline` | інтеграція SonarQube, базове сканування, PASS |
| `lab1-fail` | навмисні порушення (дублювання, 2 bugs, hotspot), FAIL |
| `lab1-pass` | виправлення через рефакторинг, PASS |
| `lab1-refactor` | зниження Cognitive Complexity (49 → 2) |
| `lab8-baseline` | OO-аудит, God Object `StoreManager` (CBO 19) |
| `lab8-godobject-fail` | Shotgun Surgery, не компілюється (навмисно) |
| `lab8-godobject-pass` | інтерфейсна декомпозиція: CBO ≤ 8, LCOM = 0 |
| `lab8-inheritance-baseline` | ієрархія звітів: DIT 3, NMO 6 |
| `lab8-inheritance-fail` | каскадний злам ієрархії, не компілюється (навмисно) |
| `lab8-inheritance-pass` | композиція замість успадкування: DIT 1, NMO 0 |

## Структура
```
src/main/java/com/kira/shop/
  domain/            Order, Product, Customer, ...        (Core Domain)
  application/       use-case сервіси та політики
  application/port/  інтерфейси-контракти
  infrastructure/    адаптери: сховища в пам'яті, платіжний шлюз, сповіщення
  report/            ReportTable, SalesReport, MonthlySalesReport
  dto/, util/        OrderDto, OrderMapper, CsvUtils, MoneyUtils
tools/               oo_metrics.py (CK/MOOD), sonar_api_report.py (Web API), doc_audit.py (актуальність документації)
scripts/             scan, setup-quality-gate, quality-gate-status, sonar-api-measures, oo-audit, oo-gate-*
reports/oo/          закомічені результати OO-аудиту для кожного етапу
reports/docs/        закомічені результати аудиту документації (ЛР10)
docs/                архітектурний паспорт (SDD, ISD, ADR), регламент, звіти
```
