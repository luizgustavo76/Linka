package com.LinkaProject.linkaLite;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
public class Bookmark{
    private String url = "";
    private String username = "";
    try{
        config cfg = new config();
        JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(this, "config.cfg"));
        JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
        JSONObject server = jsonCfg.getJSONObject("SERVER");
        url = server.optString("url", "");
        username = fastLogin.optString("username", "");
    }catch(JSONException e){
        e.printStackTrace();
    }
    public void toggle(int postId, boolean isPublic, boolean isExternal, String originalId, String platform){

    }
}