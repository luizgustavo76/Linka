package com.LinkaProject.linkalite;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.JavascriptInterface;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);

        // Permite acessar arquivos locais e internet
        settings.setAllowFileAccess(true);

        // Bridge Java -> JS
        webView.addJavascriptInterface(new LinkaBridge(), "Linka");

        // Carrega seu HTML local
        webView.loadUrl("file:///android_asset/index.html");
    }

    public class LinkaBridge {

        @JavascriptInterface
        public void httpGet(final String url) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String response = requestGET(url);

                        // Envia resposta pro HTML chamando função JS
                        final String safe = response
                                .replace("\\", "\\\\")
                                .replace("'", "\\'")
                                .replace("\n", "\\n")
                                .replace("\r", "");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                webView.loadUrl("javascript:receberResposta('" + safe + "')");
                            }
                        });

                    } catch (Exception e) {
                        final String err = ("ERRO: " + e.toString())
                                .replace("\\", "\\\\")
                                .replace("'", "\\'")
                                .replace("\n", "\\n")
                                .replace("\r", "");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                webView.loadUrl("javascript:receberErro('" + err + "')");
                            }
                        });
                    }
                }
            }).start();
        }


        @JavascriptInterface
        public void httpPost(final String url, final String jsonBody) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String response = requestPOST(url, jsonBody);

                        final String safe = response
                                .replace("\\", "\\\\")
                                .replace("'", "\\'")
                                .replace("\n", "\\n")
                                .replace("\r", "");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                webView.loadUrl("javascript:receberResposta('" + safe + "')");
                            }
                        });

                    } catch (Exception e) {
                        final String err = ("ERRO: " + e.toString())
                                .replace("\\", "\\\\")
                                .replace("'", "\\'")
                                .replace("\n", "\\n")
                                .replace("\r", "");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                webView.loadUrl("javascript:receberErro('" + err + "')");
                            }
                        });
                    }
                }
            }).start();
        }
    }


    public String requestGET(String urlStr) throws Exception {

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8")
        );

        String line;
        StringBuilder sb = new StringBuilder();

        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }

        br.close();
        conn.disconnect();

        return sb.toString();
    }


    public String requestPOST(String urlStr, String jsonBody) throws Exception {

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        OutputStream os = conn.getOutputStream();
        os.write(jsonBody.getBytes("UTF-8"));
        os.flush();
        os.close();

        BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8")
        );

        String line;
        StringBuilder sb = new StringBuilder();

        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }

        br.close();
        conn.disconnect();

        return sb.toString();
    }
}