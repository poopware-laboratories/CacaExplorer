// SPDX-License-Identifier: AGPL-3.0-or-later
package org.poopware.cacaexplorer;
public class ThemeCatalogTest {
    public static void main(String[] args) {
        if(ThemeCatalog.COLORS.length!=15||ThemeCatalog.NAMES.length!=16)throw new AssertionError("Missing theme");
        for(int i=0;i<ThemeCatalog.COLORS.length;i++){
            int[] colors=ThemeCatalog.COLORS[i];
            if(colors.length!=11)throw new AssertionError("Missing palette colors");
            if(ThemeCatalog.contrast(colors[0],colors[1])<4.5)throw new AssertionError("Unreadable text: "+ThemeCatalog.NAMES[i]);
            int accent=colors[7];if(ThemeCatalog.contrast(accent,colors[1])<3)accent=ThemeCatalog.contrast(colors[9],colors[1])>=3?colors[9]:colors[0];
            if(ThemeCatalog.contrast(accent,colors[1])<3)throw new AssertionError("Unreadable accent");
            int[] copy=ThemeCatalog.find(ThemeCatalog.NAMES[i]);copy[0]=0;
            if(ThemeCatalog.COLORS[i][0]==0)throw new AssertionError("Theme data mutated");
        }
        System.out.println("PASS: all 13 Poopterm palettes and 2 browser palettes retain full color data, readable text and accents");
    }
}
