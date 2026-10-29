import os
from pathlib import Path
import subprocess
import tempfile
import textwrap
import unittest


class ReleaseConfigTest(unittest.TestCase):
    def test_planning_requires_the_signing_identity_before_producing_outputs(self):
        root = Path(__file__).parent.parent
        workflow = (root / '.github/workflows/release.yml').read_text()
        command = textwrap.dedent(workflow.split('      - id: plan\n', 1)[1].split('        run: |\n', 1)[1].split('\n  apk:', 1)[0])
        with tempfile.TemporaryDirectory() as temp:
            for identity in ('', 'configured'):
                with self.subTest(identity=identity):
                    output = Path(temp) / (identity or 'missing')
                    result = subprocess.run(
                        ['bash', '-euc', command], cwd=root,
                        env={**os.environ, 'ANDROID_RELEASE_IDENTITY': identity, 'RETRY_APP': 'auto',
                             'PATHS_RELEASED': '[]', 'RELEASES': '{}', 'GITHUB_OUTPUT': str(output)},
                        capture_output=True, text=True,
                    )
                    if identity:
                        self.assertEqual(result.returncode, 0, result.stderr)
                        self.assertEqual(output.read_text(), 'matrix={"include":[]}\n')
                    else:
                        self.assertNotEqual(result.returncode, 0)
                        self.assertIn('ANDROID_RELEASE_IDENTITY', result.stderr)
                        self.assertFalse(output.exists())
