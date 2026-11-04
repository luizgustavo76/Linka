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

public class LoginActivity extends Activity {
    private static final String TAG = "LinkaLogin";
    
    private EditText edtUsername;
    private EditText edtPassword;
    private Button btnServer;
    private Button btnLogin;
    private TextView txtGoToSignup;
    private TextView txtGuest;
    
    private String serverUrl = "http://patient-hall-2915.luizsgustavo76.workers.dev/"; 
    private LoginTask currentLoginTask;

    private void executeLogin(String username, String password) {
        if (currentLoginTask != null && currentLoginTask.getStatus() == AsyncTask.Status.RUNNING) {
            Log.d(TAG, "Cancelando LoginTask anterior em execução.");
            currentLoginTask.cancel(true);
        }
        currentLoginTask = new LoginTask();
        currentLoginTask.execute(username, password);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnServer = (Button) findViewById(R.id.btnServer);
        edtUsername = (EditText) findViewById(R.id.edtUsername);
        edtPassword = (EditText) findViewById(R.id.edtPassword);
        txtGuest = (TextView) findViewById(R.id.txtGuest);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        txtGoToSignup = (TextView) findViewById(R.id.txtGoToSignup);

        txtGuest.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                intent.putExtra("isGuest", true);
                startActivity(intent);
            }
        });

        // Carrega configurações iniciais em thread de background
        new Thread(new Runnable() {
            @Override
            public void run() {
                config cfg = new config();
                if (!config.configFileExists(LoginActivity.this, "config.cfg")) {
                    Log.d(TAG, "[CONFIG] config.cfg não existe. Criando arquivo padrão...");
                    cfg.createDefaultConfig(LoginActivity.this, "config.cfg");
                }

                String fastUsername = "";
                String fastPassword = "";

                try {
                    String jsonString = cfg.loadCfgAsJson(LoginActivity.this, "config.cfg");
                    Log.d(TAG, "[CONFIG] Conteúdo atual do config.cfg: " + jsonString);
                    
                    JSONObject jsonCfg = new JSONObject(jsonString);
                    JSONObject server = jsonCfg.optJSONObject("SERVER");
                    if (server != null) {
                        String cfgUrl = server.optString("url", "").trim();
                        if (cfgUrl.length() > 0) {
                            serverUrl = cfgUrl;
                        }
                        Log.d(TAG, "[CONFIG] URL ativa do servidor: " + serverUrl);
                    }
                    JSONObject fastLogin = jsonCfg.optJSONObject("FAST_LOGIN");
                    if (fastLogin != null) {
                        fastUsername = fastLogin.optString("username", "");
                        fastPassword = fastLogin.optString("password", "");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "[CONFIG ERRO] Erro ao processar JSON da configuração:", e);
                }

                final String finalUser = fastUsername;
                final String finalPass = fastPassword;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing() && finalUser.length() > 0 && finalPass.length() > 0) {
                            Log.d(TAG, "[FAST LOGIN] Executando auto-login para usuário: " + finalUser);
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
                if (username.length() == 0 || password.length() == 0) {
                    Toast.makeText(LoginActivity.this, "Fill all fields", Toast.LENGTH_SHORT).show();
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
                progressDialog.setMessage("Connecting to server...");
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            if (isCancelled()) return null;

            attemptedUsername = params[0];
            attemptedPassword = params[1];

            try {
                String baseUrl = serverUrl.trim();
                while (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                
                String targetUrl = baseUrl + "/login";
                Log.d(TAG, "[HTTP] Enviando POST para: " + targetUrl);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("username", attemptedUsername);
                jsonParam.put("password", attemptedPassword);

                return request.requestHTTP(targetUrl, "POST", jsonParam, 0, LoginActivity.this);
            } catch (Exception e) {
                Log.e(TAG, "[HTTP EXCEÇÃO] Erro ao preparar requisição de login: ", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            dismissDialogSafely();
            if (isCancelled() || LoginActivity.this.isFinishing()) return;

            Log.d(TAG, "[POST EXECUTE] Resultado recebido: " + result);

            if (result != null && result.length() > 0) {
                try {
                    JSONObject responseJson = new JSONObject(result);
                    String status = responseJson.optString("status", "");

                    if (status.equalsIgnoreCase("login is sucessful") || 
                        status.equalsIgnoreCase("login is successful") || 
                        status.equalsIgnoreCase("success")) {
                        
                        // 1. Solicita o token DIRETO com credenciais em memória (Evita reler config.cfg desatualizado)
                        Log.d(TAG, "[TOKEN] Solicitando token de sessão...");
                        String newToken = tokenManager.newSession(LoginActivity.this, attemptedUsername, attemptedPassword, serverUrl);

                        // 2. Salva as credenciais e o token obtido no config.cfg em background/persistência
                        config cfg = new config();
                        cfg.updateCfg(LoginActivity.this, "config.cfg", "FAST_LOGIN", "username", attemptedUsername);
                        cfg.updateCfg(LoginActivity.this, "config.cfg", "FAST_LOGIN", "password", attemptedPassword);
                        if (newToken.length() > 0) {
                            cfg.updateCfg(LoginActivity.this, "config.cfg", "FAST_LOGIN", "token_session", newToken);
                        }
                
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.w(TAG, "[LOGIN RECUSADO] Status incorreto: " + status);
                        Toast.makeText(LoginActivity.this, "Username or password incorrect!", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "[JSON ERRO] Exceção ao ler JSON retornado: ", e);
                    Toast.makeText(LoginActivity.this, "Error processing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "[CONEXAO FALHOU] Resposta vazia ou nula retornada pela rede.");
                Toast.makeText(LoginActivity.this, "Connection with server failed", Toast.LENGTH_LONG).show();
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
                Log.e(TAG, "Erro ao ocultar dialog", e);
            }
        }
    }
}