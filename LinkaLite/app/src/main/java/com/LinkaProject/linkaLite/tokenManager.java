package com.LinkaProject.linkaLite;

import android.content.Context;
import org.json.JSONObject;

public class tokenManager {
    public static String newSession(Context context) {
        try {
            config cfg = new config();
            String rawJson = cfg.loadCfgAsJson(context, "config.cfg");
            if (rawJson != null && !rawJson.isEmpty()) {
                JSONObject jsonCfg = new JSONObject(rawJson);
                if (jsonCfg.has("[FAST_LOGIN]")) {
                    JSONObject fastLogin = jsonCfg.getJSONObject("[FAST_LOGIN]");
                    String username = fastLogin.getString("username");
                    String password = fastLogin.getString("password");
                    String url = jsonCfg.optString("url", ""); 
                    JSONObject jsonToken = new JSONObject();
                    jsonToken.put("username", username);
                    jsonToken.put("password", password);
                    JSONObject response = request.requestHTTP(url + "/new-session", "post", jsonToken);
                    if (response != null) {
                        String token = response.getString("token");
                        return token.toString();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}