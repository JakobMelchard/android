import importlib.machinery
import importlib.util
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
            self.module.validate("fz", "0.1.4", "v0.1.5")
        self.assertEqual(self.module.validate("fz", "0.1.4", "v0.1.4"), 104)
