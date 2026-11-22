---
title: CONTRIBUTING
owner: Yehor Ishchuk
status: published
review_date: 2027-02-19
tags: [developer]
---

# Як вносити зміни (CONTRIBUTING)

## 1. Цикл зміни

1. Гілка від `main`: `feat/<коротко>`, `fix/<коротко>`, `docs/<коротко>`.
2. Спершу тест (`src/test/java`), потім код; `mvn -B clean verify` — зелений.
3. **Зміна коду = зміна документації** (регламент [[maintenance]]): той самий коміт / PR оновлює README,
   [[sdd]], [[isd]] або ADR, якщо змінилась поведінка, структура чи розгортання.
4. Коміти — Conventional Commits ([[coding-standards]]).
5. Pull request за шаблоном `.github/pull_request_template.md`; рев'ю власника з `.github/CODEOWNERS` обов'язкове
   для `docs/`, `mkdocs.yml` і конвеєрів.

## 2. Документація як код

- Уся документація — Markdown у `docs/`, одне джерело на один факт: README не копіюється на сайт, а включається
  (`docs/index.md`), історія змін — так само з `CHANGELOG.md`.
- Кожен файл `docs/**/*.md` має **front matter-паспорт**:

```yaml
---
owner: Yehor Ishchuk            # відповідальний за актуальність
status: published               # draft | in-review | published | deprecated
review_date: 2027-02-19         # наступний обов'язковий перегляд
tags: [architecture]            # architecture | api | ops | developer | process
---
```

- Застарілу сторінку **не видаляють**: `status: deprecated` + `superseded_by:` і банер із посиланням на актуальний документ.
- Архітектурне рішення = новий ADR, старий не редагується по суті ([[0001-hexagonal-ports-and-adapters|канон ADR]]).
- Перед push: `mkdocs build --strict` — нуль попереджень.

## Definition of Done: docs-or-don't-merge

Pull request, що **змінює публічну поведінку** — новий або змінений ендпоінт, формат відповіді, конфігурація,
змінна середовища, порядок запуску — **не зливається**, доки не виконано всі пункти:

- [ ] `docs/` оновлено: README (рівень 1), SDD / ISD (рівень 4) або нове ADR — за таблицею тригерів [[maintenance]]
- [ ] `docs/api/openapi.yaml` оновлено, `info.version` піднято за SemVer (patch — виправлення, minor — новий ендпоінт, major — несумісна зміна)
- [ ] `CHANGELOG.md` має запис у розділі `[Unreleased]`
- [ ] у front matter змінених сторінок оновлено `review_date`; застаріла сторінка → `status: deprecated` + `superseded_by`
- [ ] конвеєр `docs.yml` зелений: markdownlint · review-date audit · doc currency gate · `mkdocs build --strict` · Spectral · Schemathesis

Автоматичні перевірки (таблиця DoD у README) блокують злиття самі; рев'ю власника з `.github/CODEOWNERS`
підтверджує, що документація описує зміну по суті, а не лише проходить лінтери.

## Версії документації

Мажорна зміна документації фіксується тегом `docs-vX.Y` на коміті релізу: знімок документації відновлюється з тегу
так само, як код релізу (`git checkout docs-v1.0 && mkdocs serve`). Історія тегів — у [Історії змін](../changelog.md).
Покроковий процес релізу документації — у [регламенті супроводу](maintenance.md), пункти 5–6 процедури.

## Пов'язані документи

[[local-setup]] · [[coding-standards]] · [[maintenance]] · [[handover]]
