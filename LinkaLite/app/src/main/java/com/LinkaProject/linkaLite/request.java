package com.LinkaProject.linkaLite;

import android.content.Context;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class request {

    public static String requestHTTP(String urlParam, String method, JSONObject json_body) {
        return requestHTTP(urlParam, method, json_body, 0, null);
    }

    public static String requestHTTP(String urlParam, String method, JSONObject json_body, Context context) {
        return requestHTTP(urlParam, method, json_body, 0, context);
    }

    public static String requestHTTP(String urlParam, String method, JSONObject json_body, int status_code) {
        return requestHTTP(urlParam, method, json_body, status_code, null);
    }

    public static String requestHTTP(String urlParam, String method, JSONObject json_body, int status_code, Context context) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlParam);
            connection = (HttpURLConnection) url.openConnection();
            
            method = method.toUpperCase();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");
            
            // --- NOVO: INJETAR TOKEN DE AUTENTICAÇÃO NO CABEÇALHO ---
            if (context != null) {
                try {
                    config cfg = new config();
                    String rawCfg = cfg.loadCfgAsJson(context, "config.cfg");
                    if (rawCfg != null && !rawCfg.isEmpty()) {
                        JSONObject jsonCfg = new JSONObject(rawCfg);
                        if (jsonCfg.has("FAST_LOGIN")) {
                            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
                            String token = fastLogin.optString("token_session", "");
                            if (!token.isEmpty()) {
                                connection.setRequestProperty("Authorization", "Bearer " + token);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            // ----------------------------------------------------

            if (method.equals("POST") || method.equals("PUT")) {
                connection.setDoOutput(true);
                OutputStream os = connection.getOutputStream();
                if (json_body != null) {
                    os.write(json_body.toString().getBytes("UTF-8"));
                }
                os.flush();
                os.close();
            }

            int responseCode = connection.getResponseCode();
            status_code = responseCode;

            // Se der 403 (Token Expirado/Inválido), tenta renovar a sessão
            if (responseCode == 403 && context != null) {
                String newToken = tokenManager.newSession(context);
                if (newToken != null && !newToken.isEmpty()) {
                    config cfg = new config();
                    cfg.updateCfg(context, "config.cfg", "FAST_LOGIN", "token_session", newToken);
                }
            }

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                return response.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return ""; 
    }
}