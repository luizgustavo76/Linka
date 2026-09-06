package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends Activity {
    private EditText edtUsername;
    private EditText edtPassword;
    private Button btnServer;
    private Button btnLogin;
    private TextView txtGoToSignup;
    private String serverUrl = "http://linkaProject.pythonanywhere.com";
    private LoginTask currentLoginTask;

    private void executeLogin(String username, String password) {
        if (currentLoginTask != null && currentLoginTask.getStatus() == AsyncTask.Status.RUNNING) {
            currentLoginTask.cancel(true);
        }
        currentLoginTask = new LoginTask();
        currentLoginTask.execute(username, password);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        System.setProperty("http.keepAlive", "false");

        btnServer = (Button) findViewById(R.id.btnServer);
        edtUsername = (EditText) findViewById(R.id.edtUsername);
        edtPassword = (EditText) findViewById(R.id.edtPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        txtGoToSignup = (TextView) findViewById(R.id.txtGoToSignup);

        // Executa a leitura de arquivos em background para não travar a UI Thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                config cfg = new config();
                if (!config.configFileExists(LoginActivity.this, "config.cfg")) {
                    cfg.createDefaultConfig(LoginActivity.this, "config.cfg");
                }

                String fastUsername = "";
                String fastPassword = "";

                try {
                    JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(LoginActivity.this, "config.cfg"));
                    JSONObject server = jsonCfg.optJSONObject("SERVER");
                    if (server != null) {
                        serverUrl = server.optString("url", serverUrl);
                    }
                    JSONObject fastLogin = jsonCfg.optJSONObject("FAST_LOGIN");
                    if (fastLogin != null) {
                        fastUsername = fastLogin.optString("username", "");
                        fastPassword = fastLogin.optString("password", "");
                    }
                } catch (JSONException e) {
                    Log.e("LinkaLogin", "Error loading JSON config", e);
                }

                final String finalUser = fastUsername;
                final String finalPass = fastPassword;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing() && !finalUser.isEmpty() && !finalPass.isEmpty()) {
                            executeLogin(finalUser, finalPass);
                        }
                    }
                });
            }
        }).start();

        btnServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ChangeServer.class);
                startActivity(intent);
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = edtUsername.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "fill all the fields", Toast.LENGTH_SHORT).show();
                } else {
                    executeLogin(username, password);
                }
            }
        });

        txtGoToSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentLoginTask != null) {
            currentLoginTask.cancel(true);
            currentLoginTask.dismissDialogSafely();
        }
    }

    private class LoginTask extends AsyncTask<String, Void, String> {
        private ProgressDialog progressDialog;
        private String attemptedUsername;
        private String attemptedPassword;

        @Override
        protected void onPreExecute() {
            if (!LoginActivity.this.isFinishing()) {
                progressDialog = new ProgressDialog(LoginActivity.this);
                progressDialog.setTitle("Wait");
                progressDialog.setMessage("Login...");
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            if (isCancelled()) return null;

            attemptedUsername = params[0];
            attemptedPassword = params[1];
            HttpURLConnection connection = null;

            try {
                String baseUrl = serverUrl.trim();
                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                URL url = new URL(baseUrl + "/login");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");

                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Connection", "close");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("username", attemptedUsername);
                jsonParam.put("password", attemptedPassword);

                byte[] postData = jsonParam.toString().getBytes("UTF-8");
                connection.setRequestProperty("Content-Length", String.valueOf(postData.length));

                OutputStream os = connection.getOutputStream();
                os.write(postData);
                os.flush();
                os.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (isCancelled()) break;
                        response.append(line);
                    }
                    in.close();
                    return response.toString();
                } else {
                    Log.e("LinkaLogin", "HTTP Error Code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e("LinkaLogin", "ERRO REAL DA CONEXAO: ", e);
                return null;
            } finally {
                if (connection != null) connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            dismissDialogSafely();
            if (isCancelled() || LoginActivity.this.isFinishing()) return;

            if (result != null) {
                try {
                    JSONObject responseJson = new JSONObject(result);
                    String status = responseJson.optString("status", "");
                    if (status.equals("login is sucessful")) {
                        
                        config cfg = new config();
                        cfg.updateCfg(LoginActivity.this, "config.cfg", "FAST_LOGIN", "username", attemptedUsername);
                        cfg.updateCfg(LoginActivity.this, "config.cfg", "FAST_LOGIN", "password", attemptedPassword);
                        String newToken = tokenManager.newSession(LoginActivity.this);
                        cfg.updateCfg(LoginActivity.this, "config.cfg", "FAST_LOGIN", "token_session", newToken);
                
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Username or password incorrect!", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, "Error processing data", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(LoginActivity.this, "Connection with server failed", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            dismissDialogSafely();
        }

        public void dismissDialogSafely() {
            try {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            } catch (Exception e) {
                Log.e("LinkaLogin", "Error dismissing progress dialog", e);
            }
        }
    }
}