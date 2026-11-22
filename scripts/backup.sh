#!/usr/bin/env bash
# Резервне копіювання репозиторію (код + документація + теги) за політикою ISD §6.
# Використання:
#   ./scripts/backup.sh full          # повна копія: git bundle --all (щотижня, неділя)
#   ./scripts/backup.sh incremental   # інкрементна: коміти після останньої успішної копії (щодня)
# Змінні: BACKUP_DIR (носій №2, напр. /Volumes/ShopBackup), BACKUP_PASSPHRASE (ключ шифрування AES-256,
#         зберігається у Keychain / менеджері паролів, НЕ в репозиторії)
set -euo pipefail
REPO="$(cd "$(dirname "$0")/.." && pwd)"
MODE="${1:-incremental}"
: "${BACKUP_DIR:?задайте BACKUP_DIR — каталог на окремому носії}"
: "${BACKUP_PASSPHRASE:?задайте BACKUP_PASSPHRASE — ключ шифрування}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$BACKUP_DIR"
cd "$REPO"

case "$MODE" in
  full)        RANGE=(--all) ;;
  incremental) BASE="$(git rev-parse -q --verify refs/backup/last 2>/dev/null || true)"
               if [[ -z "$BASE" ]]; then echo "немає попередньої копії — роблю full"; MODE=full; RANGE=(--all)
               elif [[ "$BASE" == "$(git rev-parse HEAD)" ]]; then echo "нових комітів немає — копія не потрібна"; exit 0
               else RANGE=("$BASE..HEAD" --tags); fi ;;
  *) echo "режим: full | incremental"; exit 1 ;;
esac

PLAIN="$(mktemp -d)/shop-quality-$MODE-$STAMP.bundle"
git bundle create "$PLAIN" "${RANGE[@]}"
git bundle verify "$PLAIN" >/dev/null
OUT="$BACKUP_DIR/$(basename "$PLAIN").enc"
openssl enc -aes-256-cbc -pbkdf2 -iter 200000 -salt -in "$PLAIN" -out "$OUT" -pass env:BACKUP_PASSPHRASE
shasum -a 256 "$OUT" > "$OUT.sha256"
rm -f "$PLAIN"
git update-ref refs/backup/last HEAD

# retention: інкрементні — 14 днів, повні — 8 тижнів (56 днів)
find "$BACKUP_DIR" -name 'shop-quality-incremental-*.enc*' -mtime +14 -delete
find "$BACKUP_DIR" -name 'shop-quality-full-*.enc*' -mtime +56 -delete
echo "OK $MODE → $OUT ($(du -h "$OUT" | cut -f1)), sha256 → $OUT.sha256"
