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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends Activity {
    private static final String TAG = "LinkaLogin";
    
    private EditText edtUsername;
    private EditText edtPassword;
    private Button btnServer;
    private Button btnLogin;
    private TextView txtGoToSignup;
    // CORRIGIDO: Alterado de https:// para http://
    private String serverUrl = "http://192.168.240.1:5000"; 
    private LoginTask currentLoginTask;

    private void executeLogin(String username, String password) {
        if (currentLoginTask != null && currentLoginTask.getStatus() == AsyncTask.Status.RUNNING) {
            Log.d(TAG, "Cancelando LoginTask anterior em execução.");
            currentLoginTask.cancel(true);
        }
        Log.d(TAG, "Iniciando nova LoginTask para o usuário: " + username);
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Lendo configurações de arquivo em background...");
                config cfg = new config();
                if (!config.configFileExists(LoginActivity.this, "config.cfg")) {
                    Log.d(TAG, "Arquivo config.cfg não existe. Criando padrão...");
                    cfg.createDefaultConfig(LoginActivity.this, "config.cfg");
                }

                String fastUsername = "";
                String fastPassword = "";

                try {
                    String jsonString = cfg.loadCfgAsJson(LoginActivity.this, "config.cfg");
                    Log.d(TAG, "Conteúdo do config.cfg: " + jsonString);
                    
                    JSONObject jsonCfg = new JSONObject(jsonString);
                    JSONObject server = jsonCfg.optJSONObject("SERVER");
                    if (server != null) {
                        serverUrl = server.optString("url", serverUrl);
                        Log.d(TAG, "URL do servidor carregada da config: " + serverUrl);
                    }
                    JSONObject fastLogin = jsonCfg.optJSONObject("FAST_LOGIN");
                    if (fastLogin != null) {
                        fastUsername = fastLogin.optString("username", "");
                        fastPassword = fastLogin.optString("password", "");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Erro ao processar JSON da configuração:", e);
                }

                final String finalUser = fastUsername;
                final String finalPass = fastPassword;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing() && !finalUser.isEmpty() && !finalPass.isEmpty()) {
                            Log.d(TAG, "Executando Fast Login para: " + finalUser);
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
                
                String targetUrl = baseUrl + "/login";
                Log.d(TAG, "Iniciando conexão HTTP POST para: " + targetUrl);

                URL url = new URL(targetUrl);
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

                Log.d(TAG, "Enviando Payload JSON: " + jsonParam.toString());

                OutputStream os = connection.getOutputStream();
                os.write(postData);
                os.flush();
                os.close();

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Código de Resposta HTTP recebido: " + responseCode);

                InputStream stream = (responseCode >= 200 && responseCode < 300) 
                        ? connection.getInputStream() 
                        : connection.getErrorStream();

                if (stream != null) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (isCancelled()) break;
                        response.append(line);
                    }
                    in.close();
                    Log.d(TAG, "Resposta bruta do servidor: " + response.toString());
                    return response.toString();
                } else {
                    Log.e(TAG, "InputStream e ErrorStream vieram nulos do servidor.");
                }

            } catch (Exception e) {
                Log.e(TAG, "EXCEÇÃO DURANTE O REQUEST HTTP: ", e);
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

            Log.d(TAG, "onPostExecute chamado com resultado: " + result);

            if (result != null) {
                try {
                    JSONObject responseJson = new JSONObject(result);
                    String status = responseJson.optString("status", "");
                    Log.d(TAG, "Status extraído do JSON: " + status);

                    if (status.equalsIgnoreCase("login is sucessful") || status.equalsIgnoreCase("login is successful") || status.equalsIgnoreCase("success")) {
                        
                        config cfg = new config();
                        cfg.updateCfg(LoginActivity.this, "config.cfg", "FAST_LOGIN", "username", attemptedUsername);
                        cfg.updateCfg(LoginActivity.this, "config.cfg", "FAST_LOGIN", "password", attemptedPassword);
                        
                        // Garante o log ao tentar criar o token
                        Log.d(TAG, "Gerando nova sessão no TokenManager...");
                        String newToken = tokenManager.newSession(LoginActivity.this);
                        cfg.updateCfg(LoginActivity.this, "config.cfg", "FAST_LOGIN", "token_session", newToken);
                
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.w(TAG, "Login recusado pelo backend. Status: " + status);
                        Toast.makeText(LoginActivity.this, "Username or password incorrect!", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "EXCEÇÃO AO PROCESSAR O JSON NO ONPOSTEXECUTE: ", e);
                    Toast.makeText(LoginActivity.this, "Error processing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Resultado nulo recebido em onPostExecute. Conexão falhou completamente.");
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
                Log.e(TAG, "Error dismissing progress dialog", e);
            }
        }
    }
}