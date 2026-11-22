# Гейт ЛР8 Завд.3 (Windows): класи звітів мають DIT <= 1 та NMO = 0
param([string]$Stage = "current", [string]$Classes = "ReportTable,SalesReport,MonthlySalesReport")
python tools/java_syntax_guard.py src/main/java; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }  # CAPA-2
python tools/oo_metrics.py src/main/java -o "reports/oo/$Stage" --max-dit 1 --max-nmo 0 --max-cbo 8 --max-lcom 2 --gate $Classes
exit $LASTEXITCODE
