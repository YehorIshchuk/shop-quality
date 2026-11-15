#!/usr/bin/env bash
# OO-аудит (CK / Lorenz-Kidd / MOOD) + Coupling Matrix.  Використання: ./scripts/oo-audit.sh <stage> [extra args]
# Приклад gate:  ./scripts/oo-audit.sh god-object-pass --gate OrderPlacementService,PricingService --max-cbo 8 --max-lcom 0.2
set -euo pipefail
STAGE="${1:-current}"; shift || true
python3 tools/oo_metrics.py src/main/java -o "reports/oo/$STAGE" --focus Order,FileOrderRepository,OrderDto "$@"
