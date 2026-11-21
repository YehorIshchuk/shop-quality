# Інструкція: розгортання, сканування станів і захист (ЛР1 + ЛР8)

> ⚠️ **DEPRECATED (ЛР10, 2026-11-21).** Інструкцію написано для ЛР1/ЛР8 під Docker; фактичне розгортання на
> робочій станції — SonarQube із ZIP на JDK 21. Актуальні джерела: [README](../README.md) (швидкий старт) і
> [ISD](isd.md) (середовища, вузол D4). Розділи 2–5 залишаються чинними як сценарій відтворення тегів ЛР1/ЛР8.

## 0. Що потрібно встановити

| Інструмент | Версія | Перевірка |
|---|---|---|
| JDK | 17+ | `java -version` |
| Maven | 3.9+ | `mvn -v` |
| Docker Desktop | будь-яка актуальна | `docker version` |
| Python | 3.9+ (для ЛР8) | `python --version`, далі `pip install -r tools/requirements.txt` |
| Git | — | `git --version` |

## 1. Запуск SonarQube і токен

```bash
docker compose up -d            # перший старт ~1-2 хв
```
1. Відкрити http://localhost:9000, увійти `admin / admin`, задати новий пароль.
2. **My Account → Security → Generate Token** (тип *Global Analysis Token* або *User Token*) — скопіювати `squ_...`.
3. Зберегти в змінну:
   - bash: `export SONAR_TOKEN=squ_...`
   - PowerShell: `$env:SONAR_TOKEN="squ_..."`

> Linux: якщо контейнер падає через Elasticsearch — `sudo sysctl -w vm.max_map_count=262144`.

## 2. ЛР1 — відтворення історії PASS → FAIL → PASS у SonarQube

Git-історія вже містить усі стани, позначені тегами. SonarQube має «побачити» їх по черзі, тому скануємо кожен тег послідовно (займає ~5 хв).

```bash
git checkout lab1-baseline
./scripts/setup-quality-gate.sh          # один раз: кастомний gate "Shop-Quality-Gate"
./scripts/scan.sh                        # 1) Baseline           -> PASS
git checkout lab1-fail     && ./scripts/scan.sh   # 2) Порушення  -> FAIL (збірка зупиняється — так і має бути)
git checkout lab1-pass     && ./scripts/scan.sh   # 3) Виправлення -> PASS
git checkout lab1-refactor && ./scripts/scan.sh   # 4) Рефакторинг складності -> PASS
git checkout main          && ./scripts/scan.sh   # 5) Фінальний стан після ЛР8 -> PASS
```
Windows: ті самі команди з `.\scripts\setup-quality-gate.ps1` та `.\scripts\scan.ps1`.

Після кожного кроку:
- зробити скріншот вкладки **Overview** (статус Quality Gate + метрики) та, для FAIL, блоку з порушеними умовами;
- зняти цифри для порівняльної таблиці: `./scripts/quality-gate-status.sh` (або `.ps1`).

Цифри внести в `docs/LAB1_REPORT.md` (поля `⟨…⟩`).

> **Чому FAIL спрацює.** New code = *Previous version*: усе, що змінилося після першого аналізу, — «новий код». На кроці 2 з'являються 2 нові Bugs (`==` для рядків, `new BigDecimal(0.1)`), новий неперевірений Security Hotspot (`java.util.Random`) та ~80 дубльованих рядків (≈6 % > 3 %).
> Якщо Sonar покаже дублювання трохи інакше — це нормально, важливо, що умова `Duplicated Lines % > 3` або `New Bugs > 0` червона.

## 3. ЛР8 — OO-аудит

```bash
pip install -r tools/requirements.txt

# Завд.1: CK + MOOD + Coupling Matrix для поточного стану
./scripts/oo-audit.sh current              # PowerShell: .\scripts\oo-audit.ps1 current
# Web API SonarQube (після ./scripts/scan.sh на потрібному стані):
./scripts/sonar-api-measures.sh            # сирі curl-запити + JSON  (PowerShell: .\scripts\sonar-api-measures.ps1)
python tools/sonar_api_report.py --stage current --oo reports/oo/current/oo-metrics.json
```
Результати: `reports/oo/<stage>/coupling-matrix.html` (відкрити у браузері → скріншот), `oo-metrics.md`, `reports/sonar-raw/...` (сирі JSON), `reports/<stage>/ck-mood-report.md`.

Для Baseline-таблиці ЛР8 відскануйте тег `lab8-baseline` (`git checkout lab8-baseline && ./scripts/scan.sh && python tools/sonar_api_report.py --stage baseline --oo reports/oo/baseline/oo-metrics.json`).

Гейти (exit code 1 = FAIL):
```bash
./scripts/oo-gate-god-object.sh check     # CBO <= 8, LCOM <= 0.2 для класів після декомпозиції
./scripts/oo-gate-inheritance.sh check    # DIT <= 1, NMO = 0 для класів звітів
# Windows: .\scripts\oo-gate-god-object.ps1 check ; .\scripts\oo-gate-inheritance.ps1 check
```

Стани FAIL ЛР8 (`lab8-godobject-fail`, `lab8-inheritance-fail`) **навмисно не компілюються** — їх показуємо через `git show <tag>` та логи `docs/lab8/*-build.log`; Sonar для них не запускається.

## 4. Сценарій захисту (≈5 хв)

**ЛР1**
1. SonarQube → проєкт *Shop Quality* → зелений **Passed**. Назвати 4 умови gate (див. LAB1_REPORT §3).
2. Термінал: `git log --oneline --decorate` — показати теги `lab1-baseline`, `lab1-fail`, `lab1-pass`, `lab1-refactor`.
3. IDE: `git diff lab1-pass lab1-refactor -- src/main/java/com/kira/shop/service/StoreManager.java` — «до/після» `placeOrder` (Cognitive 49 → 2) та `summarize` (40 → 0); найгірший метод після — 5: Guard Clauses + Extract Method.

**ЛР8**
1. `reports/oo/baseline/coupling-matrix.html` — червоний рядок `StoreManager` (CBO 19).
2. `git show lab8-godobject-fail --stat` + `docs/lab8/shotgun-surgery-build.log` — 7 помилок компіляції від однієї зміни.
3. `./scripts/oo-gate-god-object.sh check` → `OO GATE: PASS`; схема в `docs/lab8/architecture-blueprint.md`.
4. `docs/lab8/inheritance-crash-build.log` → `./scripts/oo-gate-inheritance.sh check` (DIT 3 → 1, NMO 6 → 0).

## 5. Шпаргалка до усних питань

- **Cyclomatic vs Cognitive.** Cyclomatic (McCabe) = кількість незалежних шляхів (≈ мінімальна кількість тестів), кожне розгалуження +1 незалежно від вкладеності. Cognitive (SonarSource) моделює *зусилля читача*: +1 за перерив лінійного потоку **плюс штраф за вкладеність**, послідовність однакових `&&` рахується один раз, `switch` — один раз. Приклад: `placeOrder` до рефакторингу — Cyclomatic 13, Cognitive 49; після — Cyclomatic 3, Cognitive 2 у самому методі (складність розподілена по маленьких методах з іменами).
- **CBO** — з якою кількістю інших класів зв'язаний клас. Високий CBO → зміна будь-якого сусіда може зламати клас (Shotgun Surgery), клас важко тестувати ізольовано.
- **LCOM** — брак згуртованості: наскільки методи класу працюють з *різними* полями. Високий LCOM → у класі живуть кілька незалежних відповідальностей, його треба розділити (LCOM4 = кількість таких «островів»).
- **God Object vs Inheritance Hell.** God Object — проблема *горизонтальна*: один клас знає/робить забагато (високі CBO, LCOM, WMC); лікується декомпозицією за SRP + DIP. Inheritance Hell — проблема *вертикальна*: глибока ієрархія з масовими перевизначеннями (DIT, NMO) — зміна в батьку каскадно ламає нащадків (Fragile Base Class); лікується композицією замість успадкування.
- **Чому метрики CK рахує окремий інструмент.** SonarQube прибрав LCOM4 ще у 4.x і ніколи не мав CBO/DIT у ядрі; запит із ключами `coupling_between_objects,lcom4` повертає `metric keys are not found` (див. `reports/sonar-raw/.../ck-legacy-keys.json`). Тому Web API дає `complexity` (WMC), `cognitive_complexity`, `ncloc` тощо, а CBO/LCOM/DIT/MOOD — `tools/oo_metrics.py`.
