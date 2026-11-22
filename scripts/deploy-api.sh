#!/usr/bin/env bash
# RB-001: розгортання версії Shop Core API (D1) з health-перевіркою; рівень автоматизації 1 (скриптований).
# Використання:
#   ./scripts/deploy-api.sh <git-тег>      # розгорнути версію (наприклад v1.1.0)
#   ./scripts/deploy-api.sh rollback       # повернути попередню розгорнуту версію
#   ./scripts/deploy-api.sh status         # яка версія працює і чи живий health
# Змінні: DEPLOY_DIR (каталог релізів, за замовч. ~/shop-deploy), SHOP_HTTP_PORT (8080),
#         BUILD_CMD (команда збірки в каталозі релізу, за замовч. "mvn -B -q -DskipTests package")
set -euo pipefail
REPO="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY_DIR="${DEPLOY_DIR:-$HOME/shop-deploy}"
PORT="${SHOP_HTTP_PORT:-8080}"
BUILD_CMD="${BUILD_CMD:-mvn -B -q -DskipTests package}"
HEALTH="http://localhost:${PORT}/api/health"
mkdir -p "$DEPLOY_DIR/releases" "$DEPLOY_DIR/logs"

log() { printf '%s  %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"; }
healthy() { curl -sf -m 2 "$HEALTH" | grep -q '"UP"'; }

stop_current() {
  if [[ -f "$DEPLOY_DIR/api.pid" ]] && kill -0 "$(cat "$DEPLOY_DIR/api.pid")" 2>/dev/null; then
    log "зупиняю PID $(cat "$DEPLOY_DIR/api.pid") ($(cat "$DEPLOY_DIR/current" 2>/dev/null || echo '?'))"
    kill "$(cat "$DEPLOY_DIR/api.pid")"
    for _ in $(seq 1 20); do kill -0 "$(cat "$DEPLOY_DIR/api.pid")" 2>/dev/null || break; sleep 0.5; done
  fi
  rm -f "$DEPLOY_DIR/api.pid"
}

deploy() {
  local tag="$1" dir="$DEPLOY_DIR/releases/$1"
  git -C "$REPO" rev-parse -q --verify "refs/tags/$tag" >/dev/null || { log "немає тегу $tag (git fetch --tags)"; exit 1; }
  if [[ ! -d "$dir/target/classes" ]]; then
    log "збираю $tag у $dir"
    rm -rf "$dir" && mkdir -p "$dir"
    git -C "$REPO" archive "$tag" | tar -x -C "$dir"
    (cd "$dir" && eval "$BUILD_CMD")
  fi
  local prev; prev="$(cat "$DEPLOY_DIR/current" 2>/dev/null || true)"
  stop_current
  log "запускаю $tag на :$PORT"
  (cd "$dir" && SHOP_HTTP_PORT="$PORT" nohup java -cp target/classes com.kira.shop.ShopApplication \
      > "$DEPLOY_DIR/logs/api-$tag.log" 2>&1 & echo $! > "$DEPLOY_DIR/api.pid")
  for i in $(seq 1 30); do
    if healthy; then
      [[ -n "$prev" && "$prev" != "$tag" ]] && echo "$prev" > "$DEPLOY_DIR/previous"
      echo "$tag" > "$DEPLOY_DIR/current"
      log "HEALTH OK за ${i} с — розгорнуто $tag (попередня: ${prev:-немає})"
      return 0
    fi
    sleep 1
  done
  log "HEALTH FAILED за 30 с — дивись $DEPLOY_DIR/logs/api-$tag.log; відкат: $0 rollback"
  exit 2
}

case "${1:-}" in
  rollback)
    prev="$(cat "$DEPLOY_DIR/previous" 2>/dev/null || true)"
    [[ -n "$prev" ]] || { log "немає попередньої версії для відкату"; exit 1; }
    log "ВІДКАТ на $prev"; deploy "$prev" ;;
  status)
    echo "current:  $(cat "$DEPLOY_DIR/current" 2>/dev/null || echo '—')"
    echo "previous: $(cat "$DEPLOY_DIR/previous" 2>/dev/null || echo '—')"
    if healthy; then echo "health:   UP ($HEALTH)"; else echo "health:   DOWN ($HEALTH)"; exit 2; fi ;;
  ""|-h|--help) sed -n '2,8p' "$0" ;;
  *) deploy "$1" ;;
esac
