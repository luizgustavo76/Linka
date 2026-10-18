package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class ExternalLogin extends Activity {
    private static final String TAG = "[DEBUG_BRUNO]";
    private ImageView networkLogo;
    private EditText edtUsername;
    private EditText edtToken;
    private Button btnSend;
    private String platform = "MASTODON"; // Fallback padrão
    private static final String CFG_EXTERNAL_FILE = "config-external.cfg";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.external_login);
        networkLogo = (ImageView) findViewById(R.id.networkLogo);
        edtUsername = (EditText) findViewById(R.id.edtUsername);
        edtToken = (EditText) findViewById(R.id.edtToken);
        btnSend = (Button) findViewById(R.id.btnSend);
        Intent intent = getIntent();
        if (intent != null) {
            String intentPlatform = intent.getStringExtra("platform");
            if (intentPlatform == null) {
                intentPlatform = intent.getStringExtra("network");
            }
            if (intentPlatform != null && intentPlatform.trim().length() > 0) {
                platform = intentPlatform.trim().toUpperCase();
            }
        }
        if ("REDDIT".equals(platform)) {
            if (networkLogo != null) networkLogo.setImageResource(R.drawable.reddit);
        } else if ("BLUESKY".equals(platform)) {
            if (networkLogo != null) networkLogo.setImageResource(R.drawable.bluesky);
        } else {
            // MASTODON por padrão
            if (networkLogo != null) networkLogo.setImageResource(R.drawable.mastodon);
        }

        // 3. Preenche os campos caso já existam dados salvos no config-external.cfg
        loadExistingCredentials();

        // 4. Clique do botão Salvar
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCredentials();
            }
        });
    }

    private void loadExistingCredentials() {
        config cfg = new config();
        String jsonRaw = cfg.loadCfgAsJson(ExternalLogin.this, CFG_EXTERNAL_FILE);
        
        if (jsonRaw != null && jsonRaw.length() > 0) {
            try {
                JSONObject jsonCfg = new JSONObject(jsonRaw);
                JSONObject section = jsonCfg.optJSONObject(platform);
                if (section != null) {
                    String savedUser = section.optString("username", "");
                    String savedToken = section.optString("token", "");
                    
                    if (edtUsername != null) edtUsername.setText(savedUser);
                    if (edtToken != null) edtToken.setText(savedToken);

                    Log.d(TAG, "Credenciais carregadas para " + platform + ": username=" + savedUser);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Erro ao realizar parse de " + CFG_EXTERNAL_FILE + ": " + e.getMessage());
            }
        }
    }

    private void saveCredentials() {
        String username = edtUsername.getText().toString().trim();
        String token = edtToken.getText().toString().trim();

        if (username.length() == 0) {
            Toast.makeText(ExternalLogin.this, "Por favor, digite o usuário!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (token.length() == 0) {
            Toast.makeText(ExternalLogin.this, "Por favor, insira o token de acesso!", Toast.LENGTH_SHORT).show();
            return;
        }

        config cfg = new config();
        
        if (!config.configFileExists(ExternalLogin.this, CFG_EXTERNAL_FILE)) {
            cfg.createDefaultConfigExternal(ExternalLogin.this, CFG_EXTERNAL_FILE);
        }
        String resUser = cfg.updateCfg(ExternalLogin.this, CFG_EXTERNAL_FILE, platform, "username", username);
        String resToken = cfg.updateCfg(ExternalLogin.this, CFG_EXTERNAL_FILE, platform, "token", token);

        Log.d(TAG, "Salvando " + platform + " -> Resposta User: " + resUser + " | Resposta Token: " + resToken);

        if ("successful!".equals(resUser) || "successful!".equals(resToken)) {
            Toast.makeText(ExternalLogin.this, "Credenciais do " + platform + " salvas com sucesso!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(ExternalLogin.this, "Erro ao salvar credenciais!", Toast.LENGTH_SHORT).show();
        }
    }
}