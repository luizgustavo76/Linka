package com.LinkaProject.linkaLite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Movie;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;
import com.LinkaProject.linkaLite.GifImageView;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {

    private static final String TAG = "ImageLoader";
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final HashMap<String, SoftReference<Bitmap>> memoryCache = new HashMap<String, SoftReference<Bitmap>>();
    private static final HashMap<String, String> avatarUrlCache = new HashMap<String, String>();
    private static String cachedBaseUrl = null;

    public ImageLoader() {
    }

    private static synchronized String getBaseUrl(Context appContext) {
        if (cachedBaseUrl != null) return cachedBaseUrl;
        try {
            config configInstance = new config();
            String rawConfig = configInstance.loadCfgAsJson(appContext, "config.cfg");
            JSONObject jsonConfig = new JSONObject(rawConfig);
            JSONObject serverConfig = jsonConfig.getJSONObject("SERVER");
            String baseUrl = serverConfig.getString("url");
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            cachedBaseUrl = baseUrl;
            return cachedBaseUrl;
        } catch (Exception e) {
            return "http://linkaProject.pythonanywhere.com";
        }
    }

    private int getTargetWidth(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.widthPixels > 0 ? metrics.widthPixels : 320;
    }

    private String cleanAndPrepareUrl(String rawUrl, int targetWidth, Integer targetHeight, int quality) {
        if (rawUrl == null || rawUrl.trim().length() == 0) return "";

        String cleanUrl = rawUrl.trim();

        while (cleanUrl.contains("wsrv.nl/?url=")) {
            try {
                int urlIndex = cleanUrl.indexOf("url=") + 4;
                int endParamIndex = cleanUrl.indexOf("&", urlIndex);
                if (endParamIndex != -1) {
                    cleanUrl = cleanUrl.substring(urlIndex, endParamIndex);
                } else {
                    cleanUrl = cleanUrl.substring(urlIndex);
                }
                cleanUrl = URLDecoder.decode(cleanUrl, "UTF-8");
            } catch (Exception e) {
                break;
            }
        }

        if (cleanUrl.toLowerCase().contains(".gif")) {
            return cleanUrl;
        }

        if (cleanUrl.contains("unavatar.io") ||
            cleanUrl.contains("ui-avatars.com") ||
            cleanUrl.contains("cdninstagram.com") ||
            cleanUrl.contains("nitter") ||
            cleanUrl.contains("twimg.com")) {
            return cleanUrl;
        }

        if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            try {
                try {
                    cleanUrl = URLDecoder.decode(cleanUrl, "UTF-8");
                } catch (Exception ignored) {}

                StringBuilder builder = new StringBuilder("http://wsrv.nl/?url=");
                builder.append(URLEncoder.encode(cleanUrl, "UTF-8"));
                builder.append("&w=").append(targetWidth);
                if (targetHeight != null && targetHeight > 0) {
                    builder.append("&h=").append(targetHeight).append("&fit=cover");
                }
                builder.append("&q=").append(quality).append("&output=jpg");
                return builder.toString();
            } catch (Exception e) {
                return cleanUrl;
            }
        }

        return cleanUrl;
    }

    private String md5Key(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                String hex = Integer.toHexString(0xff & digest[i]);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    private byte[] readFromDiskCache(Context context, String key) {
        try {
            File cacheDir = new File(context.getCacheDir(), "linka_img_cache");
            File file = new File(cacheDir, md5Key(key));
            if (!file.exists() || file.length() == 0) return null;

            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            int offset = 0;
            int read;
            while (offset < data.length && (read = fis.read(data, offset, data.length - offset)) != -1) {
                offset += read;
            }
            fis.close();
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    private void writeToDiskCache(Context context, String key, byte[] data) {
        if (data == null || data.length == 0) return;
        try {
            File cacheDir = new File(context.getCacheDir(), "linka_img_cache");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            File file = new File(cacheDir, md5Key(key));
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.flush();
            fos.close();
        } catch (Exception ignored) {}
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

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth) {
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (width > reqWidth && reqWidth > 0) {
            final int halfWidth = width / 2;
            while ((halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private Bitmap decodeSampledBitmap(byte[] data, int reqWidth) {
        if (data == null || data.length == 0) return null;

        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inDither = true;
            options.inPurgeable = true;
            options.inInputShareable = true;

            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "OutOfMemoryError ao decodificar imagem! Limpando cache.");
            memoryCache.clear();
            System.gc();
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao decodificar imagem", e);
            return null;
        }
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
                if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
                    targetView.setImageBitmap(cachedBitmap);
                    return;
                }
            }
        }

        targetView.setImageResource(android.R.color.transparent);
        final Context appContext = targetView.getContext().getApplicationContext();

        executor.submit(new Runnable() {
            @Override
            public void run() {
                if (!imageUrl.equals(targetView.getTag())) return;

                try {
                    int targetWidth = getTargetWidth(appContext);
                    String fetchUrl = cleanAndPrepareUrl(imageUrl, targetWidth, null, 75);

                    byte[] rawBytes = readFromDiskCache(appContext, fetchUrl);
                    if (rawBytes == null || rawBytes.length == 0) {
                        rawBytes = request.requestBytes(fetchUrl, "GET", appContext);
                        if (rawBytes != null && rawBytes.length > 0) {
                            writeToDiskCache(appContext, fetchUrl, rawBytes);
                        }
                    }

                    if (rawBytes != null && rawBytes.length > 0) {
                        if (!imageUrl.equals(targetView.getTag())) return;

                        if (isGif(imageUrl, rawBytes) && targetView instanceof GifImageView) {
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

                        final Bitmap decodedBitmap = decodeSampledBitmap(rawBytes, targetWidth);

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
                    } else {
                        Log.e(TAG, "[LoadImageUrl] Retorno dos bytes nulo ou 0 para a URL: " + fetchUrl);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Excecao no LoadImageUrl", e);
                }
            }
        });
    }

    private void applyFallbackAvatar(final Context appContext, final String username, final String tagKey, final ImageView targetImageView) {
        DisplayMetrics metrics = appContext.getResources().getDisplayMetrics();
        int avatarPx = Math.round(64 * metrics.density);
        final Bitmap fallback = AvatarGenerator.generateAvatar(username, avatarPx);

        if (fallback != null) {
            memoryCache.put(tagKey, new SoftReference<Bitmap>(fallback));
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (tagKey.equals(targetImageView.getTag())) {
                        targetImageView.setImageBitmap(fallback);
                    }
                }
            });
        }
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
                if (cachedAvatar != null && !cachedAvatar.isRecycled()) {
                    targetImageView.setImageBitmap(cachedAvatar);
                    return;
                }
            }
        }

        final Context appContext = context.getApplicationContext();
        DisplayMetrics metrics = appContext.getResources().getDisplayMetrics();
        final int avatarPx = Math.round(64 * metrics.density);

        Bitmap initialPlaceholder = AvatarGenerator.generateAvatar(username, avatarPx);
        if (initialPlaceholder != null) {
            targetImageView.setImageBitmap(initialPlaceholder);
        } else {
            targetImageView.setImageResource(android.R.color.transparent);
        }

        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!tagKey.equals(targetImageView.getTag())) return;

                    String avatarUrl = avatarUrlCache.get(username);

                    if (avatarUrl == null || avatarUrl.length() == 0) {
                        String baseUrl = getBaseUrl(appContext);
                        JSONObject requestBody = new JSONObject();
                        requestBody.put("username", username);

                        String jsonResponse = request.requestHTTP(baseUrl + "/view-profile-picture", "POST", requestBody, appContext);
                        if (!tagKey.equals(targetImageView.getTag())) return;

                        if (jsonResponse != null && jsonResponse.trim().length() > 0) {
                            JSONObject responseData = new JSONObject(jsonResponse);
                            if (!responseData.isNull("profile-picture")) {
                                avatarUrl = responseData.optString("profile-picture", "");
                                if (avatarUrl.length() > 0 && !avatarUrl.equals("null")) {
                                    avatarUrlCache.put(username, avatarUrl);
                                }
                            }
                        }
                    }

                    if (avatarUrl != null && avatarUrl.length() > 0 && !avatarUrl.equals("null")) {
                        String fetchUrl = cleanAndPrepareUrl(avatarUrl, avatarPx, avatarPx, 80);

                        byte[] rawBytes = readFromDiskCache(appContext, fetchUrl);
                        if (rawBytes == null || rawBytes.length == 0) {
                            rawBytes = request.requestBytes(fetchUrl, "GET", appContext);
                            if (rawBytes != null && rawBytes.length > 0) {
                                writeToDiskCache(appContext, fetchUrl, rawBytes);
                            }
                        }

                        if (!tagKey.equals(targetImageView.getTag())) return;

                        if (rawBytes != null && rawBytes.length > 0) {
                            final Bitmap decodedBitmap = decodeSampledBitmap(rawBytes, avatarPx);
                            if (decodedBitmap != null) {
                                memoryCache.put(tagKey, new SoftReference<Bitmap>(decodedBitmap));
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (tagKey.equals(targetImageView.getTag())) {
                                            targetImageView.setImageBitmap(decodedBitmap);
                                        }
                                    }
                                });
                                return;
                            }
                        }
                    }

                    applyFallbackAvatar(appContext, username, tagKey, targetImageView);

                } catch (Exception e) {
                    Log.e(TAG, "Excecao no viewProfilePicture", e);
                    applyFallbackAvatar(appContext, username, tagKey, targetImageView);
                }
            }
        });
    }
}