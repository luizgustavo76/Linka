package com.LinkaProject.linkaLite;

import android.content.Context;
import org.json.JSONException;
import org.json.JSONObject;

public class Bookmark {

    private String url = "";
    private String username = "";
    private Context context; 
    public Bookmark(Context context) {
        this.context = context; 
        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(context, "config.cfg"));
            JSONObject fastLogin = jsonCfg.optJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.optJSONObject("SERVER");

            if (server != null) {
                url = server.optString("url", "");
            }
            if (fastLogin != null) {
                username = fastLogin.optString("username", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void toggle(int postId, boolean isPublic, boolean isExternal, String originalId, String platform) {
        try {
            JSONObject jsonToggle = new JSONObject();
            jsonToggle.put("post_id", postId);
            jsonToggle.put("is_public", isPublic);
            jsonToggle.put("is_external", isExternal);
            jsonToggle.put("username", username);
            jsonToggle.put("original_id", originalId);
            jsonToggle.put("platform", platform);

            request.requestHTTP(url + "/toggle-bookmark", "post", jsonToggle, context);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}