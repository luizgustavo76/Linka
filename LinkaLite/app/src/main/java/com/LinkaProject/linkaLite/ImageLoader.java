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

    /**
     * Decodifica o array de bytes em um Bitmap de forma segura para o Dalvik VM (Android 2.3).
     * Usa inSampleSize para calcular o encolhimento e RGB_565 para cortar o uso de RAM pela metade.
     */
    private Bitmap decodeSampledBitmapFromByteArray(byte[] data, int reqWidth, int reqHeight) {
        if (data == null || data.length == 0) {
            return null;
        }

        try {
            // 1. Apenas lê as dimensões sem alocar os pixels na memória
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, options);

            // 2. Calcula o inSampleSize (fator de encolhimento)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // 3. Configura para ler a imagem reduzida com formato de cor 16-bit (RGB_565)
            // Economiza 50% da memória comparado ao formato padrão ARGB_8888
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inPurgeable = true; // Permite que a VM libere o bitmap se precisar de memória
            options.inInputShareable = true;

            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        } catch (OutOfMemoryError e) {
            // Se mesmo assim faltar RAM, força a coleta de lixo e retorna null sem crashar o app
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

    public void processImageData(final String imageUrl, final ImageView targetView) {
        if (targetView == null || imageUrl == null || imageUrl.trim().isEmpty() || imageUrl.equals("null")) {
            return;
        }

        Context appContext = targetView.getContext();
        byte[] rawBytes = request.requestBytes(imageUrl, "GET", appContext);

        if (rawBytes != null && rawBytes.length > 0) {
            // Decodifica a imagem limitando a dimensão para 144x144 pixels em memória
            final Bitmap decodedBitmap = decodeSampledBitmapFromByteArray(rawBytes, 144, 144);
            
            if (decodedBitmap != null) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Limpa o bitmap anterior do ImageView se existir para evitar vazamento de RAM
                        if (targetView.getDrawable() != null) {
                            targetView.setImageDrawable(null);
                        }
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