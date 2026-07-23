package com.LinkaProject.linkaLite;

import android.content.Context;
import org.json.JSONObject;
import org.json.JSONException;
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
            int status_code = 0;
            request.requestHTTP(url + "/valide-session", "post", valideJson, status_code, context);
            if (status_code != 200){
                try{
                    config cfg = new config();
                    JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(context, "config.cfg"));
                    JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
                    String username = fastLogin.getString("username").toString();
                    String password = fastLogin.getString("password").toString();
                    token = newSession(context);
                    cfg.updateCfg(context, "config.cfg", "FAST_LOGIN", "token_session", token);
                    return "new token has created";
                }catch (JSONException e){
                    return "error in json parsing";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}