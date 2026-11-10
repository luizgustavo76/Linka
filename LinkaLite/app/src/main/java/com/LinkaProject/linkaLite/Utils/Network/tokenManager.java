package com.LinkaProject.linkaLite;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;

public class tokenManager {
    private static final String TAG = "TOKEN_MANAGER";
    private static int retryCount = 0;
    private static final int MAX_RETRIES = 2;

    /**
     * Gera uma nova sessão passando credenciais explícitas em memória (evita I/O síncrono no config.cfg)
     */
    public static String newSession(Context context, String username, String password, String customUrl) {
        if (retryCount >= MAX_RETRIES) {
            Log.e(TAG, "[TOKEN BLOQUEADO] Limite de tentativas excedido (" + MAX_RETRIES + "). Abortando loop.");
            retryCount = 0;
            return "";
        }

        retryCount++;

        try {
            String baseUrl = customUrl;
            
            // Fallback para o config.cfg se a URL não for fornecida
            if (baseUrl == null || baseUrl.trim().length() == 0) {
                config cfg = new config();
                String rawJson = cfg.loadCfgAsJson(context, "config.cfg");
                if (rawJson != null && rawJson.length() > 0) {
                    JSONObject jsonCfg = new JSONObject(rawJson);
                    if (jsonCfg.has("SERVER")) {
                        baseUrl = jsonCfg.getJSONObject("SERVER").optString("url", "http://patient-hall-2915.luizsgustavo76.workers.dev");
                    }
                }
            }

            if (baseUrl == null || baseUrl.trim().length() == 0) {
                baseUrl = "http://patient-hall-2915.luizsgustavo76.workers.dev";
            }

            if (username == null || password == null || username.length() == 0 || password.length() == 0) {
                Log.w(TAG, "[TOKEN ERRO] Usuário ou senha ausentes para solicitação de token.");
                retryCount = 0;
                return "";
            }

            while (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            JSONObject jsonToken = new JSONObject();
            jsonToken.put("username", username);
            jsonToken.put("password", password);
            
            String targetUrl = baseUrl + "/new-session";
            Log.d(TAG, "[TOKEN HTTP] Solicitando token (Tentativa " + retryCount + "/" + MAX_RETRIES + "): " + targetUrl);

            String result = request.requestHTTP(targetUrl, "POST", jsonToken, context);
            Log.d(TAG, "[TOKEN RESPOSTA] " + result);

            if (result != null && result.length() > 0) {
                JSONObject response = new JSONObject(result);
                
                String extractedToken = "";
                if (response.has("token")) {
                    extractedToken = response.optString("token", "");
                } else if (response.has("token_session")) {
                    extractedToken = response.optString("token_session", "");
                } else if (response.has("session")) {
                    extractedToken = response.optString("session", "");
                }

                if (extractedToken.length() > 0) {
                    retryCount = 0; // Sucesso! Reseta contador
                    return extractedToken;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "[TOKEN EXCEÇÃO] Erro ao renovar sessão:", e);
        }
        
        return "";
    }

    /**
     * Mantém compatibilidade com chamadas legadas lendo do config.cfg
     */
    public static String newSession(Context context) {
        String username = "";
        String password = "";
        String baseUrl = "";
        
        try {
            config cfg = new config();
            String rawJson = cfg.loadCfgAsJson(context, "config.cfg");
            if (rawJson != null && rawJson.length() > 0) {
                JSONObject jsonCfg = new JSONObject(rawJson);
                if (jsonCfg.has("FAST_LOGIN")) {
                    JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
                    username = fastLogin.optString("username", "");
                    password = fastLogin.optString("password", "");
                }
                if (jsonCfg.has("SERVER")) {
                    baseUrl = jsonCfg.getJSONObject("SERVER").optString("url", "");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "[TOKEN CONFIG READ ERRO]", e);
        }

        return newSession(context, username, password, baseUrl);
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

            Log.w(TAG, "[TOKEN] Sessão expirada ou inválida. Tentando renovação pontual...");
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