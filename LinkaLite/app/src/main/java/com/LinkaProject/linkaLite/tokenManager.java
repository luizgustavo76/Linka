package com.LinkaProject.linkaLite;

import android.content.Context;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class tokenManager {

    public static String newSession(Context context) {
        HttpURLConnection conn = null;
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
                    String baseUrl = server.optString("url", "http://linkaProject.pythonanywhere.com"); 

                    if (username.isEmpty() || password.isEmpty()) {
                        return "";
                    }

                    JSONObject jsonToken = new JSONObject();
                    jsonToken.put("username", username);
                    jsonToken.put("password", password);
                    
                    URL url = new URL(baseUrl + "/new-session");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    OutputStream os = conn.getOutputStream();
                    os.write(jsonToken.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream is = conn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();
                        is.close();

                        JSONObject response = new JSONObject(sb.toString());
                        if (response.has("token")) {
                            return response.getString("token");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return "";
    }

    public static String valideToken(String token, String url, Context context) {
        HttpURLConnection conn = null;
        try {
            JSONObject valideJson = new JSONObject();
            valideJson.put("token", token);
            
            URL targetUrl = new URL(url + "/valide-session");
            conn = (HttpURLConnection) targetUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            conn.setDoInput(true);

            OutputStream os = conn.getOutputStream();
            os.write(valideJson.toString().getBytes("UTF-8"));
            os.flush();
            os.close();

            int statusCode = conn.getResponseCode();
            
            if (statusCode != 200) {
                try {
                    config cfg = new config();
                    JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(context, "config.cfg"));
                    JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
                    String username = fastLogin.getString("username");
                    String password = fastLogin.getString("password");
                    token = newSession(context);
                    cfg.updateCfg(context, "config.cfg", "FAST_LOGIN", "token_session", token);
                    return "new token has created";
                } catch (JSONException e) {
                    return "error in json parsing";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return "";
    }

}