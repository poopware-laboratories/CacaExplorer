#!/usr/bin/env python3
"""Generate Android theme data from Poopterm's src/theme.rs, without guessing colors.

Usage: python3 tools/import-themes.py /path/to/poopterm/src/theme.rs
Source: https://github.com/poopware-laboratories/poopterm/blob/main/src/theme.rs
"""
import hashlib
import pathlib
import re
import sys

source = pathlib.Path(sys.argv[1]).read_text()
constants = dict(re.findall(r'const (DEFAULT_FG|DEFAULT_BG|BORDER_COL): u32 = (0x[0-9A-Fa-f]+)', source))
names = {
    'MacOsDark': 'macOS Dark', 'MacOsLight': 'macOS Light', 'Nord': 'Nord',
    'RosePine': 'Rosé Pine', 'RosePineMoon': 'Rosé Pine Moon', 'RosePineDawn': 'Rosé Pine Dawn',
    'CatppuccinLatte': 'Catppuccin Latte', 'CatppuccinFrappe': 'Catppuccin Frappé',
    'CatppuccinMacchiato': 'Catppuccin Macchiato', 'CatppuccinMocha': 'Catppuccin Mocha',
    'Bark': 'Bark', 'AmoledBlack': 'AMOLED Black', 'Poop': 'Poop',
}
themes = []
for key, name in names.items():
    block = re.search(r'ThemeKind::' + key + r' => Self \{(.*?)\n\s*\},', source, re.S).group(1)
    colors = []
    for field in ('fg', 'bg', 'border'):
        value = re.search(field + r': (\w+)', block).group(1)
        colors.append(int(constants.get(value, value), 16))
    palette = re.search(r'palette: \[(.*?)\]', block, re.S)
    palette = palette.group(1) if palette else re.search(r'const PALETTE: \[u32; 8\] = \[(.*?)\]', source, re.S).group(1)
    palette = [int(x, 16) for x in re.findall(r'0x[0-9A-Fa-f]+', palette)]
    assert len(palette) == 8
    themes.append((name, colors, palette))
lines = ['// SPDX-License-Identifier: AGPL-3.0-or-later', 'package org.poopware.cacaexplorer;', '',
    '// Generated from Poopterm theme.rs, SHA-256: ' + hashlib.sha256(source.encode()).hexdigest(),
    '// Theme definitions: Poopware Laboratories / Poopterm contributors.',
    'final class ThemeCatalog {',
    '    static final String[] NAMES = {' + ','.join('"'+x[0]+'"' for x in themes) + ',"Graphite","Dolphin","Custom"};',
    '    // Foreground, background, border, then the complete 8-color terminal palette.',
    '    static final int[][] COLORS = {']
for name, colors, palette in themes:
    lines.append('        {' + ','.join('0xff%06x' % x for x in colors + palette) + '}, // ' + name)
for colors in ([0xeeeeef,0x232325,0x2b2b2e,0,0xd9534f,0x8bc34a,0xe0b040,0x29a7ee,0xb06ac8,0x4fb6b0,0xeeeeef],
               [0x232629,0xfcfcfc,0xeff0f1,0,0xd9534f,0x248a3d,0x9a6700,0x1d78b5,0xb06ac8,0x4fb6b0,0x232629]):
    lines.append('        {' + ','.join('0xff%06x' % x for x in colors) + '},')
lines += ['    };',
    '    static int[] find(String name) { for(int i=0;i<COLORS.length;i++)if(NAMES[i].equals(name))return COLORS[i].clone();return COLORS[13].clone(); }',
    '    static boolean light(int color) { return (((color>>16)&255)*299+((color>>8)&255)*587+(color&255)*114)>150000; }',
    '    static double luminance(int c) { double[] v={((c>>16)&255)/255.0,((c>>8)&255)/255.0,(c&255)/255.0};for(int i=0;i<3;i++)v[i]=v[i]<=0.04045?v[i]/12.92:Math.pow((v[i]+0.055)/1.055,2.4);return v[0]*0.2126+v[1]*0.7152+v[2]*0.0722; }',
    '    static double contrast(int a,int b) { double x=luminance(a),y=luminance(b);return (Math.max(x,y)+0.05)/(Math.min(x,y)+0.05); }',
    '}']
target = pathlib.Path(__file__).resolve().parents[1] / 'src/org/poopware/cacaexplorer/ThemeCatalog.java'
target.write_text('\n'.join(lines)+'\n')
print('Imported',len(themes),'Poopterm themes into',target)
