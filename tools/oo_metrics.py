#!/usr/bin/env python3
"""
OO-metrics analyzer for Java sources: Chidamber-Kemerer (CK), Lorenz-Kidd and MOOD (Abreu).

SonarQube (since 4.x) no longer computes CBO / LCOM4 / DIT / RFC, so this tool fills the gap
and produces machine-readable JSON + a Coupling Matrix (DSM) + Markdown tables for the report.

Usage:
    pip install javalang
    python tools/oo_metrics.py src/main/java -o reports/oo/baseline
    python tools/oo_metrics.py src/main/java -o reports/oo/after --gate "PricingService,OrderPlacementService" \
           --max-cbo 8 --max-lcom 0.2              # exit code 1 => FAIL

Metric definitions (all computed for project types only, JDK types are ignored):
    WMC   - Weighted Methods per Class = sum of McCabe cyclomatic complexity of methods and constructors
    CBO   - Coupling Between Objects = number of distinct project types this class uses (efferent, Ce)
    Ca    - afferent coupling = number of project types that use this class
    RFC   - Response For a Class = own methods + distinct methods invoked from them
    LCOM  - Henderson-Sellers LCOM* in [0..1] (0 = ideal cohesion). Instance methods/fields,
            trivial accessors (getX/setX/isX) excluded
    LCOM4 - number of connected components of the method graph (1 = cohesive, >1 = class should be split);
            constructors and trivial accessors are excluded (as in the historical Sonar LCOM4 plugin)
    DIT   - Depth of Inheritance Tree, java.lang.Object = level 0 (class without "extends" => DIT = 1)
    NOC   - Number Of Children
    NMO   - Lorenz-Kidd: number of overridden (concrete) methods of project ancestors
    NMA   - Lorenz-Kidd: number of new methods added
    PIM   - Lorenz-Kidd: Public Instance Methods incl. inherited from project ancestors
    MOOD  - MHF, AHF (hiding), MIF, AIF (inheritance), COF (coupling), PF (polymorphism)
"""
from __future__ import annotations

import argparse
import html
import json
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path

try:
    import javalang
    from javalang import tree as T
except ImportError:  # pragma: no cover
    sys.exit("javalang is required:  pip install javalang")


# --------------------------------------------------------------------------- model

@dataclass
class MethodInfo:
    name: str
    params: int
    modifiers: set
    annotations: list
    node: object
    is_constructor: bool = False
    cyclomatic: int = 1
    cognitive: int = 0
    fields_used: set = field(default_factory=set)
    calls_own: set = field(default_factory=set)
    calls_all: set = field(default_factory=set)

    @property
    def sig(self):
        return f"{self.name}/{self.params}"

    @property
    def is_static(self):
        return "static" in self.modifiers

    @property
    def is_public(self):
        return "public" in self.modifiers


@dataclass
class TypeInfo:
    name: str
    kind: str                     # class | interface | enum
    package: str
    file: str
    node: object
    extends: str | None = None
    implements: list = field(default_factory=list)
    fields: dict = field(default_factory=dict)   # name -> modifiers
    methods: list = field(default_factory=list)
    refs: set = field(default_factory=set)
    lines: int = 0
    is_abstract: bool = False


# --------------------------------------------------------------------------- helpers

def iter_children(node):
    if isinstance(node, T.Node):
        for child in node.children:
            yield from _flatten(child)


def _flatten(x):
    if isinstance(x, T.Node):
        yield x
    elif isinstance(x, (list, tuple, set)):
        for item in x:
            yield from _flatten(item)


def walk(node, skip_types=True):
    """Pre-order traversal that does not descend into nested type declarations."""
    stack = [node]
    first = True
    while stack:
        n = stack.pop()
        if not first and skip_types and isinstance(n, (T.ClassDeclaration, T.InterfaceDeclaration, T.EnumDeclaration)):
            continue
        first = False
        yield n
        stack.extend(reversed(list(iter_children(n))))


def type_names(ref) -> list:
    """All simple names in a (possibly qualified, generic) reference type."""
    out = []
    if ref is None:
        return out
    if isinstance(ref, T.ReferenceType):
        out.extend(ref.name.split("."))
        for arg in ref.arguments or []:
            if isinstance(arg, T.TypeArgument):
                out.extend(type_names(arg.type))
        if ref.sub_type is not None:
            out.extend(type_names(ref.sub_type))
    return out


# --------------------------------------------------------------------------- complexity

def cyclomatic(body_node) -> int:
    cc = 1
    for n in walk(body_node):
        if isinstance(n, (T.IfStatement, T.ForStatement, T.WhileStatement, T.DoStatement,
                          T.CatchClause, T.TernaryExpression)):
            cc += 1
        elif isinstance(n, T.SwitchStatementCase):
            cc += len([c for c in (n.case or []) if c != "default"])
        elif isinstance(n, T.BinaryOperation) and n.operator in ("&&", "||"):
            cc += 1
    return cc


def _logical_ops(expr, acc):
    """In-order sequence of && / || operators of a boolean expression (for cognitive complexity)."""
    if isinstance(expr, T.BinaryOperation) and expr.operator in ("&&", "||"):
        _logical_ops(expr.operandl, acc)
        acc.append(expr.operator)
        _logical_ops(expr.operandr, acc)


def _bool_sequences(node) -> int:
    total = 0
    # find roots: logical BinaryOperation whose parent is not a logical BinaryOperation
    roots = []

    def visit(n, parent_logical):
        is_logical = isinstance(n, T.BinaryOperation) and n.operator in ("&&", "||")
        if is_logical and not parent_logical:
            roots.append(n)
        for c in iter_children(n):
            if isinstance(c, (T.ClassDeclaration, T.InterfaceDeclaration, T.EnumDeclaration)):
                continue
            visit(c, is_logical)

    visit(node, False)
    for r in roots:
        ops = []
        _logical_ops(r, ops)
        if ops:
            total += 1 + sum(1 for a, b in zip(ops, ops[1:]) if a != b)
    return total


def cognitive(body_node) -> int:
    """Approximation of SonarSource Cognitive Complexity (G. Ann Campbell, 2017)."""
    score = 0

    def stmt(n, nesting, is_else_if=False):
        nonlocal score
        if n is None:
            return
        if isinstance(n, T.IfStatement):
            score += 1 if is_else_if else 1 + nesting
            expr(n.condition, nesting)
            stmt(n.then_statement, nesting + 1)
            if isinstance(n.else_statement, T.IfStatement):
                stmt(n.else_statement, nesting, is_else_if=True)
            elif n.else_statement is not None:
                score += 1
                stmt(n.else_statement, nesting + 1)
            return
        if isinstance(n, (T.ForStatement, T.WhileStatement, T.DoStatement)):
            score += 1 + nesting
            for c in iter_children(n):
                if c is n.body:
                    continue
                expr(c, nesting)
            stmt(n.body, nesting + 1)
            return
        if isinstance(n, T.SwitchStatement):
            score += 1 + nesting
            expr(n.expression, nesting)
            for case in n.cases or []:
                for s in case.statements or []:
                    stmt(s, nesting + 1)
            return
        if isinstance(n, T.TryStatement):
            for s in n.block or []:
                stmt(s, nesting)
            for c in n.catches or []:
                score += 1 + nesting
                for s in c.block or []:
                    stmt(s, nesting + 1)
            for s in n.finally_block or []:
                stmt(s, nesting)
            return
        if isinstance(n, (T.ClassDeclaration, T.InterfaceDeclaration, T.EnumDeclaration)):
            return
        if isinstance(n, T.Statement) or isinstance(n, T.BlockStatement):
            for c in iter_children(n):
                if isinstance(c, (T.Statement, T.BlockStatement)) or isinstance(c, T.LocalVariableDeclaration):
                    stmt(c, nesting)
                else:
                    expr(c, nesting)
            return
        expr(n, nesting)

    def expr(n, nesting):
        nonlocal score
        if n is None or not isinstance(n, T.Node):
            return
        if isinstance(n, T.TernaryExpression):
            score += 1 + nesting
            expr(n.condition, nesting)
            expr(n.if_true, nesting + 1)
            expr(n.if_false, nesting + 1)
            return
        if isinstance(n, T.LambdaExpression):
            body = n.body
            if isinstance(body, list):
                for s in body:
                    stmt(s, nesting + 1)
            else:
                expr(body, nesting + 1)
            return
        if isinstance(n, T.BinaryOperation) and n.operator in ("&&", "||"):
            score += _bool_sequences(n)
            # still look for ternaries/lambdas inside operands
            for leaf in walk(n):
                if isinstance(leaf, (T.TernaryExpression, T.LambdaExpression)):
                    expr(leaf, nesting)
            return
        if isinstance(n, (T.ClassCreator,)) and n.body:
            return
        for c in iter_children(n):
            if isinstance(c, (T.Statement, T.BlockStatement)):
                stmt(c, nesting)
            else:
                expr(c, nesting)

    for s in (body_node.body or []):
        stmt(s, 0)
    return score


# --------------------------------------------------------------------------- parsing

def parse_project(src_root: Path, exclude=()) -> dict:
    types: dict[str, TypeInfo] = {}
    for path in sorted(src_root.rglob("*.java")):
        if path.stem in exclude:
            continue
        text = path.read_text(encoding="utf-8")
        cu = javalang.parse.parse(text)
        pkg = cu.package.name if cu.package else ""
        for _, decl in cu.filter(T.TypeDeclaration):
            kind = ("interface" if isinstance(decl, T.InterfaceDeclaration)
                    else "enum" if isinstance(decl, T.EnumDeclaration) else "class")
            try:
                rel = path.resolve().relative_to(Path.cwd().resolve()).as_posix()
            except ValueError:
                rel = path.as_posix()
            info = TypeInfo(decl.name, kind, pkg, rel, decl)
            info.is_abstract = "abstract" in (decl.modifiers or set())
            if kind == "class" and decl.extends is not None:
                info.extends = decl.extends.name.split(".")[-1]
            impl = decl.implements if kind != "interface" else decl.extends
            info.implements = [r.name.split(".")[-1] for r in (impl or [])]
            info.lines = text.count("\n") + 1
            types[decl.name] = info
    return types


def is_accessor(m: MethodInfo, fields: dict) -> bool:
    body = getattr(m.node, "body", None) or []
    if len(body) != 1:
        return False
    s = body[0]
    if m.name.startswith(("get", "is")) and m.params == 0 and isinstance(s, T.ReturnStatement):
        e = s.expression
        if isinstance(e, T.MemberReference) and e.member in fields and not e.selectors:
            return True
        if isinstance(e, T.This) and e.selectors and isinstance(e.selectors[0], T.MemberReference):
            return e.selectors[0].member in fields
    if m.name.startswith("set") and m.params == 1 and isinstance(s, T.StatementExpression):
        return isinstance(s.expression, T.Assignment)
    return False


def analyze_type(info: TypeInfo, project: set):
    decl = info.node
    body = decl.body if not isinstance(decl, T.EnumDeclaration) else (decl.body.declarations if decl.body else [])
    for member in body or []:
        if isinstance(member, T.FieldDeclaration):
            for d in member.declarators:
                info.fields[d.name] = set(member.modifiers or set())
    method_names = set()
    for member in body or []:
        if isinstance(member, (T.MethodDeclaration, T.ConstructorDeclaration)):
            m = MethodInfo(member.name, len(member.parameters or []), set(member.modifiers or set()),
                           [a.name for a in (member.annotations or [])], member,
                           is_constructor=isinstance(member, T.ConstructorDeclaration))
            if info.kind == "interface" and not m.is_constructor:
                m.modifiers.add("public")
            info.methods.append(m)
            method_names.add(member.name)

    instance_fields = {f for f, mods in info.fields.items() if "static" not in mods}
    for m in info.methods:
        node = m.node
        m.cyclomatic = cyclomatic(node)
        m.cognitive = cognitive(node) if getattr(node, "body", None) else 0
        locals_ = {p.name for p in (node.parameters or [])}
        for n in walk(node):
            if isinstance(n, T.VariableDeclarator):
                locals_.add(n.name)
        for n in walk(node):
            if isinstance(n, T.MemberReference):
                if n.member in instance_fields and (not n.qualifier or n.qualifier == "this") and n.member not in locals_:
                    m.fields_used.add(n.member)
                if n.qualifier in instance_fields and n.qualifier not in locals_:
                    m.fields_used.add(n.qualifier)
            elif isinstance(n, T.This):
                for sel in n.selectors or []:
                    if isinstance(sel, T.MemberReference) and sel.member in instance_fields:
                        m.fields_used.add(sel.member)
                    if isinstance(sel, T.MethodInvocation) and sel.member in method_names:
                        m.calls_own.add(sel.member)
            elif isinstance(n, T.MethodInvocation):
                if n.qualifier in instance_fields and n.qualifier not in locals_:
                    m.fields_used.add(n.qualifier)
                if not n.qualifier and n.member in method_names:
                    m.calls_own.add(n.member)
                m.calls_all.add(f"{n.qualifier or ''}.{n.member}")
            elif isinstance(n, T.MethodReference):
                if isinstance(n.expression, T.MemberReference) and n.expression.member == info.name:
                    m.calls_own.add(n.method.member)

    # fields used indirectly through calls of own methods (e.g. markPaid -> requireStatus -> status)
    by_name = defaultdict(list)
    for m in info.methods:
        by_name[m.name].append(m)
    changed = True
    while changed:
        changed = False
        for m in info.methods:
            for callee in list(m.calls_own):
                for target in by_name.get(callee, []):
                    if not target.fields_used <= m.fields_used:
                        m.fields_used |= target.fields_used
                        changed = True

    # efferent references
    for n in walk(decl):
        if isinstance(n, T.ReferenceType):
            info.refs.update(type_names(n))
        elif isinstance(n, (T.MethodInvocation, T.MemberReference)) and n.qualifier:
            info.refs.update(n.qualifier.split("."))
        elif isinstance(n, T.MethodReference) and isinstance(n.expression, T.MemberReference):
            info.refs.add(n.expression.member)
    if info.extends:
        info.refs.add(info.extends)
    info.refs.update(info.implements)
    info.refs = {r for r in info.refs if r in project and r != info.name}


# --------------------------------------------------------------------------- metrics

def lcom_hs(info: TypeInfo) -> float:
    inst_fields = {f for f, mods in info.fields.items() if "static" not in mods}
    methods = [m for m in info.methods if not m.is_constructor and not m.is_static
               and not is_accessor(m, info.fields)]
    m, a = len(methods), len(inst_fields)
    if m <= 1 or a == 0:
        return 0.0
    mu = sum(sum(1 for meth in methods if f in meth.fields_used) for f in inst_fields) / a
    return round(max(0.0, (mu - m) / (1 - m)), 3)


def lcom4(info: TypeInfo) -> int:
    methods = [m for m in info.methods if not m.is_constructor and not is_accessor(m, info.fields)]
    if not methods:
        return 0
    idx = {m.name: i for i, m in enumerate(methods)}
    parent = list(range(len(methods)))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    def union(a, b):
        parent[find(a)] = find(b)

    for i, a in enumerate(methods):
        for callee in a.calls_own:
            if callee in idx:
                union(i, idx[callee])
        for j in range(i + 1, len(methods)):
            if a.fields_used & methods[j].fields_used:
                union(i, j)
    return len({find(i) for i in range(len(methods))})


def ancestors(name, types):
    chain, cur = [], types[name].extends
    while cur and cur in types and cur not in chain:
        chain.append(cur)
        cur = types[cur].extends
    return chain


def compute(types: dict) -> dict:
    project = set(types)
    for info in types.values():
        analyze_type(info, project)

    afferent = defaultdict(set)
    for info in types.values():
        for r in info.refs:
            afferent[r].add(info.name)
    children = defaultdict(list)
    for info in types.values():
        if info.extends in types:
            children[info.extends].append(info.name)

    rows = []
    for name, info in sorted(types.items()):
        own = [m for m in info.methods if not m.is_constructor]
        anc = ancestors(name, types)
        anc_methods = {}
        for a in anc:
            for m in types[a].methods:
                if not m.is_constructor and "private" not in m.modifiers:
                    anc_methods.setdefault(m.sig, m)
        overridden = [m.sig for m in own if m.sig in anc_methods and "abstract" not in anc_methods[m.sig].modifiers]
        new_methods = [m.sig for m in own if m.sig not in anc_methods]
        public_sigs = {m.sig for m in own if m.is_public and not m.is_static}
        public_sigs |= {s for s, m in anc_methods.items() if m.is_public and not m.is_static}
        invoked = set().union(*[m.calls_all for m in own]) if own else set()
        rfc = len(info.methods) + len({c for c in invoked if c.split(".")[-1] not in {m.name for m in info.methods}})
        worst = max(own, key=lambda m: m.cognitive, default=None)
        rows.append({
            "class": name,
            "kind": info.kind,
            "package": info.package,
            "file": info.file,
            "WMC": sum(m.cyclomatic for m in info.methods),
            "CBO": len(info.refs),
            "Ca": len(afferent[name]),
            "RFC": rfc,
            "LCOM": lcom_hs(info),
            "LCOM4": lcom4(info),
            "DIT": (len(anc) + 1) if info.kind != "interface" else 0,
            "NOC": len(children[name]),
            "NOM": len(own),
            "NOF": len(info.fields),
            "NMO": len(overridden),
            "NMA": len(new_methods),
            "PIM": len(public_sigs),
            "overridden": overridden,
            "maxCognitive": worst.cognitive if worst else 0,
            "maxCognitiveMethod": worst.name if worst else None,
            "couplesTo": sorted(info.refs),
            "usedBy": sorted(afferent[name]),
            "methods": [{"name": m.name, "params": m.params, "cyclomatic": m.cyclomatic,
                         "cognitive": m.cognitive} for m in info.methods],
        })
    return {"classes": rows, "mood": mood(types, children)}


def mood(types: dict, children) -> dict:
    classes = [t for t in types.values() if t.kind != "interface"]
    m_total = m_hidden = a_total = a_hidden = 0
    m_inherited = m_available = a_inherited = a_available = 0
    poly_num = poly_den = 0
    for info in classes:
        own = [m for m in info.methods if not m.is_constructor]
        m_total += len(own)
        m_hidden += sum(1 for m in own if not m.is_public)
        a_total += len(info.fields)
        a_hidden += sum(1 for mods in info.fields.values() if "public" not in mods)
        own_sigs = {m.sig for m in own}
        inh_m, inh_a = set(), set()
        for a in ancestors(info.name, types):
            inh_m |= {m.sig for m in types[a].methods if not m.is_constructor and "private" not in m.modifiers}
            inh_a |= {f for f, mods in types[a].fields.items() if "private" not in mods}
        inh_m -= own_sigs
        m_inherited += len(inh_m)
        m_available += len(own_sigs) + len(inh_m)
        a_inherited += len(inh_a)
        a_available += len(info.fields) + len(inh_a)
        descendants = _descendants(info.name, children)
        anc_sigs = set()
        for a in ancestors(info.name, types):
            anc_sigs |= {m.sig for m in types[a].methods if not m.is_constructor}
        new = own_sigs - anc_sigs
        poly_den += len(new) * len(descendants)
        poly_num += len(own_sigs & anc_sigs)
    tc = len(types)
    couplings = 0
    for info in types.values():
        anc = set(ancestors(info.name, types))
        couplings += len([r for r in info.refs if r not in anc])
    pct = lambda a, b: round(100.0 * a / b, 1) if b else 0.0
    return {
        "MHF": pct(m_hidden, m_total), "AHF": pct(a_hidden, a_total),
        "MIF": pct(m_inherited, m_available), "AIF": pct(a_inherited, a_available),
        "COF": pct(couplings, tc * tc - tc), "PF": pct(poly_num, poly_den),
        "classes": tc, "methods": m_total, "attributes": a_total, "couplings": couplings,
    }


def _descendants(name, children):
    out, stack = [], list(children.get(name, []))
    while stack:
        c = stack.pop()
        out.append(c)
        stack.extend(children.get(c, []))
    return out


# --------------------------------------------------------------------------- output

def write_outputs(result: dict, out: Path, focus: list):
    out.mkdir(parents=True, exist_ok=True)
    (out / "oo-metrics.json").write_text(json.dumps(result, indent=2, ensure_ascii=False), encoding="utf-8")
    rows = result["classes"]
    cols = ["class", "WMC", "CBO", "Ca", "RFC", "LCOM", "LCOM4", "DIT", "NOC", "NMO", "PIM", "maxCognitive"]
    md = ["# OO metrics (CK / Lorenz-Kidd / MOOD)", "", f"Generated: {result['generatedAt']}", "",
          "| " + " | ".join(cols) + " |", "|" + "---|" * len(cols)]
    for r in sorted(rows, key=lambda r: -r["CBO"]):
        md.append("| " + " | ".join(str(r[c]) for c in cols) + " |")
    if focus:
        md += ["", "## Focus classes", "", "| " + " | ".join(cols) + " |", "|" + "---|" * len(cols)]
        for r in rows:
            if r["class"] in focus:
                md.append("| " + " | ".join(str(r[c]) for c in cols) + " |")
    m = result["mood"]
    md += ["", "## MOOD (system level)", "", "| MHF | AHF | MIF | AIF | COF | PF |", "|---|---|---|---|---|---|",
           f"| {m['MHF']}% | {m['AHF']}% | {m['MIF']}% | {m['AIF']}% | {m['COF']}% | {m['PF']}% |"]
    (out / "oo-metrics.md").write_text("\n".join(md) + "\n", encoding="utf-8")
    write_matrix(rows, out)


def write_matrix(rows: list, out: Path):
    names = [r["class"] for r in sorted(rows, key=lambda r: (r["package"], r["class"]))]
    deps = {r["class"]: set(r["couplesTo"]) for r in rows}
    by = {r["class"]: r for r in rows}
    epicenter = max(rows, key=lambda r: r["CBO"])["class"]
    # CSV
    csv = ["client\\supplier;" + ";".join(names) + ";CBO"]
    for a in names:
        csv.append(a + ";" + ";".join("1" if b in deps[a] else "" for b in names) + f";{by[a]['CBO']}")
    csv.append("Ca;" + ";".join(str(by[b]["Ca"]) for b in names) + ";")
    (out / "coupling-matrix.csv").write_text("\n".join(csv) + "\n", encoding="utf-8")
    # HTML heatmap
    head = "".join(f'<th class="v"><div>{html.escape(n)}</div></th>' for n in names)
    body = []
    for a in names:
        cls = ' class="epi"' if a == epicenter else ""
        cells = "".join(f'<td class="{"on" if b in deps[a] else ("self" if a == b else "")}"></td>' for b in names)
        cbo = by[a]["CBO"]
        body.append(f'<tr{cls}><th>{html.escape(a)}</th>{cells}<td class="tot {"bad" if cbo > 8 else ""}">{cbo}</td></tr>')
    ca = "".join(f'<td class="tot">{by[b]["Ca"]}</td>' for b in names)
    doc = f"""<!DOCTYPE html><html lang="uk"><head><meta charset="utf-8"><title>Coupling Matrix</title>
<style>
body{{font-family:system-ui,sans-serif;margin:24px;color:#1f2328}} table{{border-collapse:collapse;font-size:12px}}
th,td{{border:1px solid #d0d7de;padding:0}} td{{width:20px;height:20px}} th{{text-align:right;padding:2px 6px;font-weight:500}}
th.v{{height:150px;vertical-align:bottom;padding:4px 0}} th.v div{{writing-mode:vertical-rl;transform:rotate(180deg)}}
td.on{{background:#cf222e}} td.self{{background:#eaeef2}} td.tot{{width:auto;padding:2px 6px;text-align:center}}
td.bad{{background:#ffebe9;font-weight:700;color:#cf222e}} tr.epi th{{background:#fff8c5;font-weight:700}}
p.note{{max-width:720px;color:#57606a}}</style></head><body>
<h2>Coupling Matrix (DSM): рядок = клієнт, стовпець = постачальник</h2>
<p class="note">Червона клітинка — клас у рядку використовує клас у стовпці. Останній стовпець — CBO (efferent),
останній рядок — Ca (afferent). Жовтим виділено епіцентр зачеплення: <b>{html.escape(epicenter)}</b> (CBO = {by[epicenter]['CBO']}).</p>
<table><tr><th></th>{head}<th>CBO</th></tr>{''.join(body)}<tr><th>Ca</th>{ca}<td></td></tr></table></body></html>"""
    (out / "coupling-matrix.html").write_text(doc, encoding="utf-8")
    # Mermaid dependency graph
    mer = ["flowchart LR"]
    for a in names:
        for b in sorted(deps[a]):
            mer.append(f"    {a} --> {b}")
    mer.append(f"    style {epicenter} fill:#ffebe9,stroke:#cf222e,stroke-width:3px")
    (out / "dependency-graph.mmd").write_text("\n".join(mer) + "\n", encoding="utf-8")


# --------------------------------------------------------------------------- cli

def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("src", type=Path, help="source root, e.g. src/main/java")
    ap.add_argument("-o", "--out", type=Path, default=Path("reports/oo"))
    ap.add_argument("--exclude", default="ShopApplication",
                    help="comma separated file stems to skip (composition root, like sonar.exclusions)")
    ap.add_argument("--focus", default="", help="comma separated classes to show separately")
    ap.add_argument("--gate", default="", help="comma separated classes checked against thresholds")
    ap.add_argument("--max-cbo", type=int, default=8)
    ap.add_argument("--max-lcom", type=float, default=0.2)
    ap.add_argument("--max-dit", type=int, default=None)
    ap.add_argument("--max-nmo", type=int, default=None)
    args = ap.parse_args(argv)

    exclude = {c.strip() for c in args.exclude.split(",") if c.strip()}
    result = compute(parse_project(args.src, exclude))
    result["generatedAt"] = datetime.now(timezone.utc).isoformat(timespec="seconds")
    result["source"] = str(args.src)
    focus = [c.strip() for c in args.focus.split(",") if c.strip()]
    write_outputs(result, args.out, focus)

    m = result["mood"]
    print(f"classes={m['classes']}  MHF={m['MHF']}%  AHF={m['AHF']}%  MIF={m['MIF']}%  AIF={m['AIF']}%  "
          f"COF={m['COF']}%  PF={m['PF']}%")
    top = max(result["classes"], key=lambda r: r["CBO"])
    print(f"max CBO: {top['class']} CBO={top['CBO']} LCOM={top['LCOM']} LCOM4={top['LCOM4']} WMC={top['WMC']}")
    print(f"written: {args.out}/oo-metrics.json, oo-metrics.md, coupling-matrix.html|csv, dependency-graph.mmd")

    gate = [c.strip() for c in args.gate.split(",") if c.strip()]
    if not gate:
        return 0
    by = {r["class"]: r for r in result["classes"]}
    failed = False
    for c in gate:
        r = by.get(c)
        if r is None:
            print(f"  ?    {c}: not found")
            failed = True
            continue
        problems = []
        if r["CBO"] > args.max_cbo:
            problems.append(f"CBO {r['CBO']} > {args.max_cbo}")
        if r["LCOM"] > args.max_lcom:
            problems.append(f"LCOM {r['LCOM']} > {args.max_lcom}")
        if args.max_dit is not None and r["DIT"] > args.max_dit:
            problems.append(f"DIT {r['DIT']} > {args.max_dit}")
        if args.max_nmo is not None and r["NMO"] > args.max_nmo:
            problems.append(f"NMO {r['NMO']} > {args.max_nmo}")
        status = "FAIL" if problems else "PASS"
        failed |= bool(problems)
        print(f"  {status} {c}: CBO={r['CBO']} LCOM={r['LCOM']} LCOM4={r['LCOM4']} DIT={r['DIT']} NMO={r['NMO']}"
              + (f"  <- {'; '.join(problems)}" if problems else ""))
    print("OO GATE:", "FAIL" if failed else "PASS")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
