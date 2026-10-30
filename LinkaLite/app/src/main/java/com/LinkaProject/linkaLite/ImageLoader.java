package com.LinkaProject.linkaLite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Movie;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.widget.ImageView;
import com.LinkaProject.linkaLite.GifImageView;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    private static final HashMap<String, SoftReference<Bitmap>> memoryCache = new HashMap<String, SoftReference<Bitmap>>();
    private static final ConcurrentHashMap<String, byte[]> gifCache = new ConcurrentHashMap<String, byte[]>();

    public ImageLoader() {
    }

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

    private boolean isGif(String url, byte[] data) {
        if (url != null && url.toLowerCase().endsWith(".gif")) {
            return true;
        }
        if (data != null && data.length >= 6) {
            return data[0] == 'G' && data[1] == 'I' && data[2] == 'F';
        }
        return false;
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
        if (targetView == null || imageUrl == null || imageUrl.trim().length() == 0 || imageUrl.equals("null")) {
            return;
        }

        targetView.setTag(imageUrl);

        if (memoryCache.containsKey(imageUrl)) {
            SoftReference<Bitmap> ref = memoryCache.get(imageUrl);
            if (ref != null) {
                Bitmap cachedBitmap = ref.get();
                if (cachedBitmap != null) {
                    targetView.setImageBitmap(cachedBitmap);
                    return;
                }
            }
        }

        if (targetView instanceof GifImageView && gifCache.containsKey(imageUrl)) {
            byte[] rawBytes = gifCache.get(imageUrl);
            InputStream is = new ByteArrayInputStream(rawBytes);
            Movie gifMovie = Movie.decodeStream(is);
            if (gifMovie != null) {
                ((GifImageView) targetView).setMovie(gifMovie);
                return;
            }
        }

        targetView.setImageResource(android.R.color.transparent);

        final Context appContext = targetView.getContext();

        executor.submit(new Runnable() {
            @Override
            public void run() {
                if (!imageUrl.equals(targetView.getTag())) return;

                try {
                    config configInstance = new config();
                    String rawConfig = configInstance.loadCfgAsJson(appContext, "config.cfg");
                    JSONObject jsonConfig = new JSONObject(rawConfig);
                    JSONObject serverConfig = jsonConfig.getJSONObject("SERVER");

                    String baseUrl = serverConfig.getString("url");
                    if (baseUrl.endsWith("/")) {
                        baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                    }

                    String fetchUrl = imageUrl;
                    if (!imageUrl.contains("/midia/") && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
                        fetchUrl = baseUrl + "/lite-render?url=" + URLEncoder.encode(imageUrl, "UTF-8");
                    }

                    int[] dims = getDensityBasedDimensions(appContext, false);
                    byte[] rawBytes = request.requestBytes(fetchUrl, "GET", appContext);

                    if (rawBytes != null && rawBytes.length > 0) {
                        if (!imageUrl.equals(targetView.getTag())) return;

                        if (isGif(imageUrl, rawBytes) && targetView instanceof GifImageView) {
                            gifCache.put(imageUrl, rawBytes);
                            final GifImageView gifTarget = (GifImageView) targetView;
                            InputStream is = new ByteArrayInputStream(rawBytes);
                            final Movie gifMovie = Movie.decodeStream(is);

                            if (gifMovie != null) {
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (imageUrl.equals(targetView.getTag())) {
                                            gifTarget.setMovie(gifMovie);
                                        }
                                    }
                                });
                                return;
                            }
                        }

                        final Bitmap decodedBitmap = decodeSampledBitmapFromByteArray(rawBytes, dims[0], dims[1]);

                        if (decodedBitmap != null) {
                            memoryCache.put(imageUrl, new SoftReference<Bitmap>(decodedBitmap));
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
                } catch (Exception ignored) {
                }
            }
        });
    }

    public void viewProfilePicture(final Context context, final String username, final ImageView targetImageView) {
        if (targetImageView == null || username == null || username.trim().length() == 0) {
            return;
        }

        final String tagKey = "avatar_" + username;
        targetImageView.setTag(tagKey);

        if (memoryCache.containsKey(tagKey)) {
            SoftReference<Bitmap> ref = memoryCache.get(tagKey);
            if (ref != null) {
                Bitmap cachedAvatar = ref.get();
                if (cachedAvatar != null) {
                    targetImageView.setImageBitmap(cachedAvatar);
                    return;
                }
            }
        }

        targetImageView.setImageResource(android.R.color.transparent);

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

                    if (jsonResponse != null && jsonResponse.trim().length() > 0) {
                        JSONObject responseData = new JSONObject(jsonResponse);

                        if (!responseData.isNull("profile-picture")) {
                            String avatarUrl = responseData.optString("profile-picture", "");

                            if (avatarUrl.length() > 0 && !avatarUrl.equals("null")) {

                                String fetchUrl = avatarUrl;
                                if (!avatarUrl.contains("/midia/")) {
                                    fetchUrl = baseUrl + "/lite-render?url=" + URLEncoder.encode(avatarUrl, "UTF-8");
                                }

                                int[] dims = getDensityBasedDimensions(context, true);
                                byte[] rawBytes = request.requestBytes(fetchUrl, "GET", context);

                                if (!tagKey.equals(targetImageView.getTag())) return;

                                if (rawBytes != null && rawBytes.length > 0) {
                                    final Bitmap decodedBitmap = decodeSampledBitmapFromByteArray(rawBytes, dims[0], dims[1]);
                                    if (decodedBitmap != null) {
                                        final Bitmap circularBitmap = profile.getCircularBitmap(decodedBitmap);
                                        memoryCache.put(tagKey, new SoftReference<Bitmap>(circularBitmap));
                                        mainHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (tagKey.equals(targetImageView.getTag())) {
                                                    targetImageView.setImageBitmap(circularBitmap);
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