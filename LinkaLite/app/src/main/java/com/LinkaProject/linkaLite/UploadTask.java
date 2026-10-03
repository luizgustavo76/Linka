package com.LinkaProject.linkaLite;

import android.content.Context;
import android.net.Uri;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadTask {

    public static String uploadProfilePicture(Context context, Uri imageUri, String requestUrl) {
        String boundary = "----LinkaUploadBound" + System.currentTimeMillis();
        String LINE_FEED = "\r\n";

        try {
            URL url = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            OutputStream outputStream = conn.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream, true);

            String fileName = "upload_image.png";

            writer.append("--").append(boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"").append(fileName).append("\"").append(LINE_FEED);
            writer.append("Content-Type: image/png").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();

            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            if (inputStream != null) {
                inputStream.close();
            }

            writer.append(LINE_FEED);
            writer.append("--").append(boundary).append("--").append(LINE_FEED);
            writer.flush();
            writer.close();

            int status = conn.getResponseCode();
            InputStream responseStream = (status == HttpURLConnection.HTTP_OK) ? conn.getInputStream() : conn.getErrorStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            conn.disconnect();

            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}