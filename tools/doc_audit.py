#!/usr/bin/env python3
"""Documentation Decay Audit (ЛР10): метрики актуальності документації з історії Git CLI.

Для кожного документа з docs/doc-map.json:
  * Last Updated Age  — git log -1 --format=%cs -- <файл>          (днів від останнього коміту)
  * Doc commits       — git log --oneline -- <файл> | wc -l
Для кожного компонента коду:
  * Code commits      — git log --oneline -- <шлях> | wc -l
  * Code-Doc Change Ratio = code commits / doc commits
  * Behind            — git rev-list --count <останній коміт документа>..HEAD -- <шлях>
                        (скільки комітів коду пройшло після останнього оновлення документа)
Структурні перевірки (structure drift) — порівняння коду з текстом документів:
  * кожен пакет (як `назва/` у бектиках) і кожен клас адаптерів (infrastructure/, api/) згадано в SDD;
  * кожен HTTP-маршрут (createContext("...")) згадано в SDD і README;
  * кожна змінна середовища (System.getenv / ${VAR} у scripts/*.sh) згадана в ISD.

Кожна виконана команда Git друкується разом із результатом — це і є «лог метрик».

Використання:
  python3 tools/doc_audit.py                       # звіт у консоль
  python3 tools/doc_audit.py --out reports/docs/baseline.md
  python3 tools/doc_audit.py --gate                # exit 1, якщо є дрейф (FAIL)
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SUSPICIOUS_DAYS = 90
STALE_DAYS = 180
DRIFT_RATIO_LIMIT = 20  # > 20 змін коду без оновлення документа — структурний дрейф (методичка)


def git(*args: str) -> str:
    out = subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True, check=False)
    return out.stdout.strip()


class Log:
    def __init__(self) -> None:
        self.lines: list[str] = []

    def cmd(self, shown: str, result: str) -> None:
        self.lines.append(f"$ {shown}\n{result if result else '(порожньо)'}")


def doc_metrics(path: str, today: dt.date, log: Log) -> dict:
    exists = (ROOT / path).exists()
    last = git("log", "-1", "--format=%cs", "--", path)
    log.cmd(f"git log -1 --format=%cs -- {path}", last)
    commits = len(git("log", "--oneline", "--", path).splitlines()) if last else 0
    log.cmd(f"git log --oneline -- {path} | wc -l", str(commits))
    if not exists:
        status, age = ("DELETED" if last else "MISSING"), None
    elif not last:
        status, age = "UNTRACKED", None
    else:
        age = (today - dt.date.fromisoformat(last)).days
        status = "OK" if age <= SUSPICIOUS_DAYS else ("SUSPICIOUS" if age <= STALE_DAYS else "STALE")
    return {"path": path, "exists": exists, "last": last or "—", "age": age, "commits": commits, "status": status}


def component_metrics(comp: dict, log: Log) -> dict:
    path, doc = comp["path"], comp["doc"]
    code_commits = len(git("log", "--oneline", "--", path).splitlines())
    log.cmd(f"git log --oneline -- {path} | wc -l", str(code_commits))
    doc_commits = len(git("log", "--oneline", "--", doc).splitlines())
    doc_last_sha = git("log", "-1", "--format=%h", "--", doc)
    if doc_last_sha:
        behind = int(git("rev-list", "--count", f"{doc_last_sha}..HEAD", "--", path) or 0)
        log.cmd(f"git rev-list --count {doc_last_sha}..HEAD -- {path}", str(behind))
    else:
        behind = code_commits
        log.cmd(f"git log -1 --format=%h -- {doc}", "(документ відсутній в історії)")
    ratio = f"{code_commits}:{doc_commits}"
    if doc_commits == 0 or behind > DRIFT_RATIO_LIMIT:
        verdict = "STRUCTURAL DRIFT"
    elif behind > 0:
        verdict = "BEHIND"
    else:
        verdict = "OK"
    return {**comp, "code": code_commits, "doc_commits": doc_commits, "ratio": ratio, "behind": behind, "verdict": verdict}


def read(path: str) -> str:
    p = ROOT / path
    return p.read_text(encoding="utf-8") if p.exists() else ""


def structure_checks(cfg: dict) -> list[tuple[str, str, bool]]:
    checks: list[tuple[str, str, bool]] = []
    sdd_path, isd_path = cfg.get("sdd", "docs/sdd.md"), cfg.get("isd", "docs/isd.md")
    sdd, isd, readme = read(sdd_path), read(isd_path), read("README.md")
    src = ROOT / "src/main/java"
    java = sorted(src.rglob("*.java"))
    if not java:
        return checks
    base = java[0].relative_to(src).parts[:3]  # com/kira/shop
    pkg_root = src.joinpath(*base)
    for pkg in sorted({p.parent for p in java if p.parent != pkg_root}):
        name = pkg.relative_to(pkg_root).as_posix()
        checks.append((f"пакет `{name}/` описано в SDD", sdd_path, f"`{name}/`" in sdd))
    for f in java:
        rel = f.relative_to(pkg_root).as_posix()
        if rel.startswith(("infrastructure/", "api/")):
            checks.append((f"компонент `{f.stem}` описано в SDD", sdd_path, f.stem in sdd))
    code = "\n".join(p.read_text(encoding="utf-8") for p in java)
    for route in sorted(set(re.findall(r'createContext\("([^"]+)"', code))):
        checks.append((f"HTTP-маршрут `{route}` описано в SDD", sdd_path, route in sdd))
        checks.append((f"HTTP-маршрут `{route}` описано в README", "README.md", route in readme))
    env = set(re.findall(r'System\.getenv\("([A-Z0-9_]+)"', code))
    for sh in sorted((ROOT / "scripts").glob("*.sh")):
        env |= set(re.findall(r"\$\{([A-Z][A-Z0-9_]+)[:}]", sh.read_text(encoding="utf-8")))
    for var in sorted(env):
        checks.append((f"змінну середовища `{var}` описано в ISD", isd_path, var in isd))
    return checks


def render(cfg: dict, today: dt.date, stage: str) -> tuple[str, bool]:
    log = Log()
    head = git("log", "-1", "--format=%h %cs %s")
    docs = [doc_metrics(p, today, log) for p in cfg["documents"]]
    comps = [component_metrics(c, log) for c in cfg["components"]]
    checks = structure_checks(cfg) if cfg.get("structure_checks", True) else []

    out = [f"# Documentation Decay Audit — `{stage}`", "",
           f"- Дата аудиту: {today.isoformat()}  ",
           f"- HEAD: `{head}`  ",
           f"- Пороги: ≤{SUSPICIOUS_DAYS} дн OK · ≤{STALE_DAYS} дн SUSPICIOUS · >{STALE_DAYS} дн STALE; "
           f"Behind > 0 — документ відстає від коду; >{DRIFT_RATIO_LIMIT} — STRUCTURAL DRIFT", "",
           "## 1. Last Updated Age (документи)", "",
           "| Документ | Останній коміт | Вік, дн | Комітів документа | Статус |", "|---|---|---|---|---|"]
    for d in docs:
        out.append(f"| `{d['path']}` | {d['last']} | {d['age'] if d['age'] is not None else '—'} | {d['commits']} | {d['status']} |")
    out += ["", "## 2. Code-Doc Change Ratio (компоненти)", "",
            "| Компонент | Код | Документ | Комітів коду : документа | Behind (комітів коду після оновлення документа) | Вердикт |",
            "|---|---|---|---|---|---|"]
    for c in comps:
        out.append(f"| {c['name']} | `{c['path']}` | `{c['doc']}` | {c['ratio']} | {c['behind']} | {c['verdict']} |")
    failed_checks = [c for c in checks if not c[2]]
    if checks:
        out += ["", "## 3. Structure drift (код ↔ текст документів)", "",
                f"Перевірок: {len(checks)}, провалено: {len(failed_checks)}", ""]
        for title, doc, ok in checks:
            out.append(f"- {'✅' if ok else '❌'} {title} (`{doc}`)")
    stale_docs = [d for d in docs if d["status"] in {"STALE", "MISSING", "UNTRACKED"}]
    behind = [c for c in comps if c["verdict"] != "OK"]
    passed = not (stale_docs or behind or failed_checks)
    out += ["", "## 4. Підсумок", "",
            f"- Документів без актуальної версії: {len(stale_docs)}",
            f"- Компонентів, де документ відстає від коду: {len(behind)}",
            f"- Провалених структурних перевірок: {len(failed_checks)}", "",
            f"**DOC CURRENCY GATE: {'PASS' if passed else 'FAIL'}**", "",
            "## 5. Лог команд Git CLI", "", "```text", *log.lines, "```", ""]
    return "\n".join(out), passed


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--map", default="docs/doc-map.json")
    ap.add_argument("--stage", default="current")
    ap.add_argument("--today", help="YYYY-MM-DD (за замовчуванням — сьогодні)")
    ap.add_argument("--out", help="зберегти звіт у Markdown-файл")
    ap.add_argument("--gate", action="store_true", help="exit 1, якщо аудит FAIL")
    args = ap.parse_args()

    cfg = json.loads((ROOT / args.map).read_text(encoding="utf-8"))
    today = dt.date.fromisoformat(args.today) if args.today else dt.date.today()
    report, passed = render(cfg, today, args.stage)
    if args.out:
        out = ROOT / args.out
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(report, encoding="utf-8")
    summary = report.split("## 5.")[0]
    print(summary)
    print(f"DOC CURRENCY GATE: {'PASS' if passed else 'FAIL'}")
    return 0 if (passed or not args.gate) else 1


if __name__ == "__main__":
    sys.exit(main())
