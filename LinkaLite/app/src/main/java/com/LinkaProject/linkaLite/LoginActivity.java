package com.LinkaProject.linkaLite;
import com.LinkaProject.linkaLite.R;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        config cfg = new config();
        cfg.deleteFileLinka(LoginActivity.this, "config.cfg");
        cfg.createDefaultConfig(LoginActivity.this, "config.cfg");
        edtUsername = (EditText) findViewById(R.id.edtUsername);
        edtPassword = (EditText) findViewById(R.id.edtPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        txtGoToSignup = (TextView) findViewById(R.id.txtGoToSignup);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = edtUsername.getText().toString();
                String password = edtPassword.getText().toString();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "fill all the camps", Toast.LENGTH_SHORT).show();
                } else {
                    new LoginTask().execute(username, password);
                }
            }
        });

        // Navegação para a tela de Cadastro
        txtGoToSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }

    // AsyncTask para processar a rede sem travar a interface do Android 2.0
    private class LoginTask extends AsyncTask<String, Void, String> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(LoginActivity.this, "Wait", "Login...");
        }

        @Override
        protected String doInBackground(String... params) {
            String username = params[0];
            String password = params[1];
            HttpURLConnection connection = null;

            try {
                // Substitua pela sua URL real de API
                URL url = new URL("http://linkaProject.pythonanywhere.com/login"); 
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Montando o JSON igual ao nlohmann::json que você usava
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("username", username);
                jsonParam.put("password", password);
                config cfg = new config();
                cfg.updateCfg(LoginActivity.this, "config.cfg", "[FAST_LOGIN]", "username", username);
                cfg.updateCfg(LoginActivity.this, "config.cfg", "[FAST_LOGIN]", "password", username);
                String newToken = tokenManager.newSession(LoginActivity.this);
                cfg.updateCfg(LoginActivity.this, "config.cfg", "[FAST_LOGIN]", "token_session", newToken);                   
                OutputStream os = connection.getOutputStream();
                os.write(jsonParam.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    return response.toString(); // Retorna o JSON de resposta
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
            progressDialog.dismiss();
            if (result != null) {
                try {
                    // Fazendo o parse do JSON nativo do Android
                    JSONObject responseJson = new JSONObject(result);
                    String status = responseJson.getString("status");
                    if (status.equals("login is sucessful")) {
                        Toast.makeText(LoginActivity.this, "Login has sucessful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish(); 
                    } else {
                        Toast.makeText(LoginActivity.this, "Username or password is incorrect!", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, "Error in processing data", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(LoginActivity.this, "conection with server has failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}