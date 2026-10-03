package com.LinkaProject.linkaLite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    /**
     * Calcula as dimensões em pixels com base na densidade do display do celular.
     */
    private int[] getDensityBasedDimensions(Context context, boolean isAvatar) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float density = metrics.density; // ex: 1.0 (mdpi), 1.5 (hdpi), 2.0 (xhdpi), 3.0 (xxhdpi)

        if (isAvatar) {
            // 56dp convertidos para pixels baseados na densidade da tela
            int sizePx = Math.round(56 * density);
            return new int[]{sizePx, sizePx};
        } else {
            // Usa a largura exata da tela como resolução máxima em pixels
            int targetWidth = metrics.widthPixels;
            int targetHeight = metrics.heightPixels;
            return new int[]{targetWidth, targetHeight};
        }
    }

    private Bitmap decodeSampledBitmapFromByteArray(byte[] data, int reqWidth, int reqHeight) {
        if (data == null || data.length == 0) {
            return null;
        }

        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // Economiza 50% RAM
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

    public void processImageData(final String imageUrl, final ImageView targetView, final boolean isAvatar) {
        if (targetView == null || imageUrl == null || imageUrl.trim().isEmpty() || imageUrl.equals("null")) {
            return;
        }

        targetView.setTag(imageUrl);
        Context appContext = targetView.getContext();

        // Obtém resolução ideal calculada pela densidade do display
        int[] dims = getDensityBasedDimensions(appContext, isAvatar);
        int reqWidth = dims[0];
        int reqHeight = dims[1];

        byte[] rawBytes = request.requestBytes(imageUrl, "GET", appContext);

        if (rawBytes != null && rawBytes.length > 0) {
            final Bitmap decodedBitmap = decodeSampledBitmapFromByteArray(rawBytes, reqWidth, reqHeight);
            
            if (decodedBitmap != null) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (imageUrl.equals(targetView.getTag())) {
                            if (targetView.getDrawable() != null) {
                                targetView.setImageDrawable(null);
                            }
                            targetView.setImageBitmap(decodedBitmap);
                            targetView.requestLayout();
                        }
                    }
                });
            }
        }
    }

    public void LoadImageUrl(final String imageUrl, final ImageView targetView) {
        if (targetView != null) {
            targetView.setTag(imageUrl);
        }
        
        executor.submit(new Runnable() {
            @Override
            public void run() {
                processImageData(imageUrl, targetView, false); // Post (largura da tela)
            }
        });
    }

    public void viewProfilePicture(final Context context, final String username, final ImageView targetImageView) {
        executor.submit(new Runnable() {
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
                                processImageData(proxyUrl, targetImageView, true); // Avatar (56dp em px)
                            }
                        }
                    }
                } catch (JSONException ignored) {
                } catch (Exception ignored) {
                }
            }
        });
    }
}