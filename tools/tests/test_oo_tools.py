"""Тести legacy-модуля oo_metrics.py і java_syntax_guard.py (CAPA-1/CAPA-2, INC-001; борг T-1).

Запуск: python3 -m unittest discover -s tools/tests -v
"""
import contextlib
import io
import sys
import tempfile
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))

import java_syntax_guard as guard  # noqa: E402
import oo_metrics  # noqa: E402

FX = Path(__file__).resolve().parent / "fixtures"
REPO = TOOLS.parent


def run_main(*args):
    err, out = io.StringIO(), io.StringIO()
    with tempfile.TemporaryDirectory() as tmp, contextlib.redirect_stderr(err), contextlib.redirect_stdout(out):
        code = oo_metrics.main([*map(str, args), "-o", tmp])
    return code, out.getvalue(), err.getvalue()


class OoMetricsExitCodes(unittest.TestCase):
    def test_java8_source_parses_and_passes(self):
        code, out, _ = run_main(FX / "java8", "--exclude", "", "--gate", "Plain")
        self.assertEqual(code, oo_metrics.EXIT_PASS)
        self.assertIn("OO GATE: PASS", out)

    def test_threshold_violation_is_exit_1(self):
        code, out, _ = run_main(FX / "java8", "--exclude", "", "--gate", "Missing")
        self.assertEqual(code, oo_metrics.EXIT_GATE_FAIL)
        self.assertIn("OO GATE: FAIL", out)

    def test_unparsable_source_is_tool_error_exit_3(self):
        code, out, err = run_main(FX / "record", "--exclude", "", "--gate", "Money")
        self.assertEqual(code, oo_metrics.EXIT_TOOL_ERROR)
        self.assertIn("OO TOOL ERROR", err)
        self.assertIn("Money.java", err)
        self.assertNotIn("OO GATE", out)           # жодного «вердикту» при збої інструмента

    def test_parse_failure_lists_every_file(self):
        with self.assertRaises(oo_metrics.ParseFailure) as ctx:
            oo_metrics.parse_project(FX)
        names = sorted(Path(p).name for p, _, _ in ctx.exception.failures)
        self.assertEqual(names, ["Modern.java", "Money.java"])


class JavaSyntaxGuard(unittest.TestCase):
    def rules(self, path):
        return {r for _, _, r, _, _ in guard.scan([path])}

    def test_java8_fixture_is_clean_despite_keywords_in_comments_and_strings(self):
        self.assertEqual(self.rules(FX / "java8"), set())

    def test_record_detected_with_line(self):
        findings = guard.scan([FX / "record"])
        self.assertEqual([(f[1], f[2]) for f in findings], [(3, "record")])

    def test_all_modern_constructs_detected(self):
        self.assertEqual(self.rules(FX / "modern"),
                         {"sealed", "text-block", "instanceof-pattern", "switch-arrow", "yield"})

    def test_guard_agrees_with_parser_on_fixtures(self):
        # усе, що guard пропускає, javalang має розібрати (інакше guard неповний)
        for d in ("java8", "record", "modern"):
            clean = not guard.scan([FX / d])
            try:
                oo_metrics.parse_project(FX / d)
                parsed = True
            except oo_metrics.ParseFailure:
                parsed = False
            self.assertEqual(clean, parsed, d)

    def test_project_sources_pass_guard(self):
        self.assertEqual(guard.scan([REPO / "src/main/java", REPO / "src/test/java"]), [])

    def test_exit_codes(self):
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(guard.main([str(FX / "java8")]), 0)
            self.assertEqual(guard.main([str(FX / "record")]), 1)


if __name__ == "__main__":
    unittest.main()
