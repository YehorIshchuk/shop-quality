#!/usr/bin/env bash
# Виводить статус Quality Gate і ключові метрики проєкту (для звіту / порівняльної таблиці).
set -euo pipefail
: "${SONAR_TOKEN:?Set SONAR_TOKEN}"
HOST="${SONAR_HOST_URL:-http://localhost:9000}"
P="shop-quality"
curl -s -u "$SONAR_TOKEN:" "$HOST/api/qualitygates/project_status?projectKey=$P"; echo
curl -s -u "$SONAR_TOKEN:" "$HOST/api/measures/component?component=$P&metricKeys=bugs,vulnerabilities,security_hotspots,code_smells,sqale_index,sqale_debt_ratio,duplicated_lines_density,complexity,cognitive_complexity,coverage,ncloc"; echo
