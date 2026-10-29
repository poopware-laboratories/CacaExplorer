# SPDX-License-Identifier: AGPL-3.0-or-later
import datetime
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as E

spec = importlib.util.spec_from_file_location('version', Path(__file__).resolve().parents[1] / 'tools/version.py')
v = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v)

class VersionTests(unittest.TestCase):
    def test_monthly_patch_and_year_rollover(self):
        self.assertEqual(v.next_version('26.10.8', datetime.date(2026,10,29)), '26.10.9')
        self.assertEqual(v.next_version('26.10.8', datetime.date(2026,11,1)), '26.11.0')
        self.assertEqual(v.next_version('26.12.99', datetime.date(2027,1,1)), '27.1.0')
        with self.assertRaises(ValueError): v.next_version('26.11.0', datetime.date(2026,10,29))
        with self.assertRaises(ValueError): v.next_version('26.10.9999', datetime.date(2026,10,29))

    def test_monthly_codenames(self):
        data = {'version':'26.10.1', 'codename':'Lunar Gecko'}
        self.assertEqual(v.prepare(data,'26.10.2')['codename'],data['codename'])
        changed = v.prepare(data,'26.11.0')
        self.assertNotEqual(changed['codename'],data['codename'])
        self.assertEqual(v.prepare(changed,'26.11.1')['codename'],changed['codename'])
        with self.assertRaises(ValueError): v.prepare(data,'26.9.99')
        with self.assertRaises(ValueError): v.prepare(data,'26.10.0')

    def test_android_upgrade_order_and_limits(self):
        versions=['26.10.0','26.10.1','26.10.9999','26.11.0','26.12.9999','27.1.0','99.12.9999']
        codes = [v.code(s) for s in versions]
        self.assertEqual(codes, sorted(set(codes)))
        self.assertGreater(codes[0],100001)  # Migration from the previous CI code.
        self.assertLess(codes[-1],2100000000)
        for s in versions:self.assertGreater(v.code(s),v.code(s,ci=True))
        for s in ['26.0.0','26.13.0','26.10.-1','26.10.10000','026.10.0','26.01.0','26.10.0;echo bad']:
            with self.assertRaises(ValueError):v.parts(s)

    def test_manifest_contains_matching_name_and_code(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            (root/'AndroidManifest.xml').write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="org.test" android:versionName="0.1.1" android:versionCode="2"/>')
            data={'version':'26.10.0','codename':'Lunar Gecko'}
            v.apply(root,data,ci=8)
            attrs=E.parse(root/'AndroidManifest.xml').getroot().attrib
            self.assertEqual(attrs[v.PREFIX+'versionName'],'26.10.0-ci.8')
            self.assertEqual(int(attrs[v.PREFIX+'versionCode']),v.code('26.10.0',True))
            v.apply(root,data)
            self.assertEqual(E.parse(root/'AndroidManifest.xml').getroot().get(v.PREFIX+'versionName'),'26.10.0')
