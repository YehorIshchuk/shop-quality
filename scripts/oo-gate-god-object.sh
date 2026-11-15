#!/usr/bin/env bash
# Гейт ЛР8 Завд.2: кожен клас, на який розбито God Object, має CBO <= 8 та LCOM <= 0.2
set -euo pipefail
./scripts/oo-audit.sh "${1:-current}" --max-cbo 8 --max-lcom 0.2 --gate \
OrderPlacementService,OrderPaymentService,OrderShippingService,OrderCancellationService,CatalogService,PromotionService,CustomerInsightsService,SalesReportingService,PricingService,StockAllocator,SalesLedger
