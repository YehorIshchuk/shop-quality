# Гейт ЛР8 Завд.2 (Windows): класи після декомпозиції God Object мають CBO <= 8 та LCOM <= 0.2
param([string]$Stage = "current")
python tools/java_syntax_guard.py src/main/java; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }  # CAPA-2
python tools/oo_metrics.py src/main/java -o "reports/oo/$Stage" --max-cbo 8 --max-lcom 0.2 `
  --gate "OrderPlacementService,OrderPaymentService,OrderShippingService,OrderCancellationService,CatalogService,PromotionService,CustomerInsightsService,SalesReportingService,PricingService,StockAllocator,SalesLedger"
exit $LASTEXITCODE
