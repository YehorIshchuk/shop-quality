# OO-аудит (CK / Lorenz-Kidd / MOOD) + Coupling Matrix (Windows).  .\scripts\oo-audit.ps1 baseline [--gate ...]
param([string]$Stage = "current", [Parameter(ValueFromRemainingArguments = $true)]$Rest)
python tools/oo_metrics.py src/main/java -o "reports/oo/$Stage" --focus Order,FileOrderRepository,OrderDto @Rest
exit $LASTEXITCODE
