package com.LinkaProject.linkaLite;
import com.LinkaProject.linkaLite.R;
import android.app.Activity;
import android.content.Intent;
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
public class SignupActivity extends Activity {
    private EditText edtUsername;
    private EditText edtPassword;
    private EditText edtRetypePassword;
    private EditText edtEmail;
    private TextView txtGoToSignin;
    private Button btnRegister;
    public String requestHTTP(String urlParam, String method, JSONObject json_body) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlParam);
            connection = (HttpURLConnection) url.openConnection();
            
            method = method.toLowerCase();
            if (method.equals("post")) {
                connection.setRequestMethod("POST");
            } else if (method.equals("get")) {
                connection.setRequestMethod("GET");
            }
            
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            OutputStream os = connection.getOutputStream();
            os.write(json_body.toString().getBytes("UTF-8"));
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
                return response.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return ""; 
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final String baseUrl = "http://linkaProject.pythonanywhere.com";
        super.onCreate(savedInstanceState);
        config cfg = new config();
        cfg.deleteFileLinka(SignupActivity.this, "config.cfg");
        cfg.createDefaultConfig(SignupActivity.this, "config.cfg");
        setContentView(R.layout.activity_register);
        edtUsername = (EditText) findViewById(R.id.edtUsername);
        edtPassword = (EditText) findViewById(R.id.edtPassword); 
        edtRetypePassword = (EditText) findViewById(R.id.edtRetypePassword);
        edtEmail = (EditText) findViewById(R.id.edtEmail);
        txtGoToSignin = (TextView) findViewById(R.id.txtGoToSignin);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = edtUsername.getText().toString();
                String password = edtPassword.getText().toString();
                String email = edtEmail.getText().toString();
                String passwordRetyped = edtRetypePassword.getText().toString();
                try {
                    JSONObject json_register = new JSONObject();
                    json_register.put("username", username);
                    json_register.put("password", password);
                    json_register.put("email", email);
                    if (password.equals(passwordRetyped)) {
                        String response = requestHTTP(baseUrl + "/register", "post", json_register);
                        if (response.length() != 0) {
                            config cfg = new config();
                            cfg.updateCfg(SignupActivity.this, "config.cfg", "FAST_LOGIN", "username", username);
                            cfg.updateCfg(SignupActivity.this, "config.cfg", "FAST_LOGIN", "password", password);
                            String newToken = tokenManager.newSession(SignupActivity.this);
                            cfg.updateCfg(SignupActivity.this, "config.cfg", "FAST_LOGIN", "token_session", newToken);
                            Intent intent = new Intent(SignupActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish(); 
                        } else {
                            Toast.makeText(SignupActivity.this, "Connection with server has failed", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(SignupActivity.this, "Passwords don't match!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}