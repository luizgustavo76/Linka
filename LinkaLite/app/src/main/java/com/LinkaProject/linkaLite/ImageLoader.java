package com.LinkaProject.linkaLite;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.widget.ImageView;
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
                    // Contorna o problema de TLS do Supabase trocando a URL se necessário,
                    // ou apenas tratando os headers de requisição padrão.
                    URL url = new URL(urlString);
                    conexao = (HttpURLConnection) url.openConnection();
                    conexao.setDoInput(true);
                    
                    // ESSENCIAL PARA O SUPABASE: Finja ser um navegador para não tomar 403
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
                                    // Força o Android antigo a refazer o cálculo de tamanho na tela
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
}