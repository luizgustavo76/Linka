package com.LinkaProject.linkaLite;

import android.content.Context;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class request {

    // 1. Sobrecarga com 3 parâmetros (usada no tokenManager e newPost)
    public static String requestHTTP(String urlParam, String method, JSONObject json_body) {
        return requestHTTP(urlParam, method, json_body, 0, null);
    }

    // 2. Sobrecarga com 4 parâmetros (url, method, json, context)
    public static String requestHTTP(String urlParam, String method, JSONObject json_body, Context context) {
        return requestHTTP(urlParam, method, json_body, 0, context);
    }

    // 3. Sobrecarga com 4 parâmetros (url, method, json, status_code)
    public static String requestHTTP(String urlParam, String method, JSONObject json_body, int status_code) {
        return requestHTTP(urlParam, method, json_body, status_code, null);
    }

    // 4. Método Principal com toda a sua lógica mantida
    public static String requestHTTP(String urlParam, String method, JSONObject json_body, int status_code, Context context) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlParam);
            connection = (HttpURLConnection) url.openConnection();
            
            method = method.toUpperCase();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");
            
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

            // Tratamento do erro 403 mantendo a sua intenção original:
            if (responseCode == 403 && context != null) {
                // Chama newSession da tokenManager (pois o método pertence a ela)
                String token = tokenManager.newSession(context);
                
                // Salva a nova sessão no arquivo de configuração usando context (em vez do 'this')
                config cfg = new config();
                cfg.updateCfg(context, "config.cfg", "FAST-LOGIN", "token_session", token);
            }

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
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