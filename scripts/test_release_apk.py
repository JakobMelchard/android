import base64
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import textwrap
import unittest


class ReleaseApkTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        (self.root / "scripts").mkdir()
        shutil.copy2(Path(__file__).with_name("release-apk"), self.root / "scripts/release-apk")
        (self.root / "bin").mkdir()
        keytool = self.root / "bin/keytool"
        keytool.write_text("#!/bin/sh\nprintf 'upload, Jan 1, 2026, PrivateKeyEntry,\\n'\n")
        keytool.chmod(0o755)
        gradlew = self.root / "gradlew"
        gradlew.write_text('''#!/usr/bin/env python3
import json,os,pathlib,sys
app=os.environ["TEST_APP"]
prefix=app.upper()
key=pathlib.Path(os.environ[prefix+"_KEYSTORE_PATH"])
pathlib.Path("record.json").write_text(json.dumps({"args":sys.argv[1:],"key":key.read_text(),"path":str(key),"alias":os.environ[prefix+"_KEY_ALIAS"]}))
if os.environ.get("FAIL_BUILD"):sys.exit(7)
out=pathlib.Path("apps")/app/"build/outputs/apk/release"
out.mkdir(parents=True)
(out/(app+"-release.apk")).write_text("apk")
''')
        gradlew.chmod(0o755)
        self.env = os.environ.copy()
        self.env["PATH"] = str(self.root / "bin") + os.pathsep + self.env["PATH"]
        for app in ("fz", "pull"):
            self.env[app.upper() + "_KEYSTORE_B64"] = base64.b64encode((app + "-key").encode()).decode()
            self.env[app.upper() + "_KEYSTORE_PASSWORD"] = "test-password"

    def release(self, app="fz", version="1.2.3", **env):
        return subprocess.run([str(self.root / "scripts/release-apk"), app, version], cwd=self.root, env={**self.env, "TEST_APP":app, **env}, capture_output=True, text=True)

    def test_component_identity_version_and_key_are_isolated(self):
        for app in ("fz", "pull"):
            with self.subTest(app=app):
                result = self.release(app)
                self.assertEqual(result.returncode, 0, result.stderr)
                record = json.loads((self.root / "record.json").read_text())
                self.assertEqual(record["args"], ["--no-daemon", f":apps:{app}:assembleRelease", f"-P{app}.versionName=1.2.3", f"-P{app}.versionCode=10203"])
                self.assertEqual(record["key"], app + "-key")
                self.assertEqual(record["alias"], "upload")
                self.assertFalse(Path(record["path"]).exists())
                self.assertTrue((self.root / f"{app}-1.2.3.apk").exists())

    def test_workflow_maps_existing_infisical_credentials_to_each_component(self):
        workflow = Path(__file__).parent.parent / ".github/workflows/release.yml"
        build = workflow.read_text().split("      - name: build selected component\n", 1)[1]
        command = build.split("        run: |\n", 1)[1].split("      - uses:", 1)[0]
        command = textwrap.dedent(command)
        for app in ("fz", "pull"):
            with self.subTest(app=app):
                env = {k: v for k, v in self.env.items() if not k.startswith(("FZ_", "PULL_", "ANDROID_"))}
                env.update({
                    "APP": app, "VERSION": "1.2.3", "TEST_APP": app,
                    "ANDROID_KEYSTORE_B64": base64.b64encode((app + "-original-key").encode()).decode(),
                    "ANDROID_KEYSTORE_PASSWORD": "test-password", "ANDROID_KEY_ALIAS": app + "-original-alias",
                })
                result = subprocess.run(["bash", "-euc", command], cwd=self.root, env=env, capture_output=True, text=True)
                self.assertEqual(result.returncode, 0, result.stderr)
                record = json.loads((self.root / "record.json").read_text())
                self.assertEqual(record["key"], app + "-original-key")
                self.assertEqual(record["alias"], app + "-original-alias")
                self.assertEqual(record["args"][1], f":apps:{app}:assembleRelease")
                self.assertFalse(Path(record["path"]).exists())

    def test_explicit_alias_is_retained(self):
        result = self.release(PULL_KEY_ALIAS="unused", FZ_KEY_ALIAS="existing-fz")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads((self.root / "record.json").read_text())["alias"], "existing-fz")

    def test_sibling_key_cannot_satisfy_missing_selected_key(self):
        result = self.release(FZ_KEYSTORE_B64="")
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.root / "record.json").exists())

    def test_unknown_app_rejected(self):
        self.assertNotEqual(self.release("other").returncode, 0)
        self.assertFalse((self.root / "record.json").exists())

    def test_ambiguous_or_invalid_versions_rejected(self):
        for version in ("1.100.0", "1.0.100", "bad", "01.2.3", "1.2.3;touch nope"):
            with self.subTest(version=version):
                self.assertNotEqual(self.release(version=version).returncode, 0)
        self.assertFalse((self.root / "record.json").exists())

    def test_key_removed_after_build_failure(self):
        result = self.release(FAIL_BUILD="1")
        self.assertEqual(result.returncode, 7)
        record = json.loads((self.root / "record.json").read_text())
        self.assertFalse(Path(record["path"]).exists())
        self.assertFalse((self.root / "fz-1.2.3.apk").exists())


if __name__ == "__main__":
    unittest.main()
