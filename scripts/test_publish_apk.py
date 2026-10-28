import importlib.machinery
import importlib.util
import os
import tempfile
from pathlib import Path
import subprocess
import unittest
from unittest.mock import patch

path = Path(__file__).with_name('publish-apk')


class PublishTest(unittest.TestCase):
    def setUp(self):
        loader = importlib.machinery.SourceFileLoader('publisher', str(path))
        spec = importlib.util.spec_from_loader(loader.name, loader)
        self.module = importlib.util.module_from_spec(spec)
        loader.exec_module(self.module)

    def test_unknown_component_and_invalid_versions_write_nothing(self):
        for app, version in [('other', '0.1.4'), ('fz', '0.1.3'), ('pull', '0.1.6'), ('fz', '1.100.0')]:
            with self.subTest(app=app, version=version), patch.object(self.module, 'gh') as gh:
                with self.assertRaises(ValueError):
                    self.module.validate(app, version)
                gh.assert_not_called()

    def test_existing_matching_asset_is_immutable(self):
        with patch.object(self.module, 'gh', return_value={'assets': [{'name': 'fz-0.1.4.apk', 'digest': 'sha256:abc'}]}) as gh:
            self.module.upload('JakobMelchard/android', 'fz-v0.1.4', Path('fz-0.1.4.apk'), 'abc')
            self.assertEqual(gh.call_count, 1)

    def test_existing_different_asset_refuses_overwrite(self):
        with patch.object(self.module, 'gh', return_value={'assets': [{'name': 'fz-0.1.4.apk', 'digest': 'sha256:other'}]}) as gh:
            with self.assertRaises(ValueError):
                self.module.upload('JakobMelchard/android', 'fz-v0.1.4', Path('fz-0.1.4.apk'), 'abc')
            self.assertEqual(gh.call_count, 1)

    def test_missing_canonical_release_never_creates_a_tag_or_mirror(self):
        with patch.object(self.module, 'gh', side_effect=subprocess.CalledProcessError(1, 'gh')) as gh:
            with self.assertRaises(subprocess.CalledProcessError):
                self.module.upload('JakobMelchard/android', 'fz-v0.1.4', Path('fz-0.1.4.apk'), 'abc')
            self.assertEqual(gh.call_count, 1)

    def test_retry_cannot_make_an_older_version_the_feed_latest(self):
        with self.assertRaises(ValueError):
            self.module.validate("fz", "0.1.4", "fz-v0.1.5")
        self.assertEqual(self.module.validate("fz", "0.1.4", "fz-v0.1.4"), 104)

    def test_publish_uses_only_android_after_legacy_repositories_are_deleted(self):
        for app, version, code in [('fz', '0.1.4', 104), ('pull', '0.1.7', 107)]:
            with self.subTest(app=app), tempfile.TemporaryDirectory() as temp:
                apk = Path(temp) / f'{app}-{version}.apk'
                apk.write_bytes(b'verified apk')
                calls = []

                def gh(*args):
                    calls.append(args)
                    self.assertEqual(args[0], 'api')
                    self.assertTrue(args[1].startswith('repos/JakobMelchard/android/'))
                    if '/tags?' in args[1]:
                        self.assertIn('--paginate', args)
                        self.assertIn('--slurp', args)
                        return [[{'name': f'{app}-v{version}'}]]
                    return {'assets': []}

                cert = f'certificate SHA-256 digest: {self.module.CERT}'
                meta = f"package: name='{self.module.APPS[app][0]}' versionCode='{code}' versionName='{version}'"
                with patch.object(self.module, 'gh', side_effect=gh), \
                        patch.object(self.module, 'upload') as upload, \
                        patch.object(self.module.subprocess, 'check_output', side_effect=[cert, meta]), \
                        patch.dict(os.environ, {'ANDROID_HOME': temp}), \
                        patch('sys.argv', ['publish-apk', app, version, str(apk)]):
                    self.module.main()
                self.assertTrue(calls)
                self.assertEqual(upload.call_count, 1)
                self.assertEqual(upload.call_args.args[:2], ('JakobMelchard/android', f'{app}-v{version}'))

    def test_newer_component_tag_on_later_page_blocks_older_upload(self):
        with tempfile.TemporaryDirectory() as temp:
            apk = Path(temp) / 'fz-0.1.4.apk'
            apk.write_bytes(b'verified apk')
            cert = f'certificate SHA-256 digest: {self.module.CERT}'
            meta = "package: name='io.lilfeelz.fz' versionCode='104' versionName='0.1.4'"
            pages = [[{'name': 'pull-v9.0.0'}, {'name': 'fz-v0.1.4'}], [{'name': 'fz-v0.1.5'}]]
            with patch.object(self.module, 'gh', return_value=pages), \
                    patch.object(self.module, 'upload') as upload, \
                    patch.object(self.module.subprocess, 'check_output', side_effect=[cert, meta]), \
                    patch.dict(os.environ, {'ANDROID_HOME': temp}), \
                    patch('sys.argv', ['publish-apk', 'fz', '0.1.4', str(apk)]):
                with self.assertRaises(ValueError):
                    self.module.main()
                upload.assert_not_called()
