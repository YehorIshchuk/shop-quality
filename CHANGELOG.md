# Changelog

Формат — [Keep a Changelog](https://keepachangelog.com/uk/1.1.0/); версії API — [SemVer](https://semver.org/).
Знімки документації позначаються тегами `docs-vX.Y`.

## [Unreleased]

### Додано

- `tools/java_syntax_guard.py` — перевірка синтаксису Java, непідтримуваного OO-гейтами (CAPA-2), крок у CI і `oo-audit.sh`.
- Модульні тести legacy-модуля `tools/tests/` (fixtures Java 8 / record / Java 17), крок у job `oo-gates`.
- [ADR-0004](docs/architecture/adr/0004-javalang-java8-syntax-limit.md), runbook RB-002, алерт A-05 `OOGateToolError`,
  щотижневий плановий запуск конвеєра; інцидент [INC-001](docs/incidents/INC-001.md) і [PM-001](docs/incidents/PM-001.md);
  [план супроводу](docs/maintenance-plan.md).

### Змінено

- `tools/oo_metrics.py`: нерозбірний файл → код виходу **3** «OO TOOL ERROR» з переліком усіх файлів замість
  `sys.exit` з кодом 1 (той самий, що й порушення порога) (CAPA-1).
- Регламент CCB: залишковий ризик приймається лише з власником і терміном; CODEOWNERS покриває `/tools/`, `/scripts/`, `/src/`.

### Виправлено

- INC-001: `PaymentResult` повернуто до `final`-класу — Java `record` ламав розбір у `oo_metrics.py` (javalang ≤ Java 8).

## [v1.1.0] — 2026-11-22

Перший реліз за RFC-процесом ([RFC-001](docs/changes/RFC-001.md), Release Checklist — `docs/changes/release-checklist.md`).

### Додано

- Експлуатаційний пакет SRE (ЛР12): runbook RB-001 і `scripts/deploy-api.sh` (розгортання/відкат), каталог алертів,
  `docs/slo.yaml` з трьома SLI і перевіркою бюджетів (`tools/slo_budget.py`), SLI-проба `tools/sli_probe.py`,
  політика бекапу 3-2-1 (`scripts/backup.sh`) і restore drill (`scripts/restore-drill.sh`).
- Управління змінами: RFC-001, протокол CCB-001, шаблон заявки `.github/ISSUE_TEMPLATE/rfc.yml`.
- Конвеєр `docs.yml`: job `oo-gates` — OO-гейти ЛР8 (CBO / LCOM / DIT / NMO) на кожен push і PR, OO-звіт як artifact (RFC-001).

### Виправлено

- Сторінка API: «Try it out» працює через локальний `mkdocs serve`; з GitHub Pages Chrome блокує запити до `localhost`.
- Звіт ЛР11 доповнено скріншотами конвеєра (FAIL → PASS), сайту, Swagger UI і графа Obsidian.

## [docs-v2.0] — 2026-11-22

### Додано

- База знань MkDocs Material за пірамідою README → Developer → API → Architecture (ЛР11).
- Front matter-паспорти (`owner`, `status`, `review_date`, `tags`) у кожному документі, `.github/CODEOWNERS`.
- OpenAPI-контракт `docs/api/openapi.yaml` (API 1.0.0), правила Spectral, довідка Swagger UI, клієнтський SDK, Javadoc.
- HTTP API: заголовок `Access-Control-Allow-Origin: *` для Swagger UI «Try it out».
- Конвеєр GitHub Actions `docs.yml`: markdownlint, review-date audit, doc currency gate, `mkdocs build --strict`,
  Spectral, Schemathesis, SDK, Javadoc і публікація на GitHub Pages; чек-лист docs-or-don't-merge у CONTRIBUTING.

### Виправлено

- HTTP API приведено до контракту (знайдено Schemathesis): JSON-екранування керуючих символів у `orderId`,
  `405` для не-GET на `/api/health`, заголовок `Allow: GET` у всіх `405`, `500` зі спільним `Error`
  замість обірваного з'єднання.

### Змінено

- Документацію реструктуровано: `docs/sdd.md` → `docs/architecture/sdd.md`, `docs/isd.md` → `docs/architecture/isd.md`,
  звіти лабораторних → `docs/reports/`, регламент і handover → `docs/developer/`.

## [docs-v1.0] — 2026-11-21

### Додано

- Архітектурний паспорт: SDD, ISD, ADR-0001…0003, регламент супроводу, handover (ЛР10).
- HTTP API замовлень: `GET /api/health`, `GET /api/orders`, `GET /api/orders/{id}`.
