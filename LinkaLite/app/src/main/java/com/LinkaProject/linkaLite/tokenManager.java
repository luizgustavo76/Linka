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
                
                if (jsonCfg.has("FAST_LOGIN") && jsonCfg.has("SERVER")) {
                    JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
                    JSONObject server = jsonCfg.getJSONObject("SERVER");
                    
                    String username = fastLogin.optString("username", "");
                    String password = fastLogin.optString("password", "");
                    String url = server.optString("url", "http://linkaProject.pythonanywhere.com"); 

                    if (username.isEmpty() || password.isEmpty()) {
                        return "";
                    }

                    JSONObject jsonToken = new JSONObject();
                    jsonToken.put("username", username);
                    jsonToken.put("password", password);
                    
                    String responseStr = request.requestHTTP(url + "/new-session", "post", jsonToken, context);
                    if (responseStr != null && !responseStr.isEmpty()) {
                        JSONObject response = new JSONObject(responseStr);
                        if (response.has("token")) {
                            return response.getString("token");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String valideToken(String token, String url, Context context) {
        try {
            JSONObject valideJson = new JSONObject();
            valideJson.put("token", token);
            return request.requestHTTP(url + "/valideToken", "post", valideJson, context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}