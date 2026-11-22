#!/usr/bin/env python3
"""Java syntax guard (CAPA-2, INC-001).

The OO gates depend on javalang 0.13.0, which parses Java syntax only up to Java 8 (ADR-0004).
This guard turns the prose rule from coding-standards §4 into an executable check that runs
BEFORE the gates and gives a clear verdict with file:line and a hint, instead of a parser crash.

Usage:
    python3 tools/java_syntax_guard.py [src_root ...]      # default: src/main/java src/test/java
Exit codes: 0 PASS, 1 FAIL (unsupported construct found).
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

# (id, regex, what, hint). Regexes run on source with comments and string literals blanked out.
RULES = [
    ("record", re.compile(r"(?m)^[ \t]*(?:(?:public|protected|private|static|final)\s+)*record\s+[A-Z]\w*\s*[(<]"),
     "record declaration (Java 16)", "use a final class with private final fields and accessors"),
    ("switch-arrow", re.compile(r"\bcase\b[^:;{}]*?->"),
     "switch rule 'case ... ->' (Java 14)", "use classic 'case X:' with break/return"),
    ("yield", re.compile(r"(?m)^[ \t]*yield\b"),
     "'yield' in switch expression (Java 14)", "return from a method or assign in classic switch"),
    ("text-block", re.compile(r'"""'),
     "text block \"\"\" (Java 15)", "concatenate string literals or use String.join"),
    ("sealed", re.compile(r"\b(?:sealed|non-sealed)\s+(?:abstract\s+)?(?:class|interface)\b|\bpermits\s+[A-Z]"),
     "sealed type / permits (Java 17)", "use a package-private constructor or final classes"),
    ("instanceof-pattern", re.compile(r"\binstanceof\s+(?:final\s+)?[A-Z][\w.<>?,\s]*?\s+[a-z_]\w*\s*[)&|;]"),
     "pattern matching for instanceof (Java 16)", "test with instanceof, then cast explicitly"),
]

_COMMENT_OR_STRING = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])*\'', re.S)


def _blank(m: re.Match) -> str:
    text = m.group(0)
    if text.startswith('"""'):
        return text
    return "".join("\n" if ch == "\n" else " " for ch in text)


def strip_comments_and_strings(src: str) -> str:
    # text blocks are kept visible on purpose (rule "text-block"); everything else is blanked
    out, pos = [], 0
    for m in _COMMENT_OR_STRING.finditer(src):
        start = m.start()
        if src.startswith('"""', start):
            continue
        out.append(src[pos:start])
        out.append(_blank(m))
        pos = m.end()
    out.append(src[pos:])
    return "".join(out)


def scan_text(src: str):
    clean = strip_comments_and_strings(src)
    for rule_id, rx, what, hint in RULES:
        for m in rx.finditer(clean):
            yield clean.count("\n", 0, m.start()) + 1, rule_id, what, hint


def scan(roots):
    findings = []
    for root in roots:
        root = Path(root)
        if not root.exists():
            continue
        for path in sorted(root.rglob("*.java")):
            for line, rule_id, what, hint in scan_text(path.read_text(encoding="utf-8")):
                findings.append((path.as_posix(), line, rule_id, what, hint))
    return findings


def main(argv=None) -> int:
    roots = (argv if argv is not None else sys.argv[1:]) or ["src/main/java", "src/test/java"]
    findings = scan(roots)
    files = {f[0] for f in findings}
    for path, line, rule_id, what, hint in findings:
        print(f"  {path}:{line}  [{rule_id}] {what} -> {hint}")
    if findings:
        print(f"JAVA SYNTAX GUARD: FAIL — {len(findings)} construct(s) in {len(files)} file(s) are not supported "
              f"by javalang 0.13.0 (OO gates, ADR-0004, coding-standards §4)")
        return 1
    print(f"JAVA SYNTAX GUARD: PASS — no Java 9+ constructs unsupported by the OO gates in {', '.join(roots)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
