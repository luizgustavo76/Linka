package com.LinkaProject.linkaLite;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

            URL originalUrl = new URL(urlParam);
            String hostName = originalUrl.getHost();
            String finalUrlStr = urlParam;

            // Intercepta e aplica o IP direto para Cloudflare Workers se necessário
            if (hostName != null && hostName.contains("workers.dev")) {
                String cloudflareIp = "104.21.80.1";
                finalUrlStr = urlParam.replace(hostName, cloudflareIp);
            }

            URL url = new URL(finalUrlStr);
            connection = (HttpURLConnection) url.openConnection();
            method = method.toUpperCase();
            
            connection.setConnectTimeout(25000);
            connection.setReadTimeout(25000);
            connection.setRequestMethod(method);
            connection.setInstanceFollowRedirects(true);

            // Adiciona User-Agent de navegador moderno para evitar redirecionamento infinito/bloqueios do Nitter/Cloudflare
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");

            if (hostName != null) {
                connection.setRequestProperty("Host", hostName);
            }

            if (context != null) {
                try {
                    config cfg = new config();
                    String rawCfg = cfg.loadCfgAsJson(context, "config.cfg");
                    if (rawCfg != null && rawCfg.length() > 0) {
                        JSONObject jsonCfg = new JSONObject(rawCfg);
                        if (jsonCfg.has("FAST_LOGIN")) {
                            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
                            String token = fastLogin.optString("token_session", "");
                            if (token.length() > 0) {
                                connection.setRequestProperty("Authorization", "Bearer " + token);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            int responseCode = connection.getResponseCode();

            // Trata redirecionamento manual se o servidor retornar 301/302/303/307
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == 307 || responseCode == 303) {
                String newUrl = connection.getHeaderField("Location");
                if (newUrl != null && newUrl.length() > 0) {
                    if (newUrl.startsWith("/")) {
                        newUrl = originalUrl.getProtocol() + "://" + hostName + newUrl;
                    }
                    if (!newUrl.equals(urlParam)) {
                        return requestBytes(newUrl, method, context);
                    }
                }
            }

            if (responseCode == 403 && context != null) {
                String newToken = tokenManager.newSession(context);
                if (newToken != null && newToken.length() > 0) {
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
            Log.e("LINKA_DEBUG", "Erro HTTP em " + urlParam, e);
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

            URL originalUrl = new URL(urlParam);
            String hostName = originalUrl.getHost();
            String finalUrlStr = urlParam;

            if (hostName != null && hostName.contains("workers.dev")) {
                String cloudflareIp = "104.21.80.1";
                finalUrlStr = urlParam.replace(hostName, cloudflareIp);
            }

            URL url = new URL(finalUrlStr);
            connection = (HttpURLConnection) url.openConnection();
            method = method.toUpperCase();

            connection.setConnectTimeout(25000); 
            connection.setReadTimeout(25000);
            
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

            if (hostName != null) {
                connection.setRequestProperty("Host", hostName);
            }

            if (context != null) {
                try {
                    config cfg = new config();
                    String rawCfg = cfg.loadCfgAsJson(context, "config.cfg");
                    if (rawCfg != null && rawCfg.length() > 0) {
                        JSONObject jsonCfg = new JSONObject(rawCfg);
                        if (jsonCfg.has("FAST_LOGIN")) {
                            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
                            String token = fastLogin.optString("token_session", "");
                            if (token.length() > 0) {
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
                if (newToken != null && newToken.length() > 0) {
                    config cfg = new config();
                    cfg.updateCfg(context, "config.cfg", "FAST_LOGIN", "token_session", newToken);
                }
            }

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
            Log.e("LINKA_DEBUG", "Erro HTTP em " + urlParam, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return ""; 
    }
}