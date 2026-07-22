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
                if (jsonCfg.has("FAST-LOGIN")) {
                    JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
                    String username = fastLogin.getString("username");
                    String password = fastLogin.getString("password");
                    String url = jsonCfg.optString("url", ""); 
                    JSONObject jsonToken = new JSONObject();
                    jsonToken.put("username", username);
                    jsonToken.put("password", password);
                    
                    JSONObject response = new JSONObject(request.requestHTTP(url + "/new-session", "post", jsonToken));
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

    public static String valideToken(String token, String url) {
        try {
            JSONObject valideJson = new JSONObject();
            valideJson.put("token", token);
            int status_code = 0;
            
            String response_token = request.requestHTTP(url + "/valideToken", "post", valideJson, status_code);
            
            if (status_code == 200 || status_code == 201) {
                return response_token.toString();
            } else {
                return response_token.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}