package com.LinkaProject.linkaLite;

import android.content.Context;
import android.content.SharedPreferences;

public class AppConfig {
    private static final String PREF_NAME = "linka_lite_prefs";
    
    // Chaves de acesso
    private static final String KEY_URL = "server_url";
    private static final String KEY_USERNAME = "fast_login_username";
    
    private final SharedPreferences prefs;

    public AppConfig(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Getters
    public String getUrl() {
        return prefs.getString(KEY_URL, "");
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    // Setters
    public void setUrl(String url) {
        prefs.edit().putString(KEY_URL, url).apply();
    }

    public void setUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    public void saveAll(String url, String username) {
        prefs.edit()
            .putString(KEY_URL, url)
            .putString(KEY_USERNAME, username)
            .apply();
    }
}