# Documentation Decay Audit — `baseline`

- Дата аудиту: 2026-11-21  
- HEAD: `b2b3ba1 2026-11-15 chore: ignore ad-hoc audit outputs`  
- Пороги: ≤90 дн OK · ≤180 дн SUSPICIOUS · >180 дн STALE; Behind > 0 — документ відстає від коду; >20 — STRUCTURAL DRIFT

## 1. Last Updated Age (документи)

| Документ | Останній коміт | Вік, дн | Комітів документа | Статус |
|---|---|---|---|---|
| `README.md` | 2026-11-15 | 6 | 2 | OK |
| `docs/DEMO.md` | 2026-11-15 | 6 | 2 | OK |
| `docs/LAB1_REPORT.md` | 2026-11-15 | 6 | 1 | OK |
| `docs/LAB8_REPORT.md` | 2026-11-15 | 6 | 1 | OK |
| `docs/lab8/architecture-blueprint.md` | 2026-11-15 | 6 | 1 | OK |
| `docs/lab8/shotgun-surgery-build.log` | 2026-11-15 | 6 | 1 | OK |
| `docs/lab8/inheritance-crash-build.log` | 2026-11-15 | 6 | 1 | OK |
| `docker-compose.yml` | 2026-11-15 | 6 | 1 | OK |
| `sonar-project.properties` | 2026-11-15 | 6 | 1 | OK |
| `pom.xml` | 2026-11-15 | 6 | 1 | OK |
| `tools/requirements.txt` | 2026-11-15 | 6 | 1 | OK |
| `docs/sdd.md` | — | — | 0 | MISSING |
| `docs/isd.md` | — | — | 0 | MISSING |
| `docs/architecture/adr` | — | — | 0 | MISSING |

## 2. Code-Doc Change Ratio (компоненти)

| Компонент | Код | Документ | Комітів коду : документа | Behind (комітів коду після оновлення документа) | Вердикт |
|---|---|---|---|---|---|
| Domain | `src/main/java/com/kira/shop/domain` | `README.md` | 1:2 | 0 | OK |
| Application services | `src/main/java/com/kira/shop/application` | `README.md` | 2:2 | 0 | OK |
| Infrastructure adapters | `src/main/java/com/kira/shop/infrastructure` | `README.md` | 4:2 | 0 | OK |
| Reports | `src/main/java/com/kira/shop/report` | `README.md` | 3:2 | 0 | OK |
| Build & pipeline scripts | `scripts` | `README.md` | 6:2 | 1 | BEHIND |
| Audit tools | `tools` | `README.md` | 2:2 | 0 | OK |

## 3. Structure drift (код ↔ текст документів)

Перевірок: 15, провалено: 15

- ❌ пакет `application/` описано в SDD (`docs/sdd.md`)
- ❌ пакет `application/port/` описано в SDD (`docs/sdd.md`)
- ❌ пакет `domain/` описано в SDD (`docs/sdd.md`)
- ❌ пакет `dto/` описано в SDD (`docs/sdd.md`)
- ❌ пакет `infrastructure/` описано в SDD (`docs/sdd.md`)
- ❌ пакет `report/` описано в SDD (`docs/sdd.md`)
- ❌ пакет `util/` описано в SDD (`docs/sdd.md`)
- ❌ компонент `ConsoleNotificationSender` описано в SDD (`docs/sdd.md`)
- ❌ компонент `FakePaymentGateway` описано в SDD (`docs/sdd.md`)
- ❌ компонент `FileOrderRepository` описано в SDD (`docs/sdd.md`)
- ❌ компонент `InventoryStore` описано в SDD (`docs/sdd.md`)
- ❌ компонент `ProductRepository` описано в SDD (`docs/sdd.md`)
- ❌ змінну середовища `GATE_CLASSES` описано в ISD (`docs/isd.md`)
- ❌ змінну середовища `SONAR_HOST_URL` описано в ISD (`docs/isd.md`)
- ❌ змінну середовища `SONAR_TOKEN` описано в ISD (`docs/isd.md`)

## 4. Підсумок

- Документів без актуальної версії: 3
- Компонентів, де документ відстає від коду: 1
- Провалених структурних перевірок: 15

**DOC CURRENCY GATE: FAIL**

## 5. Лог команд Git CLI

```text
$ git log -1 --format=%cs -- README.md
2026-11-15
$ git log --oneline -- README.md | wc -l
2
$ git log -1 --format=%cs -- docs/DEMO.md
2026-11-15
$ git log --oneline -- docs/DEMO.md | wc -l
2
$ git log -1 --format=%cs -- docs/LAB1_REPORT.md
2026-11-15
$ git log --oneline -- docs/LAB1_REPORT.md | wc -l
1
$ git log -1 --format=%cs -- docs/LAB8_REPORT.md
2026-11-15
$ git log --oneline -- docs/LAB8_REPORT.md | wc -l
1
$ git log -1 --format=%cs -- docs/lab8/architecture-blueprint.md
2026-11-15
$ git log --oneline -- docs/lab8/architecture-blueprint.md | wc -l
1
$ git log -1 --format=%cs -- docs/lab8/shotgun-surgery-build.log
2026-11-15
$ git log --oneline -- docs/lab8/shotgun-surgery-build.log | wc -l
1
$ git log -1 --format=%cs -- docs/lab8/inheritance-crash-build.log
2026-11-15
$ git log --oneline -- docs/lab8/inheritance-crash-build.log | wc -l
1
$ git log -1 --format=%cs -- docker-compose.yml
2026-11-15
$ git log --oneline -- docker-compose.yml | wc -l
1
$ git log -1 --format=%cs -- sonar-project.properties
2026-11-15
$ git log --oneline -- sonar-project.properties | wc -l
1
$ git log -1 --format=%cs -- pom.xml
2026-11-15
$ git log --oneline -- pom.xml | wc -l
1
$ git log -1 --format=%cs -- tools/requirements.txt
2026-11-15
$ git log --oneline -- tools/requirements.txt | wc -l
1
$ git log -1 --format=%cs -- docs/sdd.md
(порожньо)
$ git log --oneline -- docs/sdd.md | wc -l
0
$ git log -1 --format=%cs -- docs/isd.md
(порожньо)
$ git log --oneline -- docs/isd.md | wc -l
0
$ git log -1 --format=%cs -- docs/architecture/adr
(порожньо)
$ git log --oneline -- docs/architecture/adr | wc -l
0
$ git log --oneline -- src/main/java/com/kira/shop/domain | wc -l
1
$ git rev-list --count 0cee12c..HEAD -- src/main/java/com/kira/shop/domain
0
$ git log --oneline -- src/main/java/com/kira/shop/application | wc -l
2
$ git rev-list --count 0cee12c..HEAD -- src/main/java/com/kira/shop/application
0
$ git log --oneline -- src/main/java/com/kira/shop/infrastructure | wc -l
4
$ git rev-list --count 0cee12c..HEAD -- src/main/java/com/kira/shop/infrastructure
0
$ git log --oneline -- src/main/java/com/kira/shop/report | wc -l
3
$ git rev-list --count 0cee12c..HEAD -- src/main/java/com/kira/shop/report
0
$ git log --oneline -- scripts | wc -l
6
$ git rev-list --count 0cee12c..HEAD -- scripts
1
$ git log --oneline -- tools | wc -l
2
$ git rev-list --count 0cee12c..HEAD -- tools
0
```
