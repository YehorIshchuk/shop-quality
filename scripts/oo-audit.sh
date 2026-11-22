#!/usr/bin/env bash
# OO-аудит (CK / Lorenz-Kidd / MOOD) + Coupling Matrix.  Використання: ./scripts/oo-audit.sh <stage> [extra args]
# Приклад gate:  ./scripts/oo-audit.sh god-object-pass --gate OrderPlacementService,PricingService --max-cbo 8 --max-lcom 0.2
set -euo pipefail
STAGE="${1:-current}"; shift || true
# CAPA-2 (INC-001): спершу зрозумілий вердикт про синтаксис, непідтримуваний javalang (exit 1), а не збій парсера
python3 tools/java_syntax_guard.py src/main/java
# exit: 0 PASS · 1 порушено поріг · 3 збій інструмента (OO TOOL ERROR, CAPA-1)
python3 tools/oo_metrics.py src/main/java -o "reports/oo/$STAGE" --focus Order,FileOrderRepository,OrderDto "$@"
