package com.LinkaProject.linkaLite;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;

public class tokenManager {
    private static final String TAG = "TOKEN_MANAGER";
    private static int retryCount = 0;
    private static final int MAX_RETRIES = 2;

    public static String newSession(Context context) {
        if (retryCount >= MAX_RETRIES) {
            Log.e(TAG, "[TOKEN BLOQUEADO] Limite de tentativas excedido (" + MAX_RETRIES + "). Abortando loop.");
            retryCount = 0; // Reseta para futuras tentativas manuais de login
            return "";
        }

        retryCount++;

        try {
            config cfg = new config();
            String rawJson = cfg.loadCfgAsJson(context, "config.cfg");
            
            if (rawJson != null && rawJson.length() > 0) {
                JSONObject jsonCfg = new JSONObject(rawJson);
                
                if (jsonCfg.has("FAST_LOGIN") && jsonCfg.has("SERVER")) {
                    JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
                    JSONObject server = jsonCfg.getJSONObject("SERVER");
                    
                    String username = fastLogin.optString("username", "");
                    String password = fastLogin.optString("password", "");
                    String baseUrl = server.optString("url", "https://linka-ztg3.onrender.com").trim(); 

                    if (username.length() == 0 || password.length() == 0) {
                        Log.w(TAG, "[TOKEN ERRO] Usuário ou senha ausentes no config.cfg");
                        return "";
                    }

                    while (baseUrl.endsWith("/")) {
                        baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                    }

                    JSONObject jsonToken = new JSONObject();
                    jsonToken.put("username", username);
                    jsonToken.put("password", password);
                    
                    String targetUrl = baseUrl + "/new-session";
                    Log.d(TAG, "[TOKEN HTTP] Solicitando novo token (Tentativa " + retryCount + "/" + MAX_RETRIES + "): " + targetUrl);

                    String result = request.requestHTTP(targetUrl, "POST", jsonToken, context);
                    Log.d(TAG, "[TOKEN RESPOSTA] " + result);

                    if (result != null && result.length() > 0) {
                        JSONObject response = new JSONObject(result);
                        
                        if (response.has("token")) {
                            retryCount = 0; // Sucesso! Zerando o contador de retries
                            return response.getString("token");
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "[TOKEN EXCEÇÃO] Erro ao renovar sessão:", e);
        }
        return "";
    }

    public static String valideToken(String token, String url, Context context) {
        try {
            if (url == null || url.length() == 0) return "";
            
            while (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }

            JSONObject valideJson = new JSONObject();
            valideJson.put("token", token);
            
            String targetUrl = url + "/valide-session";
            String result = request.requestHTTP(targetUrl, "POST", valideJson, context);

            if (result != null && result.length() > 0) {
                JSONObject response = new JSONObject(result);
                String status = response.optString("status", "");
                if (status.equalsIgnoreCase("valid") || status.equalsIgnoreCase("success") || response.optBoolean("valid", false)) {
                    retryCount = 0;
                    return "valid";
                }
            }

            // Se o token for inválido, tenta renovar a sessão UMA ÚNICA VEZ
            Log.w(TAG, "[TOKEN] Sessão expirada ou inválida. Gerando nova sessão...");
            String newToken = newSession(context);
            if (newToken.length() > 0) {
                config cfg = new config();
                cfg.updateCfg(context, "config.cfg", "FAST_LOGIN", "token_session", newToken);
                return "new token has created";
            }
        } catch (Exception e) {
            Log.e(TAG, "[TOKEN EXCEÇÃO] Erro ao validar token:", e);
        }
        return "";
    }
}