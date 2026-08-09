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
    private final Handler handler = new Handler(Looper.getMainLooper());

    public void LoadImageUrl(final String urlString, final ImageView imageView) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (imageView == null || urlString == null || urlString.trim().isEmpty() || urlString.equals("null")) {
                    return;
                }

                Context context = imageView.getContext();
                byte[] imageData = request.requestBytes(urlString, "GET", context);

                if (imageData != null && imageData.length > 0) {
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(bitmap);
                            imageView.requestLayout();
                        }
                    });
                }
            }
        }).start();
    }

    public void viewProfilePicture(Context context, String username, ImageView targetImageView) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    config cfg = new config();
                    String rawCfg = cfg.loadCfgAsJson(context, "config.cfg");
                    JSONObject jsonCfg = new JSONObject(rawCfg);
                    JSONObject server = jsonCfg.getJSONObject("SERVER");
                    String url = server.getString("url");
                    JSONObject jsonProfile = new JSONObject();
                    jsonProfile.put("username", username);
                    String responseString = request.requestHTTP(url + "/view-profile-picture", "POST", jsonProfile, context);
                    if (responseString != null && !responseString.trim().isEmpty()) {
                        JSONObject responseJson = new JSONObject(responseString);
                        if (!responseJson.isNull("profile-picture")) {
                            String avatarUrl = responseJson.optString("profile-picture", "");
                            if (!avatarUrl.isEmpty() && !avatarUrl.equals("null")) {
                                String proxyUrl = url + "/lite-render?url=" + URLEncoder.encode(avatarUrl, "UTF-8");
                                LoadImageUrl(proxyUrl, targetImageView);
                            }
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}