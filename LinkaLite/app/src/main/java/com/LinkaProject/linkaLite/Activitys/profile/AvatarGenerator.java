package com.LinkaProject.linkaLite;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AvatarGenerator {

    private static final int MAX_CACHE_ENTRIES = 64;

    // Cache LRU nativo em Java puro para evitar recriar bitmaps no Android 2.3+
    private static final Map<String, Bitmap> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, Bitmap>(MAX_CACHE_ENTRIES, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Bitmap> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            }
    );

    // Paleta curada com bom contraste para texto branco (estilo Discord/Telegram)
    private static final int[] PALETTE = new int[]{
            Color.parseColor("#E53935"), // Vermelho
            Color.parseColor("#D81B60"), // Rosa escuro
            Color.parseColor("#8E24AA"), // Roxo
            Color.parseColor("#5E35B1"), // Índigo
            Color.parseColor("#3949AB"), // Azul royal
            Color.parseColor("#1E88E5"), // Azul claro
            Color.parseColor("#00897B"), // Verde-água (Teal)
            Color.parseColor("#43A047"), // Verde
            Color.parseColor("#F4511E"), // Laranja queimado
            Color.parseColor("#6D4C41")  // Marrom café
    };

    /**
     * Retorna sempre a mesma cor para um determinado username.
     */
    public static int getColorForUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return PALETTE[0];
        }
        String normalized = username.trim().toLowerCase();
        int hash = 5381;
        for (int i = 0; i < normalized.length(); i++) {
            hash = ((hash << 5) + hash) + normalized.charAt(i);
        }
        int index = Math.abs(hash) % PALETTE.length;
        return PALETTE[index];
    }

    public static Bitmap generateAvatar(String username, int sizePx) {
        String cleanName = (username != null) ? username.trim() : "?";
        if (cleanName.startsWith("@") && cleanName.length() > 1) {
            cleanName = cleanName.substring(1);
        }

        String cacheKey = (cleanName.isEmpty() ? "?" : cleanName.toLowerCase()) + "_" + sizePx;
        Bitmap cached = CACHE.get(cacheKey);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }

        String initial = cleanName.isEmpty() ? "?" : cleanName.substring(0, 1).toUpperCase();
        int bgColor = getColorForUsername(cleanName);

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 1. Fundo quadrado para alinhar com os avatares de foto do feed
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(bgColor);
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, sizePx, sizePx, bgPaint);

        // 2. Desenha a inicial centralizada com precisão usando FontMetrics
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(sizePx * 0.50f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        float center = sizePx / 2f;
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = center - (fm.ascent + fm.descent) / 2f;

        canvas.drawText(initial, center, textY, textPaint);

        CACHE.put(cacheKey, bitmap);
        return bitmap;
    }
}