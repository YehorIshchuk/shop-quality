#!/usr/bin/env bash
# Гейт ЛР8 Завд.3: класи звітів мають DIT <= 1 та 0 перевизначених методів (NMO = 0)
set -euo pipefail
./scripts/oo-audit.sh "${1:-current}" --max-dit 1 --max-nmo 0 --max-cbo 8 --max-lcom 2 \
  --gate "${GATE_CLASSES:-ReportTable,SalesReport,MonthlySalesReport}"
