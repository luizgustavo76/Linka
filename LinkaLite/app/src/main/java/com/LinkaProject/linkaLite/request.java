package com.LinkaProject.linkaLite;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
public class request {
    public interface RequestCallback {
        void onSuccess(String result);
        void onError(Exception e);
    }
    public interface RequestBytesCallback {
        void onSuccess(byte[] result);
        void onError(Exception e);
    }
    public static void requestHTTPAsync(final String urlParam, final String method, final JSONObject json_body, final int status_code, final Context context, final RequestCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String result = requestHTTP(urlParam, method, json_body, status_code, context);
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(result);
                            }
                        });
                    }
                } catch (final Exception e) {
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(e);
                            }
                        });
                    }
                }
            }
        }).start();
    }
    public static void requestBytesAsync(final String urlParam, final String method, final Context context, final RequestBytesCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final byte[] result = requestBytes(urlParam, method, context);
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(result);
                            }
                        });
                    }
                } catch (final Exception e) {
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(e);
                            }
                        });
                    }
                }
            }
        }).start();
    }
    public static String requestHTTP(String urlParam, String method, JSONObject json_body) {
        return requestHTTP(urlParam, method, json_body, 0, null);
    }
    public static String requestHTTP(String urlParam, String method, JSONObject json_body, Context context) {
        return requestHTTP(urlParam, method, json_body, 0, context);
    }
    public static String requestHTTP(String urlParam, String method, JSONObject json_body, int status_code) {
        return requestHTTP(urlParam, method, json_body, status_code, null);
    }
    public static byte[] requestBytes(String urlParam, String method, Context context) {
        HttpURLConnection connection = null;
        try {
            System.setProperty("http.keepAlive", "false");
            URL url = new URL(urlParam);
            connection = (HttpURLConnection) url.openConnection();
            method = method.toUpperCase();
            connection.setRequestMethod(method);
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
            int responseCode = connection.getResponseCode();
            if (responseCode == 403 && context != null) {
                String newToken = tokenManager.newSession(context);
                if (newToken != null && !newToken.isEmpty()) {
                    config cfg = new config();
                    cfg.updateCfg(context, "config.cfg", "FAST_LOGIN", "token_session", newToken);
                }
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                java.io.InputStream in = connection.getInputStream();
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                in.close();
                return out.toByteArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }
    public static String requestHTTP(String urlParam, String method, JSONObject json_body, int status_code, Context context) {
    HttpURLConnection connection = null;
    try {
        System.setProperty("http.keepAlive", "false");
        URL url = new URL(urlParam);
        connection = (HttpURLConnection) url.openConnection();
        method = method.toUpperCase();
        
        // Aumentado o timeout para 25 segundos para suportar buscas externas em lote
        connection.setConnectTimeout(25000); 
        connection.setReadTimeout(25000);
        
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        
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

        if (responseCode == 403 && context != null) {
            String newToken = tokenManager.newSession(context);
            if (newToken != null && !newToken.isEmpty()) {
                config cfg = new config();
                cfg.updateCfg(context, "config.cfg", "FAST_LOGIN", "token_session", newToken);
            }
        }

        // Lê tanto a resposta de sucesso (2xx) quanto a stream de erro (4xx, 5xx)
        java.io.InputStream is;
        if (responseCode >= 200 && responseCode < 400) {
            is = connection.getInputStream();
        } else {
            is = connection.getErrorStream();
        }

        if (is != null) {
            BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            return response.toString();
        }

    } catch (Exception e) {
        android.util.Log.e("LINKA_DEBUG", "Erro HTTP em " + urlParam, e);
    } finally {
        if (connection != null) {
            connection.disconnect();
        }
    }
    return ""; 
}
}