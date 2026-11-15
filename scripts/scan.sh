#!/usr/bin/env bash
# Збирає проєкт, проганяє тести з JaCoCo і відправляє аналіз у SonarQube.
# Збірка завершується з помилкою, якщо Quality Gate = FAILED (sonar.qualitygate.wait=true).
# Використання:  SONAR_TOKEN=squ_xxx ./scripts/scan.sh
set -euo pipefail
: "${SONAR_TOKEN:?Set SONAR_TOKEN (My Account -> Security -> Generate token)}"
SONAR_HOST_URL="${SONAR_HOST_URL:-http://localhost:9000}"

mvn -B clean verify sonar:sonar \
  -Dsonar.host.url="$SONAR_HOST_URL" \
  -Dsonar.token="$SONAR_TOKEN" \
  -Dsonar.qualitygate.wait=true
