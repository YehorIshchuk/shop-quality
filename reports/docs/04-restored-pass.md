# Documentation Decay Audit — `restored-pass`

- Дата аудиту: 2026-11-21  
- HEAD: `c3fbea7 2026-11-21 docs(maintenance): restore documentation currency, PASS`  
- Пороги: ≤90 дн OK · ≤180 дн SUSPICIOUS · >180 дн STALE; Behind > 0 — документ відстає від коду; >20 — STRUCTURAL DRIFT

## 1. Last Updated Age (документи)

| Документ | Останній коміт | Вік, дн | Комітів документа | Статус |
|---|---|---|---|---|
| `README.md` | 2026-11-21 | 0 | 5 | OK |
| `docs/sdd.md` | 2026-11-21 | 0 | 3 | OK |
| `docs/isd.md` | 2026-11-21 | 0 | 3 | OK |
| `docs/architecture/adr/0001-hexagonal-ports-and-adapters.md` | 2026-11-21 | 0 | 1 | OK |
| `docs/architecture/adr/0002-in-memory-storage-with-csv-export.md` | 2026-11-21 | 0 | 1 | OK |
| `docs/architecture/adr/0003-embedded-jdk-http-server.md` | 2026-11-21 | 0 | 1 | OK |
| `docs/maintenance.md` | 2026-11-21 | 0 | 2 | OK |
| `docs/DEMO.md` | 2026-11-21 | 0 | 3 | OK |
| `docs/LAB1_REPORT.md` | 2026-11-21 | 0 | 2 | OK |
| `docs/LAB8_REPORT.md` | 2026-11-21 | 0 | 2 | OK |
| `docs/lab8/architecture-blueprint.md` | 2026-11-21 | 0 | 2 | OK |
| `docs/lab8/shotgun-surgery-build.log` | 2026-11-15 | 6 | 1 | OK |
| `docs/lab8/inheritance-crash-build.log` | 2026-11-15 | 6 | 1 | OK |
| `docker-compose.yml` | 2026-11-21 | 0 | 2 | OK |
| `sonar-project.properties` | 2026-11-21 | — | 2 | DELETED |
| `pom.xml` | 2026-11-15 | 6 | 1 | OK |
| `tools/requirements.txt` | 2026-11-15 | 6 | 1 | OK |

## 2. Code-Doc Change Ratio (компоненти)

| Компонент | Код | Документ | Комітів коду : документа | Behind (комітів коду після оновлення документа) | Вердикт |
|---|---|---|---|---|---|
| Whole codebase (structure) | `src/main/java` | `docs/sdd.md` | 12:3 | 0 | OK |
| Domain | `src/main/java/com/kira/shop/domain` | `docs/sdd.md` | 1:3 | 0 | OK |
| Application services | `src/main/java/com/kira/shop/application` | `docs/sdd.md` | 2:3 | 0 | OK |
| Infrastructure adapters | `src/main/java/com/kira/shop/infrastructure` | `docs/sdd.md` | 5:3 | 0 | OK |
| Reports | `src/main/java/com/kira/shop/report` | `docs/sdd.md` | 3:3 | 0 | OK |
| HTTP API | `src/main/java/com/kira/shop/api` | `docs/sdd.md` | 1:3 | 0 | OK |
| Composition root → user behaviour | `src/main/java/com/kira/shop/ShopApplication.java` | `README.md` | 3:5 | 0 | OK |
| Composition root → deployment | `src/main/java/com/kira/shop/ShopApplication.java` | `docs/isd.md` | 3:3 | 0 | OK |
| Build config | `pom.xml` | `docs/isd.md` | 1:3 | 0 | OK |
| Pipeline scripts | `scripts` | `docs/isd.md` | 6:3 | 0 | OK |
| Audit tools | `tools` | `docs/isd.md` | 4:3 | 0 | OK |

## 3. Structure drift (код ↔ текст документів)

Перевірок: 22, провалено: 0

- ✅ пакет `api/` описано в SDD (`docs/sdd.md`)
- ✅ пакет `application/` описано в SDD (`docs/sdd.md`)
- ✅ пакет `application/port/` описано в SDD (`docs/sdd.md`)
- ✅ пакет `domain/` описано в SDD (`docs/sdd.md`)
- ✅ пакет `dto/` описано в SDD (`docs/sdd.md`)
- ✅ пакет `infrastructure/` описано в SDD (`docs/sdd.md`)
- ✅ пакет `report/` описано в SDD (`docs/sdd.md`)
- ✅ пакет `util/` описано в SDD (`docs/sdd.md`)
- ✅ компонент `ShopHttpApi` описано в SDD (`docs/sdd.md`)
- ✅ компонент `ConsoleNotificationSender` описано в SDD (`docs/sdd.md`)
- ✅ компонент `FakePaymentGateway` описано в SDD (`docs/sdd.md`)
- ✅ компонент `FileOrderRepository` описано в SDD (`docs/sdd.md`)
- ✅ компонент `InventoryStore` описано в SDD (`docs/sdd.md`)
- ✅ компонент `ProductRepository` описано в SDD (`docs/sdd.md`)
- ✅ HTTP-маршрут `/api/health` описано в SDD (`docs/sdd.md`)
- ✅ HTTP-маршрут `/api/health` описано в README (`README.md`)
- ✅ HTTP-маршрут `/api/orders` описано в SDD (`docs/sdd.md`)
- ✅ HTTP-маршрут `/api/orders` описано в README (`README.md`)
- ✅ змінну середовища `GATE_CLASSES` описано в ISD (`docs/isd.md`)
- ✅ змінну середовища `SHOP_HTTP_PORT` описано в ISD (`docs/isd.md`)
- ✅ змінну середовища `SONAR_HOST_URL` описано в ISD (`docs/isd.md`)
- ✅ змінну середовища `SONAR_TOKEN` описано в ISD (`docs/isd.md`)

## 4. Підсумок

- Документів без актуальної версії: 0
- Компонентів, де документ відстає від коду: 0
- Провалених структурних перевірок: 0

**DOC CURRENCY GATE: PASS**

## 5. Лог команд Git CLI

```text
$ git log -1 --format=%cs -- README.md
2026-11-21
$ git log --oneline -- README.md | wc -l
5
$ git log -1 --format=%cs -- docs/sdd.md
2026-11-21
$ git log --oneline -- docs/sdd.md | wc -l
3
$ git log -1 --format=%cs -- docs/isd.md
2026-11-21
$ git log --oneline -- docs/isd.md | wc -l
3
$ git log -1 --format=%cs -- docs/architecture/adr/0001-hexagonal-ports-and-adapters.md
2026-11-21
$ git log --oneline -- docs/architecture/adr/0001-hexagonal-ports-and-adapters.md | wc -l
1
$ git log -1 --format=%cs -- docs/architecture/adr/0002-in-memory-storage-with-csv-export.md
2026-11-21
$ git log --oneline -- docs/architecture/adr/0002-in-memory-storage-with-csv-export.md | wc -l
1
$ git log -1 --format=%cs -- docs/architecture/adr/0003-embedded-jdk-http-server.md
2026-11-21
$ git log --oneline -- docs/architecture/adr/0003-embedded-jdk-http-server.md | wc -l
1
$ git log -1 --format=%cs -- docs/maintenance.md
2026-11-21
$ git log --oneline -- docs/maintenance.md | wc -l
2
$ git log -1 --format=%cs -- docs/DEMO.md
2026-11-21
$ git log --oneline -- docs/DEMO.md | wc -l
3
$ git log -1 --format=%cs -- docs/LAB1_REPORT.md
2026-11-21
$ git log --oneline -- docs/LAB1_REPORT.md | wc -l
2
$ git log -1 --format=%cs -- docs/LAB8_REPORT.md
2026-11-21
$ git log --oneline -- docs/LAB8_REPORT.md | wc -l
2
$ git log -1 --format=%cs -- docs/lab8/architecture-blueprint.md
2026-11-21
$ git log --oneline -- docs/lab8/architecture-blueprint.md | wc -l
2
$ git log -1 --format=%cs -- docs/lab8/shotgun-surgery-build.log
2026-11-15
$ git log --oneline -- docs/lab8/shotgun-surgery-build.log | wc -l
1
$ git log -1 --format=%cs -- docs/lab8/inheritance-crash-build.log
2026-11-15
$ git log --oneline -- docs/lab8/inheritance-crash-build.log | wc -l
1
$ git log -1 --format=%cs -- docker-compose.yml
2026-11-21
$ git log --oneline -- docker-compose.yml | wc -l
2
$ git log -1 --format=%cs -- sonar-project.properties
2026-11-21
$ git log --oneline -- sonar-project.properties | wc -l
2
$ git log -1 --format=%cs -- pom.xml
2026-11-15
$ git log --oneline -- pom.xml | wc -l
1
$ git log -1 --format=%cs -- tools/requirements.txt
2026-11-15
$ git log --oneline -- tools/requirements.txt | wc -l
1
$ git log --oneline -- src/main/java | wc -l
12
$ git rev-list --count c3fbea7..HEAD -- src/main/java
0
$ git log --oneline -- src/main/java/com/kira/shop/domain | wc -l
1
$ git rev-list --count c3fbea7..HEAD -- src/main/java/com/kira/shop/domain
0
$ git log --oneline -- src/main/java/com/kira/shop/application | wc -l
2
$ git rev-list --count c3fbea7..HEAD -- src/main/java/com/kira/shop/application
0
$ git log --oneline -- src/main/java/com/kira/shop/infrastructure | wc -l
5
$ git rev-list --count c3fbea7..HEAD -- src/main/java/com/kira/shop/infrastructure
0
$ git log --oneline -- src/main/java/com/kira/shop/report | wc -l
3
$ git rev-list --count c3fbea7..HEAD -- src/main/java/com/kira/shop/report
0
$ git log --oneline -- src/main/java/com/kira/shop/api | wc -l
1
$ git rev-list --count c3fbea7..HEAD -- src/main/java/com/kira/shop/api
0
$ git log --oneline -- src/main/java/com/kira/shop/ShopApplication.java | wc -l
3
$ git rev-list --count c3fbea7..HEAD -- src/main/java/com/kira/shop/ShopApplication.java
0
$ git log --oneline -- src/main/java/com/kira/shop/ShopApplication.java | wc -l
3
$ git rev-list --count c3fbea7..HEAD -- src/main/java/com/kira/shop/ShopApplication.java
0
$ git log --oneline -- pom.xml | wc -l
1
$ git rev-list --count c3fbea7..HEAD -- pom.xml
0
$ git log --oneline -- scripts | wc -l
6
$ git rev-list --count c3fbea7..HEAD -- scripts
0
$ git log --oneline -- tools | wc -l
4
$ git rev-list --count c3fbea7..HEAD -- tools
0
```
