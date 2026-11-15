#!/usr/bin/env bash
# Створює кастомний Quality Gate "Shop-Quality-Gate" і прив'язує його до проєкту.
# Потрібен токен користувача з правами адміністратора.
# Використання:  SONAR_TOKEN=squ_xxx ./scripts/setup-quality-gate.sh
set -uo pipefail
: "${SONAR_TOKEN:?Set SONAR_TOKEN}"
HOST="${SONAR_HOST_URL:-http://localhost:9000}"
GATE="Shop-Quality-Gate"
PROJECT="shop-quality"

api() { curl -s -u "$SONAR_TOKEN:" -X POST "$HOST/api/$1" "${@:2}"; echo; }

echo "> Створення проєкту (якщо ще не існує)"
api projects/create --data-urlencode "name=Shop Quality" --data-urlencode "project=$PROJECT"

echo "> Створення Quality Gate $GATE"
api qualitygates/create --data-urlencode "name=$GATE"

cond() { api qualitygates/create_condition --data-urlencode "gateName=$GATE" \
           --data-urlencode "metric=$1" --data-urlencode "op=$2" --data-urlencode "error=$3"; }

echo "> Категорія 1: New Code"
# Критерій 1 (надійність): нових багів = 0. На інстансах у режимі MQR метрики new_bugs може не бути —
# тоді використовується еквівалент New Reliability Rating = A.
out=$(cond new_bugs GT 0); echo "$out"
if echo "$out" | grep -q '"errors"'; then cond new_reliability_rating GT 1; fi
# Критерій 2 (безпека): 100% нових Security Hotspots переглянуто
cond new_security_hotspots_reviewed LT 100

echo "> Категорія 2: Overall Code"
# Критерій 3 (дублювання): Duplicated Lines % <= 3
cond duplicated_lines_density GT 3
# Критерій 4 (підтримуваність): Maintainability Rating = A (технічний борг <= 5% вартості розробки)
cond sqale_rating GT 1

echo "> Прив'язка до проєкту та new code = previous version"
api qualitygates/select --data-urlencode "gateName=$GATE" --data-urlencode "projectKey=$PROJECT"
api new_code_periods/set --data-urlencode "project=$PROJECT" --data-urlencode "type=PREVIOUS_VERSION"

echo "> Поточні умови:"
curl -s -u "$SONAR_TOKEN:" "$HOST/api/qualitygates/show?name=$GATE"; echo
