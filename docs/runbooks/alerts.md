---
title: Каталог алертів
owner: Yehor Ishchuk
status: published
review_date: 2027-02-22
tags: [ops, runbook]
---

# Каталог алертів моніторингу

**Правило: алерт без runbook — це шум.** Кожен рядок має посилання на розділ runbook, який черговий виконує при
спрацюванні. Джерело даних для умов — `tools/sli_probe.py --check` (синтетична проба), health-перевірка і
історія запусків GitHub Actions; визначення індикаторів — [slo.yaml](../slo.yaml).

| ID | Назва | Умова спрацювання | Серйозність | Відповідальна роль | Runbook |
|---|---|---|---|---|---|
| A-01 | ShopApiDown | `GET /api/health` не повертає `200 {"status":"UP"}` за 5 с у **3 послідовних** перевірках з інтервалом 30 с (≈ 90 с) | SEV-1 | SRE on-call (L2) | [RB-001 §4.3 перезапуск](RB-001.md#43-перезапуск-поточної-версії-a-01) → [§7 відкат](RB-001.md#7-відкат) |
| A-02 | ShopApiErrorRate | частка 5xx (і таймаутів) на `/api/*` **> 1 %** у вікні 5 хв (`sli_probe --check`) | SEV-2 | SRE on-call (L2) | [RB-001 §7 відкат](RB-001.md#7-відкат) |
| A-03 | ShopApiLatencyP95 | p95 латентності GET `/api/*` **≥ 300 мс** протягом 10 хв | SEV-3 | SRE on-call (L2) | [RB-001 §6 діагностика](RB-001.md#6-діагностика-a-03-і-невдала-верифікація) |
| A-05 | OOGateToolError *(додано за CAPA-1, [INC-001](../incidents/INC-001.md))* | крок `God Object gate` / `Inheritance gate` job `oo-gates` завершився **кодом 3** (`OO TOOL ERROR`) — анотація `::error title=A-05` і лист GitHub «Run failed» власнику; або `java_syntax_guard` FAIL на `main` | SEV-2 | черговий конвеєра (L2) | [RB-002](RB-002.md) |
| A-04 | ErrorBudgetBurn | витрачено **≥ 50 %** місячного бюджету будь-якого SLO (`tools/slo_budget.py --consumed …`) | SEV-3 | Change Manager | [error_budget_policy](../slo.yaml) → засідання [CCB](../changes/CCB-001.md) |

## Пов'язані документи

[[RB-001]] · [[isd]] · [[maintenance-plan]]
