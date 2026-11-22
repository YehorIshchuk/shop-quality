# Changelog

Формат — [Keep a Changelog](https://keepachangelog.com/uk/1.1.0/); версії API — [SemVer](https://semver.org/).
Знімки документації позначаються тегами `docs-vX.Y`.

## [Unreleased]

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
