package com.LinkaProject.linkaLite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class ImageLoader {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String TAG = "LINKA_IMG_LOADER";

    /*
     * O Android antigo fala somente com este endpoint HTTP.
     * O lite-render faz a requisicao moderna no servidor e devolve a imagem.
     */
    private static final String LITE_RENDER_BASE =
            "http:/linkaProject.pythonanywhere.com/lite-render?url=";
    public static String buildLiteRenderUrl(String imageUrl) {
        if (imageUrl == null) {
            Log.e(TAG, "buildLiteRenderUrl: imageUrl == null");
            return "";
        }

        String clean = imageUrl.replace("&amp;", "&").trim();

        if (clean.length() == 0 || clean.equalsIgnoreCase("null")) {
            Log.e(TAG, "buildLiteRenderUrl: URL vazia");
            return "";
        }

        String target = clean;
        String lower = clean.toLowerCase();
        String marker = "/lite-render?url=";
        int markerIndex = lower.indexOf(marker);

        try {
            if (markerIndex >= 0) {
                target = clean.substring(markerIndex + marker.length());

                // Caso o alvo ja esteja URL-encoded.
                if (target.startsWith("http%3A") || target.startsWith("https%3A")
                        || target.indexOf("%3A") == 4) {
                    try {
                        target = java.net.URLDecoder.decode(target, "UTF-8");
                    } catch (Exception e) {
                        Log.d(TAG, "Nao foi necessario decodificar target: " + e.getMessage());
                    }
                }
            }

            String encodedTarget = URLEncoder.encode(target, "UTF-8");
            // URLEncoder usa + para espaco; %20 e mais seguro em URL HTTP.
            encodedTarget = encodedTarget.replace("+", "%20");

            String result = LITE_RENDER_BASE + encodedTarget;

            Log.d(TAG, "URL original: " + imageUrl);
            Log.d(TAG, "Target do lite-render: " + target);
            Log.d(TAG, "URL final HTTP: " + result);

            return result;

        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Erro UTF-8 ao montar lite-render", e);
            return "";
        } catch (Exception e) {
            Log.e(TAG, "Erro ao montar lite-render", e);
            return "";
        }
    }

    public void processImageData(String imageUrl, final ImageView targetView) {

        if (targetView == null || imageUrl == null || imageUrl.trim().isEmpty()
                || imageUrl.equalsIgnoreCase("null")) {

            Log.e(TAG, "processImageData: parametros invalidos.");
            return;
        }

        final String liteRenderUrl = buildLiteRenderUrl(imageUrl);

        if (liteRenderUrl.isEmpty()) {
            Log.e(TAG, "Nao foi possivel montar o lite-render para: " + imageUrl);
            return;
        }

        Log.d(TAG, "----------------------------------------");
        Log.d(TAG, "IMAGE ORIGINAL: " + imageUrl);
        Log.d(TAG, "IMAGE VIA LITE-RENDER: " + liteRenderUrl);
        Log.d(TAG, "Iniciando HttpURLConnection diretamente no ImageLoader.");

        HttpURLConnection connection = null;
        InputStream input = null;
        ByteArrayOutputStream output = null;

        try {
            URL url = new URL(liteRenderUrl);
            connection = (HttpURLConnection) url.openConnection();

            // IMPORTANTE: o cliente antigo fala HTTP com o lite-render.
            // Nao seguir redirect para HTTPS automaticamente: vamos registrar
            // o Location no log caso o servidor esteja forçando HTTPS.
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setUseCaches(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "LinkaLite/1.0 Android");
            connection.setRequestProperty("Accept", "image/jpeg,image/png,image/gif,image/*,*/*;q=0.8");

            int responseCode = connection.getResponseCode();
            String contentType = connection.getContentType();
            String location = connection.getHeaderField("Location");

            Log.d(TAG, "HTTP STATUS: " + responseCode);
            Log.d(TAG, "CONTENT-TYPE: " + contentType);
            Log.d(TAG, "CONTENT-LENGTH: " + connection.getHeaderField("Content-Length"));
            Log.d(TAG, "LOCATION: " + location);
            Log.d(TAG, "URL EFETIVA: " + connection.getURL());

            if (responseCode >= 300 && responseCode < 400) {
                Log.e(TAG, "LITE-RENDER DEVOLVEU REDIRECT.");
                Log.e(TAG, "Location = " + location);
                Log.e(TAG, "Se Location comecar com https://, o servidor esta forcando HTTPS.");
                return;
            }

            if (responseCode < 200 || responseCode >= 300) {
                Log.e(TAG, "LITE-RENDER RETORNOU HTTP " + responseCode);
                input = connection.getErrorStream();
                if (input != null) {
                    byte[] errorBytes = readAllBytes(input);
                    Log.e(TAG, "Resposta de erro: " + new String(errorBytes, "UTF-8"));
                }
                return;
            }

            input = connection.getInputStream();
            output = new ByteArrayOutputStream();

            byte[] buffer = new byte[4096];
            int count;
            int total = 0;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
                total += count;
            }

            byte[] rawBytes = output.toByteArray();

            Log.d(TAG, "BYTES RECEBIDOS: " + total);

            if (rawBytes.length == 0) {
                Log.e(TAG, "LITE-RENDER respondeu 200, mas enviou 0 bytes.");
                return;
            }

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.length, bounds);

            Log.d(TAG, "BITMAP BOUNDS: " + bounds.outWidth + "x" + bounds.outHeight
                    + " MIME=" + bounds.outMimeType);

            final Bitmap decodedBitmap = BitmapFactory.decodeByteArray(
                    rawBytes,
                    0,
                    rawBytes.length
            );

            if (decodedBitmap == null) {
                Log.e(TAG, "BitmapFactory retornou NULL.");
                Log.e(TAG, "Content-Type recebido: " + contentType);
                return;
            }

            Log.d(TAG, "BITMAP OK: " + decodedBitmap.getWidth() + "x"
                    + decodedBitmap.getHeight());

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    targetView.setImageBitmap(decodedBitmap);
                    targetView.setVisibility(View.VISIBLE);
                    targetView.requestLayout();
                    Log.d(TAG, "Bitmap aplicado no ImageView.");
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "FALHA REAL NO DOWNLOAD DA IMAGEM: " + e.getClass().getName());
            Log.e(TAG, "Mensagem: " + e.getMessage());
            Log.e(TAG, "URL que falhou: " + liteRenderUrl, e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignored) {
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    public void LoadImageUrl(final String imageUrl, final ImageView targetView) {

        Log.d(TAG, "LoadImageUrl: " + imageUrl);

        new Thread(new Runnable() {
            @Override
            public void run() {
                processImageData(imageUrl, targetView);
            }
        }).start();
    }

    public void viewProfilePicture(final Context context, final String username,
                                   final ImageView targetImageView) {

        if (context == null || username == null || username.trim().isEmpty()
                || targetImageView == null) {

            Log.e(TAG, "viewProfilePicture: parametros invalidos.");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    config configInstance = new config();

                    String rawConfig = configInstance.loadCfgAsJson(
                            context,
                            "config.cfg"
                    );

                    JSONObject jsonConfig = new JSONObject(rawConfig);
                    JSONObject serverConfig = null;

                    if (jsonConfig.has("SERVER")) {
                        serverConfig = jsonConfig.getJSONObject("SERVER");
                    } else if (jsonConfig.has("server")) {
                        serverConfig = jsonConfig.getJSONObject("server");
                    }

                    if (serverConfig == null) {
                        Log.e(TAG, "Could not find 'server' or 'SERVER' key in config.cfg");
                        return;
                    }

                    String baseUrl = serverConfig.optString("url", "").trim();

                    while (baseUrl.endsWith("/")) {
                        baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                    }

                    if (baseUrl.isEmpty()) {
                        Log.e(TAG, "Base URL is empty in server config.");
                        return;
                    }

                    JSONObject requestBody = new JSONObject();
                    requestBody.put("username", username.replace("@", ""));

                    Log.d(TAG, "Buscando foto de perfil de: " + username);

                    String jsonResponse = request.requestHTTP(
                            baseUrl + "/view-profile-picture",
                            "POST",
                            requestBody,
                            context
                    );

                    if (jsonResponse != null && !jsonResponse.trim().isEmpty()) {

                        JSONObject responseData = new JSONObject(jsonResponse);

                        if (!responseData.isNull("profile-picture")) {

                            String avatarUrl = responseData
                                    .optString("profile-picture", "")
                                    .trim();

                            if (!avatarUrl.isEmpty() && !avatarUrl.equalsIgnoreCase("null")) {

                                String finalAvatarUrl = avatarUrl;

                                // Se for caminho interno, completa com o servidor.
                                if (!avatarUrl.startsWith("http://")
                                        && !avatarUrl.startsWith("https://")) {

                                    if (!avatarUrl.startsWith("/")) {
                                        avatarUrl = "/" + avatarUrl;
                                    }

                                    finalAvatarUrl = baseUrl + avatarUrl;
                                }

                                // processImageData tambem envia pelo lite-render.
                                processImageData(finalAvatarUrl, targetImageView);
                            }
                        }
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "JSONException in viewProfilePicture: " + e.getMessage(), e);
                } catch (Exception e) {
                    Log.e(TAG, "Exception in viewProfilePicture: " + e.getMessage(), e);
                }
            }
        }).start();
    }

    /*
     * Mantida para compatibilidade com codigo existente.
     * O encaminhamento real para o lite-render agora acontece em
     * buildLiteRenderUrl()/processImageData().
     */
    private String sanitizeAndEncodeUrl(String rawUrl) {
        if (rawUrl == null) {
            return "";
        }
        return rawUrl.replace("&amp;", "&").trim();
    }
}
