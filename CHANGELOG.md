# Changelog

Формат — [Keep a Changelog](https://keepachangelog.com/uk/1.1.0/); версії API — [SemVer](https://semver.org/).
Знімки документації позначаються тегами `docs-vX.Y`.

## [Unreleased]

### Додано

- База знань MkDocs Material за пірамідою README → Developer → API → Architecture (ЛР11).
- Front matter-паспорти (`owner`, `status`, `review_date`, `tags`) у кожному документі, `.github/CODEOWNERS`.

### Змінено

- Документацію реструктуровано: `docs/sdd.md` → `docs/architecture/sdd.md`, `docs/isd.md` → `docs/architecture/isd.md`,
  звіти лабораторних → `docs/reports/`, регламент і handover → `docs/developer/`.

## [docs-v1.0] — 2026-11-21

### Додано

- Архітектурний паспорт: SDD, ISD, ADR-0001…0003, регламент супроводу, handover (ЛР10).
- HTTP API замовлень: `GET /api/health`, `GET /api/orders`, `GET /api/orders/{id}`.
