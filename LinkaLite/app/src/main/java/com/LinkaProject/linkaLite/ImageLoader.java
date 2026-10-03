package com.LinkaProject.linkaLite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    // Executor unico e estatico para toda a aplicacao
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    private int[] getDensityBasedDimensions(Context context, boolean isAvatar) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float density = metrics.density;

        if (isAvatar) {
            int sizePx = Math.round(56 * density);
            return new int[]{sizePx, sizePx};
        } else {
            int targetWidth = metrics.widthPixels;
            int targetHeight = metrics.heightPixels;
            return new int[]{targetWidth, targetHeight};
        }
    }

    private Bitmap decodeSampledBitmapFromByteArray(byte[] data, int reqWidth, int reqHeight) {
        if (data == null || data.length == 0) return null;

        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inPurgeable = true;
            options.inInputShareable = true;

            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        } catch (OutOfMemoryError e) {
            System.gc();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public void LoadImageUrl(final String imageUrl, final ImageView targetView) {
        if (targetView == null || imageUrl == null || imageUrl.trim().isEmpty() || imageUrl.equals("null")) {
            return;
        }

        // Define a TAG imediatamente na UI Thread
        targetView.setTag(imageUrl);
        final Context appContext = targetView.getContext();

        executor.submit(new Runnable() {
            @Override
            public void run() {
                if (!imageUrl.equals(targetView.getTag())) return;

                int[] dims = getDensityBasedDimensions(appContext, false);
                byte[] rawBytes = request.requestBytes(imageUrl, "GET", appContext);

                if (rawBytes != null && rawBytes.length > 0) {
                    final Bitmap decodedBitmap = decodeSampledBitmapFromByteArray(rawBytes, dims[0], dims[1]);

                    if (decodedBitmap != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (imageUrl.equals(targetView.getTag())) {
                                    targetView.setImageBitmap(decodedBitmap);
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    public void viewProfilePicture(final Context context, final String username, final ImageView targetImageView) {
        if (targetImageView == null || username == null || username.trim().isEmpty()) {
            return;
        }

        final String tagKey = "avatar_" + username;
        // Define a TAG do avatar imediatamente na UI Thread
        targetImageView.setTag(tagKey);

        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!tagKey.equals(targetImageView.getTag())) return;

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

                    if (!tagKey.equals(targetImageView.getTag())) return;

                    if (jsonResponse != null && !jsonResponse.trim().isEmpty()) {
                        JSONObject responseData = new JSONObject(jsonResponse);

                        if (!responseData.isNull("profile-picture")) {
                            String avatarUrl = responseData.optString("profile-picture", "");

                            if (!avatarUrl.isEmpty() && !avatarUrl.equals("null")) {
                                String proxyUrl = baseUrl + "/lite-render?url=" + URLEncoder.encode(avatarUrl, "UTF-8");

                                int[] dims = getDensityBasedDimensions(context, true);
                                byte[] rawBytes = request.requestBytes(proxyUrl, "GET", context);

                                if (rawBytes != null && rawBytes.length > 0) {
                                    final Bitmap decodedBitmap = decodeSampledBitmapFromByteArray(rawBytes, dims[0], dims[1]);

                                    if (decodedBitmap != null) {
                                        mainHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (tagKey.equals(targetImageView.getTag())) {
                                                    targetImageView.setImageBitmap(decodedBitmap);
                                                }
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }
}