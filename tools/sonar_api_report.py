#!/usr/bin/env python3
"""
Extracts OO-design measures from SonarQube Web API (api/measures/component) for the key files,
saves RAW JSON responses and merges them with CK/MOOD metrics from tools/oo_metrics.py
into one Markdown table for the report.

Usage:
    export SONAR_TOKEN=squ_...                      # PowerShell: $env:SONAR_TOKEN="squ_..."
    python tools/oo_metrics.py src/main/java -o reports/oo/baseline
    python tools/sonar_api_report.py --stage baseline --oo reports/oo/baseline/oo-metrics.json

Only the Python standard library is used.
"""
from __future__ import annotations

import argparse
import base64
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

DEFAULT_FILES = {
    "Core Domain": "src/main/java/com/kira/shop/domain/Order.java",
    "Infrastructure": "src/main/java/com/kira/shop/infrastructure/FileOrderRepository.java",
    "Utility/DTO": "src/main/java/com/kira/shop/dto/OrderDto.java",
}
# metrics that exist in modern SonarQube (10.x / 2025.x)
SONAR_FILE_METRICS = "complexity,cognitive_complexity,ncloc,functions,classes,code_smells,sqale_index,comment_lines_density"
# CK metric keys from the lab assignment; SonarQube removed / never had them -> the raw error is saved as evidence
CK_LEGACY_METRICS = "class_complexity,coupling_between_objects,lcom4"
PROJECT_METRICS = ("complexity,cognitive_complexity,ncloc,classes,functions,code_smells,sqale_index,"
                   "sqale_debt_ratio,duplicated_lines_density,bugs,vulnerabilities,coverage")


def get(host, token, path, params):
    url = f"{host.rstrip('/')}/api/{path}?{urllib.parse.urlencode(params)}"
    req = urllib.request.Request(url)
    req.add_header("Authorization", "Basic " + base64.b64encode(f"{token}:".encode()).decode())
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return url, resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", "replace")
        try:
            return url, e.code, json.loads(body)
        except json.JSONDecodeError:
            return url, e.code, {"raw": body}


def measures(payload):
    comp = payload.get("component", {})
    return {m["metric"]: m.get("value", m.get("period", {}).get("value")) for m in comp.get("measures", [])}


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default=os.environ.get("SONAR_HOST_URL", "http://localhost:9000"))
    ap.add_argument("--token", default=os.environ.get("SONAR_TOKEN"))
    ap.add_argument("--project", default="shop-quality")
    ap.add_argument("--stage", default="baseline", help="label for output folder, e.g. baseline / god-object-pass")
    ap.add_argument("--oo", type=Path, help="oo-metrics.json produced by tools/oo_metrics.py")
    ap.add_argument("--file", action="append", default=[],
                    help="extra 'Role=path' entries (replace defaults if given)")
    args = ap.parse_args(argv)
    if not args.token:
        sys.exit("Set SONAR_TOKEN or pass --token")

    files = dict(DEFAULT_FILES)
    if args.file:
        files = dict(item.split("=", 1) for item in args.file)

    raw_dir = Path("reports/sonar-raw") / args.stage
    raw_dir.mkdir(parents=True, exist_ok=True)
    log = []

    def call(name, path, params):
        url, status, payload = get(args.host, args.token, path, params)
        (raw_dir / f"{name}.json").write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")
        log.append(f"GET {url}  -> HTTP {status}")
        print(log[-1])
        return status, payload

    sonar = {}
    for role, path in files.items():
        key = f"{args.project}:{path}"
        _, payload = call(Path(path).stem, "measures/component", {"component": key, "metricKeys": SONAR_FILE_METRICS})
        sonar[role] = (path, measures(payload))
    first = next(iter(files.values()))
    call("ck-legacy-keys", "measures/component",
         {"component": f"{args.project}:{first}", "metricKeys": CK_LEGACY_METRICS})
    _, proj = call("project", "measures/component", {"component": args.project, "metricKeys": PROJECT_METRICS})
    call("files-by-complexity", "measures/component_tree",
         {"component": args.project, "qualifiers": "FIL", "metricKeys": "complexity,cognitive_complexity,ncloc",
          "s": "metric", "metricSort": "complexity", "asc": "false", "ps": 10})

    ck = {}
    mood = {}
    if args.oo and args.oo.exists():
        data = json.loads(args.oo.read_text(encoding="utf-8"))
        ck = {Path(r["file"]).as_posix(): r for r in data["classes"]}
        mood = data["mood"]

    out = [f"# CK & MOOD report — stage `{args.stage}`", "", "## API requests", "", "```"] + log + ["```", "",
           "## File level (SonarQube Web API + CK analyzer)", "",
           "| Role | File | WMC (Sonar complexity) | Cognitive | NCLOC | CBO | Ca | RFC | LCOM (HS) | LCOM4 | DIT |",
           "|---|---|---|---|---|---|---|---|---|---|---|"]
    for role, (path, m) in sonar.items():
        r = next((v for k, v in ck.items() if k.endswith(path)), {})
        out.append(f"| {role} | `{Path(path).name}` | {m.get('complexity', '—')} | {m.get('cognitive_complexity', '—')} | "
                   f"{m.get('ncloc', '—')} | {r.get('CBO', '—')} | {r.get('Ca', '—')} | {r.get('RFC', '—')} | "
                   f"{r.get('LCOM', '—')} | {r.get('LCOM4', '—')} | {r.get('DIT', '—')} |")
    pm = measures(proj)
    out += ["", "## Project level", "",
            "| Sonar complexity | Cognitive | NCLOC | Classes | Code smells | Debt (min) | Dup % | MHF | AHF | COF |",
            "|---|---|---|---|---|---|---|---|---|---|",
            f"| {pm.get('complexity', '—')} | {pm.get('cognitive_complexity', '—')} | {pm.get('ncloc', '—')} | "
            f"{pm.get('classes', '—')} | {pm.get('code_smells', '—')} | {pm.get('sqale_index', '—')} | "
            f"{pm.get('duplicated_lines_density', '—')} | {mood.get('MHF', '—')}% | {mood.get('AHF', '—')}% | "
            f"{mood.get('COF', '—')}% |"]
    report = Path("reports") / args.stage / "ck-mood-report.md"
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text("\n".join(out) + "\n", encoding="utf-8")
    print(f"raw JSON: {raw_dir}/   report: {report}")


if __name__ == "__main__":
    main()
