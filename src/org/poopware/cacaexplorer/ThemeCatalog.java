// SPDX-License-Identifier: AGPL-3.0-or-later
package org.poopware.cacaexplorer;

// Generated from Poopterm theme.rs, SHA-256: 2267021a24bd052e435c95d00833ee5ef78263b8a95f5ba6457958b9e3cd5f1d
// Theme definitions: Poopware Laboratories / Poopterm contributors.
final class ThemeCatalog {
    static final String[] NAMES = {"macOS Dark","macOS Light","Nord","Rosé Pine","Rosé Pine Moon","Rosé Pine Dawn","Catppuccin Latte","Catppuccin Frappé","Catppuccin Macchiato","Catppuccin Mocha","Bark","AMOLED Black","Poop","Graphite","Dolphin","Custom"};
    // Foreground, background, border, then the complete 8-color terminal palette.
    static final int[][] COLORS = {
        {0xffe6e6e6,0xff1e1e1e,0xff3a3a3c,0xff000000,0xffc23621,0xff25bc26,0xffadad27,0xff492ee1,0xffd338d3,0xff33bbc8,0xffbbbbbb}, // macOS Dark
        {0xff1d1d1f,0xfff5f5f7,0xffd2d2d7,0xff6e6e73,0xffc41e3a,0xff248a3d,0xff9a6700,0xff0066cc,0xff8944ab,0xff007a82,0xff1d1d1f}, // macOS Light
        {0xffd8dee9,0xff2e3440,0xff434c5e,0xff3b4252,0xffbf616a,0xffa3be8c,0xffebcb8b,0xff81a1c1,0xffb48ead,0xff88c0d0,0xffe5e9f0}, // Nord
        {0xffe0def4,0xff191724,0xff26233a,0xff26233a,0xffeb6f92,0xff31748f,0xfff6c177,0xff9ccfd8,0xffc4a7e7,0xffebbcba,0xffe0def4}, // Rosé Pine
        {0xffe0def4,0xff232136,0xff393552,0xff393552,0xffeb6f92,0xff3e8fb0,0xfff6c177,0xff9ccfd8,0xffc4a7e7,0xffea9a97,0xffe0def4}, // Rosé Pine Moon
        {0xff464261,0xfffaf4ed,0xfff2e9e1,0xfff2e9e1,0xffb4637a,0xff286983,0xffea9d34,0xff56949f,0xff907aa9,0xffd7827e,0xff464261}, // Rosé Pine Dawn
        {0xff4c4f69,0xffeff1f5,0xffccd0da,0xff5c5f77,0xffd20f39,0xff40a02b,0xffdf8e1d,0xff1e66f5,0xffea76cb,0xff179299,0xffacb0be}, // Catppuccin Latte
        {0xffc6d0f5,0xff303446,0xff414559,0xff51576d,0xffe78284,0xffa6d189,0xffe5c890,0xff8caaee,0xffca9ee6,0xff81c8be,0xffb5bfe2}, // Catppuccin Frappé
        {0xffcad3f5,0xff24273a,0xff363a4f,0xff494d64,0xffed8796,0xffa6da95,0xffeed49f,0xff8aadf4,0xfff5bde6,0xff8bd5ca,0xffb8c0e0}, // Catppuccin Macchiato
        {0xffcdd6f4,0xff1e1e2e,0xff45475a,0xff45475a,0xfff38ba8,0xffa6e3a1,0xfff9e2af,0xff89b4fa,0xfff5c2e7,0xff94e2d5,0xffbac2de}, // Catppuccin Mocha
        {0xffd0d0d0,0xff101010,0xff2b2b2b,0xff101010,0xffab4642,0xffa1b56c,0xfff7ca88,0xff7cafc2,0xffba8baf,0xff86c1b9,0xffd0d0d0}, // Bark
        {0xffe9e4d4,0xff000000,0xff1b1b1b,0xff121212,0xffff5c57,0xff8ce88a,0xfff2c14e,0xffd4af37,0xffc792ea,0xff6edbd2,0xffe9e4d4}, // AMOLED Black
        {0xffe8d5b0,0xff24170b,0xff7a4e1f,0xff1e1409,0xffd9534f,0xff8bc34a,0xffe0b040,0xff5c9bd6,0xffb06ac8,0xff4fb6b0,0xffe8d5b0}, // Poop
        {0xffeeeeef,0xff232325,0xff2b2b2e,0xff000000,0xffd9534f,0xff8bc34a,0xffe0b040,0xff29a7ee,0xffb06ac8,0xff4fb6b0,0xffeeeeef},
        {0xff232629,0xfffcfcfc,0xffeff0f1,0xff000000,0xffd9534f,0xff248a3d,0xff9a6700,0xff1d78b5,0xffb06ac8,0xff4fb6b0,0xff232629},
    };
    static int[] find(String name) { for(int i=0;i<COLORS.length;i++)if(NAMES[i].equals(name))return COLORS[i].clone();return COLORS[13].clone(); }
    static boolean light(int color) { return (((color>>16)&255)*299+((color>>8)&255)*587+(color&255)*114)>150000; }
    static double luminance(int c) { double[] v={((c>>16)&255)/255.0,((c>>8)&255)/255.0,(c&255)/255.0};for(int i=0;i<3;i++)v[i]=v[i]<=0.04045?v[i]/12.92:Math.pow((v[i]+0.055)/1.055,2.4);return v[0]*0.2126+v[1]*0.7152+v[2]*0.0722; }
    static double contrast(int a,int b) { double x=luminance(a),y=luminance(b);return (Math.max(x,y)+0.05)/(Math.min(x,y)+0.05); }
}
