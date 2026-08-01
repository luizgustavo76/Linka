package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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
    private Button btnLogin;
    private TextView txtGoToSignup;
    private String serverUrl = "http://bfdad03a0c9a3294-179-222-238-217.serveousercontent.com";
    
    private LoginTask currentLoginTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        System.setProperty("http.keepAlive", "false");
        config cfg = new config();
        if (!config.configFileExists(this, "config.cfg")) {
            cfg.createDefaultConfig(this, "config.cfg");
        }
        
        try {
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(LoginActivity.this, "config.cfg"));
            
            JSONObject server = jsonCfg.optJSONObject("SERVER");
            if (server != null) {
                serverUrl = server.optString("url", serverUrl);
            }

            JSONObject fastLogin = jsonCfg.optJSONObject("FAST_LOGIN");
            if (fastLogin != null) {
                String username = fastLogin.optString("username", "");
                String password = fastLogin.optString("password", "");

                if (!username.isEmpty() && !password.isEmpty()) {
                    currentLoginTask = new LoginTask();
                    currentLoginTask.execute(username, password);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        edtUsername = (EditText) findViewById(R.id.edtUsername);
        edtPassword = (EditText) findViewById(R.id.edtPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        txtGoToSignup = (TextView) findViewById(R.id.txtGoToSignup);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = edtUsername.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "fill all the fields", Toast.LENGTH_SHORT).show();
                } else {
                    currentLoginTask = new LoginTask();
                    currentLoginTask.execute(username, password);
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
            currentLoginTask.dismissDialogSafely();
        }
    }

    private class LoginTask extends AsyncTask<String, Void, String> {
        private ProgressDialog progressDialog;
        private String attemptedUsername;
        private String attemptedPassword;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(LoginActivity.this, "Wait", "Login...", true, false);
        }

        @Override
        protected String doInBackground(String... params) {
            attemptedUsername = params[0];
            attemptedPassword = params[1];
            HttpURLConnection connection = null;

            try {
                URL url = new URL(serverUrl + "/login"); 
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("username", attemptedUsername);
                jsonParam.put("password", attemptedPassword);

                OutputStream os = connection.getOutputStream();
                os.write(jsonParam.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    return response.toString();
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            dismissDialogSafely();

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
        public void dismissDialogSafely() {
            if (progressDialog != null && progressDialog.isShowing()) {
                if (!LoginActivity.this.isFinishing()) {
                    try {
                        progressDialog.dismiss();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}