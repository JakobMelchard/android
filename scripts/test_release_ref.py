import os
from pathlib import Path
import subprocess
import tempfile
import textwrap
import unittest


class ReleaseRefTest(unittest.TestCase):
    def test_only_tags_in_trusted_history_can_be_built(self):
        workflow = (Path(__file__).parent.parent / '.github/workflows/release.yml').read_text()
        marker = '      - name: verify release history\n'
        self.assertIn(marker, workflow)
        command = textwrap.dedent(workflow.split(marker, 1)[1].split('        run: |\n', 1)[1].split('      - uses:', 1)[0])
        with tempfile.TemporaryDirectory() as temp:
            def git(*args):
                return subprocess.check_output(['git', *args], cwd=temp, text=True, stderr=subprocess.DEVNULL).strip()

            git('init', '-b', 'main')
            git('config', 'user.name', 'test')
            git('config', 'user.email', 'test@example.com')
            git('commit', '--allow-empty', '-m', 'trusted')
            trusted = git('rev-parse', 'HEAD')
            git('tag', '-a', 'pull-v1.2.3', '-m', 'trusted release')
            git('switch', '-c', 'unreviewed')
            git('commit', '--allow-empty', '-m', 'unreviewed build code')
            git('tag', 'pull-v9.9.9')
            for tag, allowed in [('pull-v1.2.3', True), ('pull-v9.9.9', False), ('missing', False)]:
                with self.subTest(tag=tag):
                    git('checkout', '--detach', trusted)
                    result = subprocess.run(
                        ['bash', '-euc', command], cwd=temp,
                        env={**os.environ, 'GITHUB_SHA': trusted, 'RELEASE_TAG': tag}, capture_output=True, text=True,
                    )
                    self.assertEqual(result.returncode == 0, allowed, result.stderr)
                    self.assertEqual(git('rev-parse', 'HEAD'), trusted)
