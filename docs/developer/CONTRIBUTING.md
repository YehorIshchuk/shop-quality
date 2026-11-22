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

## Пов'язані документи

[[local-setup]] · [[coding-standards]] · [[maintenance]] · [[handover]]
