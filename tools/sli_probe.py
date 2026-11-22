#!/usr/bin/env python3
"""SLI-проба HTTP API (ЛР12): вимірює індикатори з docs/slo.yaml на живому сервісі.

Моніторингу Prometheus/Grafana у проєкті немає (ЛР2 не входила в цей репозиторій), тому SLI
вимірюються синтетичним навантаженням: суміш запитів до всіх маршрутів контракту
(health 20 %, список 40 %, існуюче замовлення 30 %, неіснуюче 10 %).

  availability  — частка відповідей не 5xx (404/405 — коректна поведінка контракту = успіх)
  latency       — частка запитів з латентністю < 300 мс; також p50 / p95 / p99
  latency_window— частка 10-секундних вікон, у яких p95 < 300 мс (віконний SLI для бюджету у хвилинах)

Латентність міряється на клієнті (від відправлення до отримання тіла) — це верхня оцінка
серверної латентності, тобто консервативна для SLO.

Використання:
  python3 tools/sli_probe.py --url http://localhost:8080 --requests 3000 --concurrency 8
  python3 tools/sli_probe.py --url http://localhost:8080 --check     # exit 1, якщо порушено алертні умови
"""
from __future__ import annotations

import argparse
import concurrent.futures as cf
import json
import random
import statistics
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone

MIX = [("/api/health", 20), ("/api/orders", 40), ("/api/orders/ORD-1", 30), ("/api/orders/ORD-404", 10)]
LATENCY_THRESHOLD_MS = 300.0
WINDOW_S = 10


def one(url: str) -> tuple[float, int, float]:
    started = time.time()
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(url, timeout=5) as resp:
            resp.read()
            code = resp.status
    except urllib.error.HTTPError as e:
        e.read()
        code = e.code
    except (urllib.error.URLError, TimeoutError, ConnectionError):
        code = 599  # мережева відмова = недоступність
    return started, code, (time.perf_counter() - t0) * 1000.0


def percentile(values: list[float], q: float) -> float:
    s = sorted(values)
    k = max(0, min(len(s) - 1, round(q / 100 * (len(s) - 1))))
    return s[k]


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--url", default="http://localhost:8080")
    ap.add_argument("--requests", type=int, default=3000)
    ap.add_argument("--concurrency", type=int, default=8)
    ap.add_argument("--json", help="зберегти результат у JSON")
    ap.add_argument("--check", action="store_true", help="перевірити алертні умови каталогу (exit 1 при порушенні)")
    args = ap.parse_args()

    random.seed(42)
    paths = [p for p, w in MIX for _ in range(w)]
    plan = [args.url.rstrip("/") + random.choice(paths) for _ in range(args.requests)]
    t_start = datetime.now(timezone.utc)
    with cf.ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        results = list(pool.map(one, plan))
    t_end = datetime.now(timezone.utc)

    codes: dict[int, int] = {}
    for _, c, _ in results:
        codes[c] = codes.get(c, 0) + 1
    lat = [ms for _, _, ms in results]
    good_avail = sum(1 for _, c, _ in results if c < 500)
    good_lat = sum(1 for ms in lat if ms < LATENCY_THRESHOLD_MS)
    windows: dict[int, list[float]] = {}
    for started, _, ms in results:
        windows.setdefault(int(started // WINDOW_S), []).append(ms)
    good_windows = sum(1 for w in windows.values() if percentile(w, 95) < LATENCY_THRESHOLD_MS)

    out = {
        "measured_at_utc": t_start.isoformat(timespec="seconds"),
        "duration_s": round((t_end - t_start).total_seconds(), 2),
        "target": args.url, "requests": len(results), "concurrency": args.concurrency,
        "status_codes": dict(sorted(codes.items())),
        "sli": {
            "availability": round(100.0 * good_avail / len(results), 3),
            "latency_under_300ms": round(100.0 * good_lat / len(results), 3),
            "latency_good_windows": round(100.0 * good_windows / len(windows), 3),
        },
        "latency_ms": {"p50": round(statistics.median(lat), 2), "p95": round(percentile(lat, 95), 2),
                       "p99": round(percentile(lat, 99), 2), "max": round(max(lat), 2)},
    }
    print(json.dumps(out, ensure_ascii=False, indent=2))
    if args.json:
        with open(args.json, "w", encoding="utf-8") as fh:
            json.dump(out, fh, ensure_ascii=False, indent=2)
    if args.check:
        five_xx = 100.0 - out["sli"]["availability"]
        breached = five_xx > 1.0 or out["latency_ms"]["p95"] >= LATENCY_THRESHOLD_MS
        print("ALERT CONDITIONS:", "BREACHED" if breached else "OK")
        return 1 if breached else 0
    return 0


if __name__ == "__main__":
    sys.exit(main())
