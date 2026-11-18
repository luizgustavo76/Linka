package com.LinkaProject.linkaLite;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadTask {

    private static final String TAG = "Linka_UploadTask";

    public static String uploadProfilePicture(Context context, Uri imageUri, String requestUrl) {
        return uploadProfilePicture(context, imageUri, requestUrl, null);
    }

    public static String uploadProfilePicture(Context context, Uri imageUri, String requestUrl, String username) {
        if (context == null || imageUri == null || requestUrl == null || requestUrl.trim().length() == 0) {
            return makeJsonError("UploadTask", "Parâmetros de entrada inválidos.");
        }

        String boundary = "*****" + System.currentTimeMillis() + "*****";
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        InputStream inputStream = null;

        try {
            Log.d(TAG, "Iniciando upload para: " + requestUrl);

            URL url = new URL(requestUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            OutputStream outStream = conn.getOutputStream();
            if (outStream == null) {
                return makeJsonError("UploadTask", "Não foi possível obter OutputStream HTTP.");
            }
            dos = new DataOutputStream(outStream);

            // 1. Campo 'username' no Multipart Form-Data
            if (username != null && username.length() > 0) {
                String userPart = twoHyphens + boundary + lineEnd +
                        "Content-Disposition: form-data; name=\"username\"" + lineEnd +
                        "Content-Type: text/plain; charset=UTF-8" + lineEnd + lineEnd +
                        username + lineEnd;
                dos.write(userPart.getBytes("UTF-8"));
            }

            // 2. Cabeçalhos da Imagem
            String fileHeader = twoHyphens + boundary + lineEnd +
                    "Content-Disposition: form-data; name=\"image\"; filename=\"profile.jpg\"" + lineEnd +
                    "Content-Type: image/jpeg" + lineEnd + lineEnd;
            dos.write(fileHeader.getBytes("UTF-8"));

            // 3. Leitura e escrita dos bytes da imagem
            inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                return makeJsonError("UploadTask", "Falha ao abrir InputStream da imagem.");
            }

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }

            // 4. Fechamento das boundaries do Multipart
            dos.write(lineEnd.getBytes("UTF-8"));
            String endBoundary = twoHyphens + boundary + twoHyphens + lineEnd;
            dos.write(endBoundary.getBytes("UTF-8"));
            dos.flush();

            int status = conn.getResponseCode();
            Log.d(TAG, "HTTP Response Code: " + status);

            InputStream responseStream = (status >= 200 && status < 300) 
                    ? conn.getInputStream() 
                    : conn.getErrorStream();

            if (responseStream == null) {
                return makeJsonError("UploadTask", "Servidor retornou HTTP " + status + " sem corpo de resposta.");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream, "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();

        } catch (Exception e) {
            Log.e(TAG, "Erro no upload da imagem", e);
            return makeJsonError("UploadTask", e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (inputStream != null) { try { inputStream.close(); } catch (Exception ignored) {} }
            if (dos != null) { try { dos.close(); } catch (Exception ignored) {} }
            if (conn != null) { conn.disconnect(); }
        }
    }

    private static String makeJsonError(String source, String message) {
        try {
            JSONObject err = new JSONObject();
            err.put("error", source + " -> " + message);
            return err.toString();
        } catch (Exception e) {
            return "{\"error\":\"" + source + " -> " + message + "\"}";
        }
    }
}