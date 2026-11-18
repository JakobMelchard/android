import json
import os
from pathlib import Path
import subprocess
import unittest


class ReleasePlanTest(unittest.TestCase):
    def plan(self, paths, releases):
        return subprocess.run(
            ["python3", str(Path(__file__).with_name("release-plan"))],
            env={**os.environ, "RETRY_APP": "auto", "PATHS_RELEASED": json.dumps(paths), "RELEASES": json.dumps(releases)},
            capture_output=True,
            text=True,
        )

    def test_no_release(self):
        result = self.plan([], {})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(result.stdout), {"include": []})

    def test_pull_selects_only_its_own_metadata(self):
        releases = {".": {"tag_name": "pull-v1.2.0", "version": "1.2.0"}}
        result = self.plan(list(releases), releases)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(
            json.loads(result.stdout)["include"],
            [{"app": "pull", "prefix": "PULL", **releases["."]}],
        )

    def test_inconsistent_unknown_or_unsafe_metadata_fails_closed(self):
        valid = {"tag_name": "pull-v1.2.3", "version": "1.2.3"}
        cases = [
            (["apps/pull"], {"apps/pull": valid}),
            (["."], {}),
            ([], {".": valid}),
            ([".", "."], {".": valid}),
            ({".": valid}, {".": valid}),
            ([1], {}),
            (["."], {".": None}),
            (["."], {".": {"tag_name": "other-v1.2.3", "version": "1.2.3"}}),
        ]
        for version in ("01.2.3", "1.100.0", "1.0.100", "210001.0.0", "1.2.3;echo bad", "1.2.3\n", 123):
            cases.append((["."], {".": {"tag_name": f"pull-v{version}", "version": version}}))
        for paths, releases in cases:
            with self.subTest(paths=paths, releases=releases):
                result = self.plan(paths, releases)
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(result.stdout, "")

    def test_malformed_json_rejected(self):
        result = subprocess.run(
            ["python3", str(Path(__file__).with_name("release-plan"))],
            env={**os.environ, "RETRY_APP": "auto", "PATHS_RELEASED": "[", "RELEASES": "{}"},
            capture_output=True, text=True,
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(result.stdout, "")

    def test_retry_builds_only_a_valid_existing_component_tag(self):
        for app in ("pull",):
            result = subprocess.run(
                ["python3", str(Path(__file__).with_name("release-plan"))],
                env={**os.environ, "RETRY_APP": app, "RETRY_VERSION": "0.1.9"}, capture_output=True, text=True,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(
                json.loads(result.stdout)["include"],
                [{"app": app, "prefix": app.upper(), "tag_name": app + "-v0.1.9", "version": "0.1.9"}],
            )

    def test_removed_component_is_rejected_in_automatic_and_retry_plans(self):
        result = self.plan(["apps/fz"], {"apps/fz": {"tag_name": "fz-v1.2.3", "version": "1.2.3"}})
        self.assertNotEqual(result.returncode, 0)
        result = subprocess.run(
            ["python3", str(Path(__file__).with_name("release-plan"))],
            env={**os.environ, "RETRY_APP": "fz", "RETRY_VERSION": "1.2.3"}, capture_output=True, text=True,
        )
        self.assertNotEqual(result.returncode, 0)
