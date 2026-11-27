package com.LinkaProject.linkaLite;

import android.content.Context;
import org.json.JSONObject;
import org.json.JSONException;

public class Follow {
    private String username = "";
    private String server = "";

    public void FollowUsername(Context context, String usernameFollow) {
        try {
            config cfg = new config();
            String cfgString = cfg.loadCfgAsJson(context, "config.cfg");
            JSONObject jsonCfg = new JSONObject(cfgString);
            JSONObject serverObj = jsonCfg.getJSONObject("SERVER");
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            username = fastLogin.optString("username", "");
            server = serverObj.optString("url", "");
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        try {
            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("username", username);
            jsonPayload.put("follow_username", usernameFollow);
            request.requestHTTPAsync(server + "/follow", "post", jsonPayload, 0, context, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}