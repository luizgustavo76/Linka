package com.LinkaProject.linkaLite;

import android.content.Context;
import android.content.SharedPreferences;

public class ThemeManager {

    private static final String PREF_NAME = "linka_theme_prefs";
    private static final String KEY_THEME = "selected_theme";

    public static void applyTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String theme = prefs.getString(KEY_THEME, "light");

        if ("dark".equals(theme)) {
            context.setTheme(R.style.AppTheme_Dark);
        } else if ("dark_blue".equals(theme)) {
            context.setTheme(R.style.AppTheme_DarkBlue);
        } else if ("light_blue".equals(theme)) {
            context.setTheme(R.style.AppTheme_LightBlue);
        } else if ("dark_red".equals(theme)) {
            context.setTheme(R.style.AppTheme_DarkRed);
        } else if ("light_red".equals(theme)) {
            context.setTheme(R.style.AppTheme_LightRed);
        } else {
            context.setTheme(R.style.AppTheme); // Default Light
        }
    }

    public static void setTheme(Context context, String themeName) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, themeName).commit();
    }
}