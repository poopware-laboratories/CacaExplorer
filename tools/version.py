#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""Poopterm-style YEAR.MONTH.PATCH and monthly codenames for Honeycomb APKs."""
import argparse
import datetime
import json
from pathlib import Path
import re
import secrets
import xml.etree.ElementTree as E
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parents[1]
PREFIX = '{http://schemas.android.com/apk/res/android}'
PATTERN = r'(0|[1-9][0-9]?)\.(1[0-2]|[1-9])\.(0|[1-9][0-9]{0,3})'

def parts(version):
    if not re.fullmatch(PATTERN, version):
        raise ValueError('Use YEAR.MONTH.PATCH, e.g. 26.10.0 (month 1–12; patch 0–9999)')
    return tuple(map(int, version.split('.')))

def next_version(current, today):
    year, month, patch = parts(current)
    period = (today.year % 100, today.month)
    if period < (year, month):
        raise ValueError('Refusing to release a version older than the current month')
    version = f'{period[0]}.{period[1]}.{patch + 1 if period == (year, month) else 0}'
    parts(version)
    return version

def code(version, ci=False):
    year, month, patch = parts(version)
    # A stable release sorts after test APKs of the same calendar version.
    return year * 10000000 + month * 100000 + patch * 10 + (0 if ci else 9)

def prepare(data, version):
    old_period = parts(data['version'])[:2]
    period = parts(version)[:2]
    if period < old_period:
        raise ValueError('Refusing to move the version backwards')
    if parts(version) < parts(data['version']):
        raise ValueError('Refusing to move the patch backwards')
    result = dict(data, version=version)
    if period != old_period:
        previous = data['codename']
        while result['codename'] == previous:
            result['codename'] = secrets.choice(['Cosmic', 'Disco', 'Turbo', 'Velvet', 'Lunar', 'Electric']) + ' ' + secrets.choice(['Beetle', 'Moth', 'Gecko', 'Wombat', 'Otter', 'Fly'])
    return result

def apply(root, data, ci=None):
    version = data['version']
    parts(version)
    tree = E.parse(root / 'AndroidManifest.xml')
    tree.getroot().set(PREFIX + 'versionName', version + (f'-ci.{ci}' if ci is not None else ''))
    tree.getroot().set(PREFIX + 'versionCode', str(code(version, ci is not None)))
    E.register_namespace('android', PREFIX[1:-1])
    tree.write(root / 'AndroidManifest.xml', encoding='utf-8', xml_declaration=True)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    action = parser.add_mutually_exclusive_group()
    action.add_argument('--next', action='store_true', help='Prepare the next monthly patch before committing/tagging')
    action.add_argument('--tag', help='Release tag such as v26.10.0')
    parser.add_argument('--ci', type=int, help='Label a test build; release builds omit this')
    args = parser.parse_args()
    if args.ci is not None and args.ci < 1:
        parser.error('CI run number must be positive')
    data = json.loads((ROOT / 'version.json').read_text())
    if args.next:
        today = datetime.datetime.now(ZoneInfo('America/Fortaleza')).date()
        data = prepare(data, next_version(data['version'], today))
    elif args.tag:
        if not args.tag.startswith('v'):
            parser.error('Tags start with v, e.g. v26.10.0')
        data = prepare(data, args.tag[1:])
    (ROOT / 'version.json').write_text(json.dumps(data, indent=2) + '\n')
    apply(ROOT, data, args.ci)
    print(data['version'] + ' "' + data['codename'] + '"')

if __name__ == '__main__':
    main()
