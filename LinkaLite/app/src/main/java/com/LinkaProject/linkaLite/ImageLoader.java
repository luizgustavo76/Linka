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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ImageLoader {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final String TAG = "LINKA_IMG_LOADER";

    public void processImageData(String imageUrl, final ImageView targetView) {
        if (targetView == null || imageUrl == null || imageUrl.trim().isEmpty() || imageUrl.equalsIgnoreCase("null")) {
            Log.e(TAG, "TargetView is null or invalid Image URL received.");
            return;
        }

        final String cleanUrl = sanitizeAndEncodeUrl(imageUrl);
        final Context appContext = targetView.getContext();
        
        Log.d(TAG, "Downloading image bytes from: " + cleanUrl);

        byte[] rawBytes = request.requestBytes(cleanUrl, "GET", appContext);

        if (rawBytes != null && rawBytes.length > 0) {
            Log.d(TAG, "Downloaded " + rawBytes.length + " bytes. Decoding bitmap...");
            final Bitmap decodedBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.length);

            if (decodedBitmap != null) {
                Log.d(TAG, "Bitmap decoded successfully! Dimensions: " + decodedBitmap.getWidth() + "x" + decodedBitmap.getHeight());
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        targetView.setImageBitmap(decodedBitmap);
                        targetView.setVisibility(View.VISIBLE);
                        targetView.requestLayout();
                    }
                });
            } else {
                Log.e(TAG, "BitmapFactory failed to decode byte array (data corrupted or unsupported format).");
            }
        } else {
            Log.e(TAG, "requestBytes returned null or 0 bytes for URL: " + cleanUrl);
        }
    }

    public void LoadImageUrl(final String imageUrl, final ImageView targetView) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                processImageData(imageUrl, targetView);
            }
        }).start();
    }

    public void viewProfilePicture(final Context context, final String username, final ImageView targetImageView) {
        if (context == null || username == null || username.trim().isEmpty() || targetImageView == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    config configInstance = new config();
                    String rawConfig = configInstance.loadCfgAsJson(context, "config.cfg");
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
                    if (baseUrl.endsWith("/")) {
                        baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                    }

                    if (baseUrl.isEmpty()) {
                        Log.e(TAG, "Base URL is empty in server config.");
                        return;
                    }

                    JSONObject requestBody = new JSONObject();
                    requestBody.put("username", username.replace("@", ""));

                    String jsonResponse = request.requestHTTP(baseUrl + "/view-profile-picture", "POST", requestBody, context);

                    if (jsonResponse != null && !jsonResponse.trim().isEmpty()) {
                        JSONObject responseData = new JSONObject(jsonResponse);

                        if (!responseData.isNull("profile-picture")) {
                            String avatarUrl = responseData.optString("profile-picture", "").trim();

                            if (!avatarUrl.isEmpty() && !avatarUrl.equalsIgnoreCase("null")) {
                                String finalAvatarUrl = avatarUrl;
                                
                                // Se a foto de perfil for uma URL interna do servidor, chama direto sem passar pelo lite-render
                                if (!avatarUrl.startsWith("http://") && !avatarUrl.startsWith("https://")) {
                                    if (!avatarUrl.startsWith("/")) {
                                        avatarUrl = "/" + avatarUrl;
                                    }
                                    finalAvatarUrl = baseUrl + avatarUrl;
                                }

                                processImageData(finalAvatarUrl, targetImageView);
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSONException in viewProfilePicture: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "Exception in viewProfilePicture: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Sanitiza a URL e intercepta chamadas para o lite-render garantindo o encode do parâmetro.
     */
    private String sanitizeAndEncodeUrl(String rawUrl) {
        if (rawUrl == null) return "";

        String clean = rawUrl.replace("&amp;", "&").trim();

        if (clean.contains("/lite-render?url=")) {
            String[] parts = clean.split("/lite-render\\?url=");
            if (parts.length > 1) {
                String base = parts[0];
                String targetParam = parts[1];

                // Se o parâmetro interno ainda não estiver codificado em URL (não começa com http%3A)
                if (!targetParam.startsWith("http%3A") && !targetParam.startsWith("https%3A")) {
                    try {
                        return base + "/lite-render?url=" + URLEncoder.encode(targetParam, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Encoding exception: " + e.getMessage());
                    }
                }
            }
        }
        return clean;
    }
}