package com.LinkaProject.linkaLite;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONObject;
import org.json.JSONException;
public class AppStatus {
    private String username = "";
    private String url = "";
    public void SendStatus(String action, Context context){
        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(context, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            username = fastLogin.getString("username").toString();
            url = server.optString("url", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try{
            JSONObject jsonStatus = new JSONObject();
            jsonStatus.put("username", username);
            jsonStatus.put("action", action);
            request.requestHTTP(url + "/send-status", "post", jsonStatus, context);
        }catch(JSONException e){
            e.printStackTrace();
        }
    }
}