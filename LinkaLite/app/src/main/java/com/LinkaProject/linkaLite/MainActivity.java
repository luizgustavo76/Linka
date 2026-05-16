package com.LinkaProject.linkalite;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.JavascriptInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    public static MainActivity instance;
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        instance = this;

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);

        webView.addJavascriptInterface(new LinkaBridge(), "Linka");

        webView.loadUrl("file:///android_asset/index.html");
    }


    public String requestGET(String urlStr) throws Exception {

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        int status = conn.getResponseCode();

        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        } catch (Exception e) {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
        }

        String line;
        StringBuilder sb = new StringBuilder();

        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }

        br.close();
        conn.disconnect();

        return "{\"status\":" + status + ",\"body\":\""
                + sb.toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                + "\"}";
    }

    public String requestPOST(String urlStr, String jsonBody) throws Exception {

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        conn.getOutputStream().write(jsonBody.getBytes("UTF-8"));

        int status = conn.getResponseCode();

        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        } catch (Exception e) {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
        }

        String line;
        StringBuilder sb = new StringBuilder();

        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }

        br.close();
        conn.disconnect();

        return "{\"status\":" + status + ",\"body\":\""
                + sb.toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                + "\"}";
    }


    public class LinkaBridge {

        @JavascriptInterface
        public String fileExists(String filename) {
            File file = getFileStreamPath(filename);
            return String.valueOf(file.exists());
        }

        @JavascriptInterface
        public void createEmptyFile(String filename) {
            try {
                FileOutputStream fos = openFileOutput(filename, MODE_PRIVATE);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public void saveCfg(String filename, String content) {
            try {
                OutputStreamWriter writer =
                        new OutputStreamWriter(openFileOutput(filename, MODE_PRIVATE));
                writer.write(content);
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public String loadCfgAsJson(String filename) {
            try {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(openFileInput(filename), "UTF-8")
                );

                String line;
                String section = "";
                StringBuilder json = new StringBuilder();
                json.append("{");

                boolean firstSection = true;

                while ((line = br.readLine()) != null) {
                    line = line.trim();

                    if (line.equals("") || line.startsWith("#") || line.startsWith(";")) {
                        continue;
                    }

                    if (line.startsWith("[") && line.endsWith("]")) {
                        section = line.substring(1, line.length() - 1);

                        if (!firstSection) {
                            json.append("},");
                        }

                        json.append("\"").append(section).append("\":{");
                        firstSection = false;
                        continue;
                    }

                    int eq = line.indexOf("=");
                    if (eq > 0 && section.length() > 0) {
                        String key = line.substring(0, eq).trim();
                        String value = line.substring(eq + 1).trim();

                        value = value.replace("\\", "\\\\").replace("\"", "\\\"");

                        json.append("\"").append(key).append("\":\"").append(value).append("\",");
                    }
                }

                br.close();

                if (json.charAt(json.length() - 1) == ',') {
                    json.deleteCharAt(json.length() - 1);
                }

                if (!firstSection) {
                    json.append("}");
                }

                json.append("}");

                return json.toString();

            } catch (Exception e) {
                return "{}";
            }
        }

        @JavascriptInterface
        public void httpGet(final String url) {
            new Thread(() -> {
                try {
                    String response = MainActivity.instance.requestGET(url);

                    final String safe = response
                            .replace("\\", "\\\\")
                            .replace("'", "\\'")
                            .replace("\n", "\\n")
                            .replace("\r", "");

                    MainActivity.instance.runOnUiThread(() ->
                            MainActivity.instance.webView.loadUrl(
                                    "javascript:receberResposta('" + safe + "')"
                            )
                    );

                } catch (Exception e) {
                    final String err = ("ERRO: " + e.toString())
                            .replace("\\", "\\\\")
                            .replace("'", "\\'")
                            .replace("\n", "\\n")
                            .replace("\r", "");

                    MainActivity.instance.runOnUiThread(() ->
                            MainActivity.instance.webView.loadUrl(
                                    "javascript:receberErro('" + err + "')"
                            )
                    );
                }
            }).start();
        }

        @JavascriptInterface
        public void httpPost(final String url, final String jsonBody) {
            new Thread(() -> {
                try {
                    String response = MainActivity.instance.requestPOST(url, jsonBody);

                    final String safe = response
                            .replace("\\", "\\\\")
                            .replace("'", "\\'")
                            .replace("\n", "\\n")
                            .replace("\r", "");

                    MainActivity.instance.runOnUiThread(() ->
                            MainActivity.instance.webView.loadUrl(
                                    "javascript:receberResposta('" + safe + "')"
                            )
                    );

                } catch (Exception e) {
                    final String err = ("ERRO: " + e.toString())
                            .replace("\\", "\\\\")
                            .replace("'", "\\'")
                            .replace("\n", "\\n")
                            .replace("\r", "");

                    MainActivity.instance.runOnUiThread(() ->
                            MainActivity.instance.webView.loadUrl(
                                    "javascript:receberErro('" + err + "')"
                            )
                    );
                }
            }).start();
        }
    }
}