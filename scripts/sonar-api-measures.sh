#!/usr/bin/env bash
# Прямі curl-запити до SonarQube Web API (лістинги для звіту ЛР8, Завд.1). Сирі JSON -> reports/sonar-raw/curl/
set -euo pipefail
: "${SONAR_TOKEN:?Set SONAR_TOKEN}"
HOST="${SONAR_HOST_URL:-http://localhost:9000}"
P="shop-quality"
OUT=reports/sonar-raw/curl; mkdir -p "$OUT"
M="complexity,cognitive_complexity,ncloc,functions,code_smells,sqale_index"

for f in domain/Order infrastructure/FileOrderRepository dto/OrderDto; do
  name=$(basename "$f")
  set -x
  curl -s -u "$SONAR_TOKEN:" "$HOST/api/measures/component?component=$P:src/main/java/com/kira/shop/$f.java&metricKeys=$M" | tee "$OUT/$name.json"
  { set +x; } 2>/dev/null; echo
done

# Ключі CK з методички — SonarQube повертає помилку "metric keys are not found"
set -x
curl -s -u "$SONAR_TOKEN:" "$HOST/api/measures/component?component=$P:src/main/java/com/kira/shop/domain/Order.java&metricKeys=class_complexity,coupling_between_objects,lcom4" | tee "$OUT/ck-legacy-keys.json"
curl -s -u "$SONAR_TOKEN:" "$HOST/api/measures/component?component=$P&metricKeys=complexity,cognitive_complexity,ncloc,classes,functions,code_smells,sqale_index" | tee "$OUT/project.json"
