package com.LinkaProject.linkaLite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

public class ImageLoader {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void processImageData(final String imageUrl, final ImageView targetView) {
        if (targetView == null || imageUrl == null || imageUrl.trim().isEmpty() || imageUrl.equals("null")) {
            return;
        }

        Context appContext = targetView.getContext();
        byte[] rawBytes = request.requestBytes(imageUrl, "GET", appContext);

        if (rawBytes != null && rawBytes.length > 0) {
            final Bitmap decodedBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.length);
            
            if (decodedBitmap != null) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        targetView.setImageBitmap(decodedBitmap);
                        targetView.requestLayout();
                    }
                });
            }
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    config configInstance = new config();
                    String rawConfig = configInstance.loadCfgAsJson(context, "config.cfg");
                    JSONObject jsonConfig = new JSONObject(rawConfig);
                    JSONObject serverConfig = jsonConfig.getJSONObject("SERVER");
                    
                    String baseUrl = serverConfig.getString("url");
                    if (baseUrl.endsWith("/")) {
                        baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                    }

                    JSONObject requestBody = new JSONObject();
                    requestBody.put("username", username);

                    String jsonResponse = request.requestHTTP(baseUrl + "/view-profile-picture", "POST", requestBody, context);

                    if (jsonResponse != null && !jsonResponse.trim().isEmpty()) {
                        JSONObject responseData = new JSONObject(jsonResponse);
                        
                        if (!responseData.isNull("profile-picture")) {
                            String avatarUrl = responseData.optString("profile-picture", "");
                            
                            if (!avatarUrl.isEmpty() && !avatarUrl.equals("null")) {
                                String proxyUrl = baseUrl + "/lite-render?url=" + URLEncoder.encode(avatarUrl, "UTF-8");
                                processImageData(proxyUrl, targetImageView);
                            }
                        }
                    }
                } catch (JSONException ignored) {
                } catch (Exception ignored) {
                }
            }
        }).start();
    }
}