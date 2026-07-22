package com.LinkaProject.linkaLite;

import android.content.Context; // Adicionado para poder ler o arquivo de config
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.widget.ImageView;

import org.json.JSONException;  // Import do JSON
import org.json.JSONObject;     // Import do JSON

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageLoader {
    private final Handler handler = new Handler();

    public void LoadImageUrl(final String urlString, final ImageView imageView) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conexao = null;
                InputStream input = null;
                try {
                    URL url = new URL(urlString);
                    conexao = (HttpURLConnection) url.openConnection();
                    conexao.setDoInput(true);
                    
                    // Finja ser um navegador para não tomar 403
                    conexao.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:13.0) Gecko/13.0 Firefox/13.0");
                    conexao.connect();

                    int responseCode = conexao.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        input = conexao.getInputStream();
                        final Bitmap bitmap = BitmapFactory.decodeStream(input);

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (bitmap != null && imageView != null) {
                                    imageView.setImageBitmap(bitmap);
                                    imageView.requestLayout(); 
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (input != null) input.close();
                        if (conexao != null) conexao.disconnect();
                    } catch (Exception ignored) {}
                }
            }
        }).start();
    }

    // 💡 PASSE O CONTEXTO PARA CONSEGUIR LER O CONFIG.CFG NO ANDROID
    public void viewProfilePicture(Context context, String username, ImageView targetImageView) {
        // Roda a requisição de rede em uma Thread separada (obrigatório no Android)
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
                    request req = new request(); // Assumindo que sua classe de request se chama "request"
                    String responseString = req.requestHTTP(url + "/view-profile-picture", "POST", jsonProfile);
                    if (responseString != null && !responseString.trim().isEmpty()) {
                        JSONObject responseJson = new JSONObject(responseString);
                        String avatarUrl = responseJson.optString("profile-picture", "");

                        if (!avatarUrl.isEmpty()) {
                            LoadImageUrl(avatarUrl, targetImageView);
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