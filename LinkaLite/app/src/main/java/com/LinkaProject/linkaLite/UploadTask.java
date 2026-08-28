package com.LinkaProject.linkaLite;

import android.content.Context;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.json.JSONObject;
public class UploadTask {

    public static String uploadProfilePicture(Context context, Uri imageUri, String targetUrl) {
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****" + System.currentTimeMillis() + "*****";
        
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] rawImageBytes = byteBuffer.toByteArray();
            inputStream.close();

            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            try {
                    config cfg = new config();
                    String rawCfg = cfg.loadCfgAsJson(context, "config.cfg");
                    if (rawCfg != null && !rawCfg.isEmpty()) {
                        JSONObject jsonCfg = new JSONObject(rawCfg);
                        if (jsonCfg.has("FAST_LOGIN")) {
                            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
                            String token = fastLogin.optString("token_session", "");
                            if (!token.isEmpty()) {
                                conn.setRequestProperty("Authorization", "Bearer " + token);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"profile.jpg\"" + lineEnd);
            dos.writeBytes("Content-Type: image/jpeg" + lineEnd);
            dos.writeBytes(lineEnd);

            dos.write(rawImageBytes);
            dos.writeBytes(lineEnd);

            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            dos.flush();
            dos.close();

            int serverResponseCode = conn.getResponseCode();
            if (serverResponseCode == 200) {
                InputStream is = conn.getInputStream();
                return convertStreamToString(is); 
            } else {
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}