---
title: Архів процесу
owner: Yehor Ishchuk
status: published
review_date: 2027-11-22
tags: [process]
---

# Архів документації процесу

Звіти лабораторних — **документація процесу**: знімок стану системи в момент виконання. Вони не переписуються
(при міграції в ЛР11 змінено лише відносні шляхи посилань і додано front matter). Актуальний стан — рівні 1–4.

| Документ | Етап | Статус |
|---|---|---|
| [LAB1_REPORT](LAB1_REPORT.md) | SonarQube, Quality Gate | published (архів) |
| [LAB8_REPORT](LAB8_REPORT.md) | ООП-аудит CK/MOOD, God Object, Inheritance Hell | published (архів) |
| [LAB10_REPORT](LAB10_REPORT.md) | аудит документації, архітектурний паспорт, handover | published (архів) |
| [LAB11_REPORT](LAB11_REPORT.md) | Documentation as Code: MkDocs, OpenAPI, CI/CD документації | published (архів) |
| [DEMO](DEMO.md) | відтворення станів ЛР1/ЛР8 на Docker | **deprecated** → [[local-setup]] |

## Стани репозиторію (git-теги)

| Тег | Стан |
|---|---|
| `lab1-baseline` / `lab1-fail` / `lab1-pass` / `lab1-refactor` | SonarQube: базове сканування → порушення → виправлення → Cognitive 49 → 5 |
| `lab8-baseline` / `lab8-godobject-fail` / `lab8-godobject-pass` | God Object `StoreManager` → Shotgun Surgery (не компілюється) → порти й адаптери |
| `lab8-inheritance-baseline` / `-fail` / `-pass` | ієрархія звітів DIT 3 → каскадний злам → композиція |
| `lab10-decay-fail` / `lab10-docs-pass` | HTTP API без оновлення документів → документацію відновлено |
| `lab11-ci-fail` / `lab11-ci-pass` | мертве посилання → strict-збірка Exit 1 → відновлено, Exit 0 |
| `docs-v1.0` / `docs-v2.0` | знімки документації: паспорт ЛР10 / база знань ЛР11 |
