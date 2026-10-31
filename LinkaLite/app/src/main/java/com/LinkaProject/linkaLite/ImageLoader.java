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
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {

    private static final String TAG = "ImageLoader";
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final ExecutorService executor = Executors.newFixedThreadPool(1);
    private static final HashMap<String, SoftReference<Bitmap>> memoryCache = new HashMap<String, SoftReference<Bitmap>>();
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
        int width = metrics.widthPixels > 0 ? metrics.widthPixels : 320;
        int height = metrics.heightPixels > 0 ? metrics.heightPixels : 480;
        
        Log.d(TAG, "Resolucao detectada: " + width + "x" + height + " | Densidade (density): " + metrics.density);
        return width;
    }

    /**
     * Limpa URLs encadeadas/duplicadas do wsrv.nl vindas do backend
     * e aplica o proxy wsrv.nl com as dimensoes reais e qualidade 85/90.
     */
    private String cleanAndPrepareUrl(String rawUrl, int targetWidth, Integer targetHeight, int quality) {
        if (rawUrl == null || rawUrl.trim().length() == 0) return "";
        
        String cleanUrl = rawUrl.trim();

        // Se a URL ja veio envelopada com wsrv.nl, desfaz o encadeamento para pegar a URL original
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

        // Se for midia local (/midia/), nao usa wsrv.nl
        if (cleanUrl.contains("/midia/")) {
            return cleanUrl;
        }

        // Se for URL externa via HTTP/HTTPS, envelopa no wsrv.nl limpo
        if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            try {
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

    private boolean isGif(String url, byte[] data) {
        if (url != null && url.toLowerCase().endsWith(".gif")) {
            return true;
        }
        if (data != null && data.length >= 6) {
            return data[0] == 'G' && data[1] == 'I' && data[2] == 'F';
        }
        return false;
    }

    private Bitmap decodeByteArrayDirect(byte[] data) {
        if (data == null || data.length == 0) return null;

        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1; 
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
                if (cachedBitmap != null) {
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
                    String fetchUrl = cleanAndPrepareUrl(imageUrl, targetWidth, null, 85);

                    Log.d(TAG, "Requisitando imagem na URL: " + fetchUrl);

                    byte[] rawBytes = request.requestBytes(fetchUrl, "GET", appContext);

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

                        final Bitmap decodedBitmap = decodeByteArrayDirect(rawBytes);

                        if (decodedBitmap != null) {
                            Log.d(TAG, "Imagem decodificada com sucesso! Dims reais do Bitmap: " + decodedBitmap.getWidth() + "x" + decodedBitmap.getHeight());
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
                } catch (Exception e) {
                    Log.e(TAG, "Excecao no LoadImageUrl", e);
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
        final Context appContext = context.getApplicationContext();

        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!tagKey.equals(targetImageView.getTag())) return;

                    String baseUrl = getBaseUrl(appContext);
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("username", username);

                    String jsonResponse = request.requestHTTP(baseUrl + "/view-profile-picture", "POST", requestBody, appContext);

                    if (!tagKey.equals(targetImageView.getTag())) return;

                    if (jsonResponse != null && jsonResponse.trim().length() > 0) {
                        JSONObject responseData = new JSONObject(jsonResponse);

                        if (!responseData.isNull("profile-picture")) {
                            String avatarUrl = responseData.optString("profile-picture", "");

                            if (avatarUrl.length() > 0 && !avatarUrl.equals("null")) {

                                DisplayMetrics metrics = appContext.getResources().getDisplayMetrics();
                                int avatarPx = Math.round(64 * metrics.density);

                                Log.d(TAG, "Calculando dimensao do Avatar: " + avatarPx + "px");

                                String fetchUrl = cleanAndPrepareUrl(avatarUrl, avatarPx, avatarPx, 90);

                                byte[] rawBytes = request.requestBytes(fetchUrl, "GET", appContext);

                                if (!tagKey.equals(targetImageView.getTag())) return;

                                if (rawBytes != null && rawBytes.length > 0) {
                                    final Bitmap decodedBitmap = decodeByteArrayDirect(rawBytes);
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
                } catch (Exception e) {
                    Log.e(TAG, "Excecao no viewProfilePicture", e);
                }
            }
        });
    }
}