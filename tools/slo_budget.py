#!/usr/bin/env python3
"""Перевірка контракту рівня послуг docs/slo.yaml (ЛР12).

  * Error Budget = (100% − ціль) × хвилини вікна — для SLO і SLA кожного SLI;
    --check звіряє з error_budget_minutes у YAML (помилка арифметики → exit 1);
  * SLA має бути консервативнішим (меншим) за SLO;
  * --consumed SLI_ID=ХВИЛИНИ рахує частку витраченого бюджету і режим error budget policy.

Використання:
  python3 tools/slo_budget.py --check
  python3 tools/slo_budget.py --consumed pipeline-verdict=41
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parent.parent


def budget(target: float, window_min: int) -> float:
    return round((100.0 - target) / 100.0 * window_min, 1)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--file", default="docs/slo.yaml")
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--consumed", action="append", default=[], help="SLI_ID=хвилини витраченого бюджету")
    args = ap.parse_args()

    spec = yaml.safe_load((ROOT / args.file).read_text(encoding="utf-8"))
    window = int(spec["window"]["minutes"])
    if window != spec["window"]["days"] * 24 * 60:
        print(f"ERROR: window.minutes={window} ≠ days×1440")
        return 1
    errors = 0
    print(f"| SLI | SLO | SLA | Бюджет SLO, хв/{spec['window']['days']} дн | Бюджет SLA, хв | Буфер, хв | Виміряно |")
    print("|---|---|---|---|---|---|---|")
    for s in spec["slis"]:
        b_slo, b_sla = budget(s["slo"], window), budget(s["sla"], window)
        declared = s.get("error_budget_minutes", {})
        if s["sla"] >= s["slo"]:
            print(f"ERROR: {s['id']}: SLA {s['sla']} має бути нижчою (консервативнішою) за SLO {s['slo']}")
            errors += 1
        for kind, calc in (("slo", b_slo), ("sla", b_sla)):
            if args.check and abs(float(declared.get(kind, -1)) - calc) > 0.05:
                print(f"ERROR: {s['id']}: error_budget_minutes.{kind}={declared.get(kind)} ≠ розрахунку {calc}")
                errors += 1
        print(f"| {s['id']} | {s['slo']} % | {s['sla']} % | {b_slo} | {b_sla} | {round(b_sla - b_slo, 1)} | "
              f"{s.get('measured', {}).get('value', '—')} % |")
    for item in args.consumed:
        sli_id, minutes = item.split("=")
        s = next(x for x in spec["slis"] if x["id"] == sli_id)
        b = budget(s["slo"], window)
        pct = round(100.0 * float(minutes) / b, 1)
        mode = next(t["action"].split("—")[0].strip() for t in reversed(spec["error_budget_policy"]["thresholds"])
                    if pct >= t.get("consumed_from", 0))
        print(f"\n{sli_id}: витрачено {minutes} хв з {b} хв бюджету SLO = {pct} % → режим: {mode}")
    if args.check:
        print("\nSLO SPEC CHECK:", "FAIL" if errors else "PASS")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
