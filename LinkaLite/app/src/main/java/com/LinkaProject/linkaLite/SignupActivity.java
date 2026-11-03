package com.LinkaProject.linkaLite;

import com.LinkaProject.linkaLite.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import org.json.JSONException;

public class SignupActivity extends Activity {
    private static final String TAG = "LinkaSignup";

    private EditText edtUsername;
    private EditText edtPassword;
    private EditText edtRetypePassword;
    private EditText edtEmail;
    private TextView txtGoToSignin;
    private Button btnRegister;
    private String serverUrl = "https://linka-ztg3.onrender.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Carrega a URL do servidor configurada no config.cfg
        try {
            config cfg = new config();
            if (config.configFileExists(SignupActivity.this, "config.cfg")) {
                String rawCfg = cfg.loadCfgAsJson(SignupActivity.this, "config.cfg");
                JSONObject jsonCfg = new JSONObject(rawCfg);
                JSONObject server = jsonCfg.optJSONObject("SERVER");
                if (server != null) {
                    String cfgUrl = server.optString("url", "").trim();
                    if (cfgUrl.length() > 0) {
                        serverUrl = cfgUrl;
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao carregar URL do servidor no SignupActivity", e);
        }

        setContentView(R.layout.activity_register);

        edtUsername = (EditText) findViewById(R.id.edtUsername);
        edtPassword = (EditText) findViewById(R.id.edtPassword); 
        edtRetypePassword = (EditText) findViewById(R.id.edtRetypePassword);
        edtEmail = (EditText) findViewById(R.id.edtEmail);
        txtGoToSignin = (TextView) findViewById(R.id.txtGoToSignin);
        btnRegister = (Button) findViewById(R.id.btnRegister);

        txtGoToSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String username = edtUsername.getText().toString().trim();
                final String password = edtPassword.getText().toString().trim();
                final String email = edtEmail.getText().toString().trim();
                String passwordRetyped = edtRetypePassword.getText().toString().trim();

                if (username.length() == 0 || password.length() == 0 || email.length() == 0) {
                    Toast.makeText(SignupActivity.this, "Fill all fields!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(passwordRetyped)) {
                    Toast.makeText(SignupActivity.this, "Passwords don't match!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Desabilita o botão temporariamente para evitar cliques duplos
                btnRegister.setEnabled(false);

                // Executa a requisição HTTP em background para não travar a UI
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonRegister = new JSONObject();
                            jsonRegister.put("username", username);
                            jsonRegister.put("senha", password);    // Envia 'senha' para casar com o backend Flask
                            jsonRegister.put("password", password); // Envia 'password' por compatibilidade
                            jsonRegister.put("email", email);

                            String baseUrl = serverUrl.trim();
                            while (baseUrl.endsWith("/")) {
                                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                            }
                            String targetUrl = baseUrl + "/register";

                            Log.d(TAG, "Enviando requisição de cadastro via request.java para: " + targetUrl);

                            // Utiliza o motor centralizado com suporte a Conscrypt, Follow Redirects e SSL Fallback
                            final String response = request.requestHTTP(targetUrl, "POST", jsonRegister, 0, SignupActivity.this);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnRegister.setEnabled(true);

                                    if (response != null && response.length() > 0) {
                                        Log.d(TAG, "Resposta do registro: " + response);
                                        
                                        config cfg = new config();
                                        cfg.updateCfg(SignupActivity.this, "config.cfg", "FAST_LOGIN", "username", username);
                                        cfg.updateCfg(SignupActivity.this, "config.cfg", "FAST_LOGIN", "password", password);
                                        
                                        String newToken = tokenManager.newSession(SignupActivity.this);
                                        cfg.updateCfg(SignupActivity.this, "config.cfg", "FAST_LOGIN", "token_session", newToken);

                                        Toast.makeText(SignupActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(SignupActivity.this, HomeActivity.class);
                                        startActivity(intent);
                                        finish(); 
                                    } else {
                                        Log.e(TAG, "Falha no registro. Resposta vazia do servidor.");
                                        Toast.makeText(SignupActivity.this, "Connection with server has failed", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                        } catch (Exception e) {
                            Log.e(TAG, "Exceção durante o cadastro: ", e);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnRegister.setEnabled(true);
                                    Toast.makeText(SignupActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        });
    }
}