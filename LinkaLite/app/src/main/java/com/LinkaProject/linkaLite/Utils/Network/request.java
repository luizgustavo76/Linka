package com.LinkaProject.linkaLite;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class request {

    public interface RequestCallback {
        void onSuccess(String result);
        void onError(Exception e);
    }

    public interface RequestBytesCallback {
        void onSuccess(byte[] result);
        void onError(Exception e);
    }

    /**
     * Ajusta a URL de acordo com o suporte nativo de TLS da versão do Android:
     * - API < 16 (Android 2.3 / 3.x): OpenSSL nativo não suporta TLS 1.2 -> usa HTTP.
     * - API >= 16 (Android 4.1+): suporta TLS 1.2 -> mantém HTTPS.
     */
    private static String normalizeUrlForSdk(String rawUrl) {
        if (rawUrl == null) return "";
        String url = rawUrl.trim();
        if (Build.VERSION.SDK_INT < 16 && url.startsWith("https://")) {
            return "http://" + url.substring(8);
        }
        return url;
    }

    /**
     * Resolve o erro "CertPathValidatorException: Trust anchor for certification path not found"
     * em aparelhos Android 4.1 até 7.1 (API 16..25), cujos Root CAs do sistema expiraram
     * e não reconhecem certificados modernos como ISRG Root X1 (Let's Encrypt) e GTS Root (Cloudflare).
     */
    private static void configureLegacySSL(HttpURLConnection connection) {
        if (Build.VERSION.SDK_INT <= 25 && (connection instanceof HttpsURLConnection)) {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
                };

                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new SecureRandom());

                HttpsURLConnection httpsConn = (HttpsURLConnection) connection;
                httpsConn.setSSLSocketFactory(sc.getSocketFactory());
                httpsConn.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
            } catch (Exception e) {
                Log.e("LINKA_DEBUG", "Falha ao configurar SSL legado", e);
            }
        }
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
        return requestBytesInternal(urlParam, method, context, 0);
    }

    private static byte[] requestBytesInternal(String urlParam, String method, Context context, int redirectCount) {
        if (redirectCount > 5) {
            Log.e("LINKA_DEBUG", "Loop de redirecionamento abortado para: " + urlParam);
            return null;
        }

        HttpURLConnection connection = null;
        try {
            System.setProperty("http.keepAlive", "false");

            String safeUrl = normalizeUrlForSdk(urlParam);
            URL url = new URL(safeUrl);
            connection = (HttpURLConnection) url.openConnection();
            configureLegacySSL(connection);

            method = method.toUpperCase();

            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod(method);
            connection.setInstanceFollowRedirects(false);

            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");

            if (url.getHost() != null) {
                connection.setRequestProperty("Host", url.getHost());
            }

            int responseCode = connection.getResponseCode();

            // Trata redirecionamentos HTTP 301, 302, 303, 307, 308
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == 307 || responseCode == 308 || responseCode == 303) {

                String newUrl = connection.getHeaderField("Location");
                if (newUrl != null && newUrl.length() > 0) {
                    if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                        if (!newUrl.startsWith("/")) {
                            newUrl = "/" + newUrl;
                        }
                        newUrl = url.getProtocol() + "://" + url.getHost() + newUrl;
                    }
                    return requestBytesInternal(newUrl, method, context, redirectCount + 1);
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
            } else {
                Log.e("LINKA_DEBUG", "HTTP " + responseCode + " ao baixar bytes de: " + safeUrl);
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

            String safeUrl = normalizeUrlForSdk(urlParam);
            URL url = new URL(safeUrl);
            connection = (HttpURLConnection) url.openConnection();
            configureLegacySSL(connection);

            method = method.toUpperCase();

            connection.setConnectTimeout(25000);
            connection.setReadTimeout(25000);

            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

            if (url.getHost() != null) {
                connection.setRequestProperty("Host", url.getHost());
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