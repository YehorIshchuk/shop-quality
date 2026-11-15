# Shop Quality

Ядро інтернет-магазину (Java 17, Maven): каталог, склад, замовлення, оплата, знижки, доставка, звіти.
Проєкт для лабораторних робіт з контролю якості: **ЛР1** (SonarQube, Quality Gate) та **ЛР8** (CK / MOOD, God Object, Inheritance Hell).

## Швидкий старт

```bash
docker compose up -d                       # SonarQube на http://localhost:9000
export SONAR_TOKEN=squ_...                 # My Account -> Security -> Generate Token
./scripts/setup-quality-gate.sh            # кастомний Quality Gate "Shop-Quality-Gate"
./scripts/scan.sh                          # build + tests + JaCoCo + SonarQube (fails on red gate)

pip install -r tools/requirements.txt
./scripts/oo-audit.sh current              # CK / MOOD / Coupling Matrix -> reports/oo/current/
```
Windows (PowerShell): ті самі скрипти з розширенням `.ps1`.

## Документація
- [docs/DEMO.md](docs/DEMO.md) — покрокове відтворення всіх станів у SonarQube і сценарій захисту
- [docs/LAB1_REPORT.md](docs/LAB1_REPORT.md) — звіт ЛР1
- [docs/LAB8_REPORT.md](docs/LAB8_REPORT.md) — звіт ЛР8, [архітектурний blueprint](docs/lab8/architecture-blueprint.md)

## Стани репозиторію (git tags)

| Тег | Стан |
|---|---|
| `lab1-baseline` | інтеграція SonarQube, базове сканування — PASS |
| `lab1-fail` | навмисні порушення (дублювання, 2 bugs, hotspot) — FAIL |
| `lab1-pass` | виправлення через рефакторинг — PASS |
| `lab1-refactor` | зниження Cognitive Complexity (49 → 2) |
| `lab8-baseline` | OO-аудит, God Object `StoreManager` (CBO 19) |
| `lab8-godobject-fail` | Shotgun Surgery — не компілюється (навмисно) |
| `lab8-godobject-pass` | інтерфейсна декомпозиція — CBO ≤ 8, LCOM = 0 |
| `lab8-inheritance-baseline` | ієрархія звітів DIT 3, NMO 6 |
| `lab8-inheritance-fail` | каскадний злам ієрархії — не компілюється (навмисно) |
| `lab8-inheritance-pass` | композиція замість успадкування — DIT 1, NMO 0 |

## Структура
```
src/main/java/com/kira/shop/
  domain/            Order, Product, Customer, ...        (Core Domain)
  application/       use-case сервіси та політики
  application/port/  інтерфейси-контракти
  infrastructure/    адаптери: репозиторії, платіжний шлюз, сповіщення
  report/            ReportTable, SalesReport, MonthlySalesReport
  dto/, util/        OrderDto, OrderMapper, CsvUtils, MoneyUtils
tools/               oo_metrics.py (CK/MOOD), sonar_api_report.py (Web API)
scripts/             scan, setup-quality-gate, quality-gate-status, sonar-api-measures, oo-audit, oo-gate-*
reports/oo/          закомічені результати OO-аудиту для кожного етапу
```
