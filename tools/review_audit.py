#!/usr/bin/env python3
"""Безперервний аудит актуальності документації за front matter-паспортами (ЛР11, Завдання 3).

Для кожного docs/**/*.md читає YAML front matter і перевіряє:
  * паспорт повний: owner, status, review_date (YYYY-MM-DD);          → інакше ERROR (exit 1)
  * status ∈ {draft, in-review, published, deprecated};               → інакше ERROR
  * deprecated-сторінка має superseded_by, що вказує на існуючий документ → інакше ERROR
  * review_date < сьогодні для не-deprecated сторінки                 → PROSTROCHENO (попередження)

Прострочені сторінки не видаляються: власник переглядає їх і оновлює review_date
або переводить у status: deprecated з superseded_by (переспрямування на актуальний документ).

Вивід — Markdown-таблиця у stdout і, в GitHub Actions, у $GITHUB_STEP_SUMMARY;
прострочені сторінки додатково позначаються анотаціями ::warning.

Використання:
  python3 tools/review_audit.py                     # сьогоднішня дата
  python3 tools/review_audit.py --today 2027-03-01  # «що буде прострочено на дату»
  python3 tools/review_audit.py --fail-on-overdue   # зробити прострочення блокуючим
"""
from __future__ import annotations

import argparse
import datetime as dt
import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DOCS = ROOT / "docs"
STATUSES = {"draft", "in-review", "published", "deprecated"}
REQUIRED = ("owner", "status", "review_date")
FRONT = re.compile(r"\A---\n(.*?)\n---\n", re.S)


def front_matter(text: str) -> dict[str, str]:
    """Мінімальний розбір плаского YAML (ключ: значення) без зовнішніх залежностей."""
    m = FRONT.match(text)
    if not m:
        return {}
    meta = {}
    for line in m.group(1).splitlines():
        if ":" in line and not line.startswith((" ", "-", "#")):
            key, value = line.split(":", 1)
            meta[key.strip()] = value.strip().strip("'\"")
    return meta


def audit(today: dt.date) -> tuple[list, list, list, list]:
    errors, overdue, deprecated, ok = [], [], [], []
    for path in sorted(DOCS.rglob("*.md")):
        rel = path.relative_to(DOCS).as_posix()
        meta = front_matter(path.read_text(encoding="utf-8"))
        missing = [k for k in REQUIRED if not meta.get(k)]
        if missing:
            errors.append((rel, f"немає полів паспорта: {', '.join(missing)}"))
            continue
        status = meta["status"]
        if status not in STATUSES:
            errors.append((rel, f"невідомий status: {status}"))
            continue
        try:
            review = dt.date.fromisoformat(meta["review_date"])
        except ValueError:
            errors.append((rel, f"review_date не YYYY-MM-DD: {meta['review_date']}"))
            continue
        if status == "deprecated":
            target = meta.get("superseded_by", "")
            if not target or not (DOCS / target).exists():
                errors.append((rel, f"deprecated без дійсного superseded_by: '{target}'"))
            else:
                deprecated.append((rel, meta["owner"], target, meta["review_date"]))
        elif review < today:
            overdue.append((rel, meta["owner"], status, meta["review_date"], (today - review).days))
        else:
            ok.append((rel, meta["owner"], status, meta["review_date"], (review - today).days))
    return errors, overdue, deprecated, ok


def render(today: dt.date, errors, overdue, deprecated, ok) -> str:
    out = [f"## Аудит актуальності документації — {today.isoformat()}", "",
           f"Сторінок: {len(errors) + len(overdue) + len(deprecated) + len(ok)} · "
           f"актуальних: {len(ok)} · прострочених: {len(overdue)} · deprecated: {len(deprecated)} · "
           f"помилок паспорта: {len(errors)}", ""]
    out += ["### Прострочені review_date", ""]
    if overdue:
        out += ["| Сторінка | Власник | Статус | review_date | Прострочено, дн |", "|---|---|---|---|---|"]
        out += [f"| `{p}` | {o} | {s} | {d} | {n} |" for p, o, s, d, n in overdue]
    else:
        out.append("Немає — усі сторінки в межах циклу перегляду.")
    out += ["", "### Deprecated (переспрямування на актуальний документ)", ""]
    if deprecated:
        out += ["| Сторінка | Власник | superseded_by | Дата рішення |", "|---|---|---|---|"]
        out += [f"| `{p}` | {o} | `{t}` | {d} |" for p, o, t, d in deprecated]
    else:
        out.append("Немає.")
    if errors:
        out += ["", "### Помилки паспортів (блокують збірку)", ""]
        out += [f"- `{p}`: {msg}" for p, msg in errors]
    out += ["", "### Найближчі перегляди", "", "| Сторінка | Власник | review_date | Залишилось, дн |", "|---|---|---|---|"]
    out += [f"| `{p}` | {o} | {d} | {n} |" for p, o, _s, d, n in sorted(ok, key=lambda r: r[4])[:5]]
    return "\n".join(out) + "\n"


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--today", help="YYYY-MM-DD (за замовчуванням — сьогодні)")
    ap.add_argument("--fail-on-overdue", action="store_true")
    args = ap.parse_args()
    today = dt.date.fromisoformat(args.today) if args.today else dt.date.today()

    errors, overdue, deprecated, ok = audit(today)
    report = render(today, errors, overdue, deprecated, ok)
    print(report)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as fh:
            fh.write(report)
    if os.environ.get("GITHUB_ACTIONS"):
        for p, _o, _s, d, n in overdue:
            print(f"::warning file=docs/{p},title=review_date прострочено::{p}: review_date {d} (на {n} дн)")
        for p, msg in errors:
            print(f"::error file=docs/{p},title=Паспорт документа::{msg}")
    if errors or (args.fail_on_overdue and overdue):
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
