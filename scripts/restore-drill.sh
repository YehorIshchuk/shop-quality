#!/usr/bin/env bash
# Restore drill (ISD §6.4): тестове відновлення з ПОВНОЇ зашифрованої копії в чистий каталог
# з вимірюванням фактичного RTO. Копія, з якої відновлення не перевірялося, копією не вважається.
# Використання:  ./scripts/restore-drill.sh <шлях до shop-quality-full-*.bundle.enc>
# Змінні: BACKUP_PASSPHRASE (ключ), SHOP_HTTP_PORT (порт перевірки, за замовч. 18080),
#         BUILD_CMD (збірка + тести, за замовч. "mvn -B -q verify"),
#         MKDOCS_CMD (збірка бази знань у чистому venv, за замовч. "mkdocs build --strict", БЕЗ -q)
# Drill #1 (2026-11-22) показав: відновлення не може покладатися на інструменти старої машини —
# тому крок 6 створює чисте venv і ставить залежності з requirements-docs.txt.
set -euo pipefail
ENC="${1:?шлях до .bundle.enc}"
: "${BACKUP_PASSPHRASE:?задайте BACKUP_PASSPHRASE}"
PORT="${SHOP_HTTP_PORT:-18080}"
BUILD_CMD="${BUILD_CMD:-mvn -B -q verify}"
MKDOCS_CMD="${MKDOCS_CMD:-mkdocs build --strict}"   # без -q: з -q warnings не рахуються і strict дає хибний PASS (ЛР11)
WORK="$(mktemp -d)"
T0=$(date +%s)
step() { printf '%s  +%4ss  %s\n' "$(date -u +%H:%M:%SZ)" "$(( $(date +%s) - T0 ))" "$*"; }

step "1. перевірка цілісності: sha256"
if [[ -f "$ENC.sha256" ]]; then (cd "$(dirname "$ENC")" && shasum -a 256 -c "$(basename "$ENC").sha256"); else step "   (немає .sha256 — пропущено)"; fi
step "2. розшифрування AES-256"
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -in "$ENC" -out "$WORK/restore.bundle" -pass env:BACKUP_PASSPHRASE
step "3. git bundle verify"
git bundle verify "$WORK/restore.bundle" >/dev/null
step "4. клонування в чистий каталог"
git clone -q "$WORK/restore.bundle" "$WORK/shop-quality"
cd "$WORK/shop-quality"
step "   HEAD $(git log -1 --format='%h %s')  ·  тегів: $(git tag | wc -l | tr -d ' ')"
step "5. збірка і тести: $BUILD_CMD"
eval "$BUILD_CMD"
step "6. база знань: чисте venv + requirements-docs.txt + $MKDOCS_CMD"
python3 -m venv "$WORK/venv"
"$WORK/venv/bin/pip" install -q -r requirements-docs.txt
(source "$WORK/venv/bin/activate" && eval "$MKDOCS_CMD")
step "7. запуск API на :$PORT і health-перевірка"
# Порт має бути вільним: інакше health-перевірку пройде чужий процес (стара версія) — хибний PASS (знахідка 2026-11-22)
if curl -s -m 2 -o /dev/null "localhost:$PORT/api/health"; then
  echo "RESTORE DRILL: FAIL — порт $PORT уже зайнятий іншим процесом; зупиніть його або задайте SHOP_HTTP_PORT" >&2; exit 1
fi
SHOP_HTTP_PORT="$PORT" java -cp target/classes com.kira.shop.ShopApplication > "$WORK/api.log" 2>&1 &
PID=$!
healthy=0
for _ in $(seq 1 30); do
  kill -0 "$PID" 2>/dev/null || { echo "RESTORE DRILL: FAIL — відновлений API завершився (див. $WORK/api.log)" >&2; tail -5 "$WORK/api.log" >&2; exit 1; }
  curl -sf -m 2 "localhost:$PORT/api/health" | grep -q UP && { healthy=1; break; }
  sleep 1
done
[[ $healthy -eq 1 ]] || { kill "$PID" 2>/dev/null; echo "RESTORE DRILL: FAIL — health не UP за 30 с" >&2; exit 1; }
curl -sf -m 2 "localhost:$PORT/api/orders/ORD-1" >/dev/null || { kill "$PID"; echo "RESTORE DRILL: FAIL — ORD-1 недоступне" >&2; exit 1; }
kill "$PID"
step "RESTORE DRILL: PASS — фактичний RTO $(( $(date +%s) - T0 )) с (ціль ISD: ≤ 900 с)"
rm -rf "$WORK"
