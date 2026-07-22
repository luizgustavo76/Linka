package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class newPost extends Activity {

    private TextView newPostText;
    private EditText textPost;
    private Button btnSend;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.new_post_activity);

        newPostText = (TextView) findViewById(R.id.newPostText);
        textPost = (EditText) findViewById(R.id.textPost);
        btnSend = (Button) findViewById(R.id.btnSend);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 1. Pegamos o texto AQUI na Thread de UI
                String postContent = textPost.getText().toString();
                
                if (postContent.trim().isEmpty()) {
                    Toast.makeText(newPost.this, "write something!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 2. Passamos o texto para a Task
                new SendPostTask().execute(postContent);
            }
        });
    }

    // Alterado para receber o texto via String... no doInBackground
    private class SendPostTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String postTextValue = params[0]; // Texto que veio da UI

            try {
                config cfg = new config();
                JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(newPost.this, "config.cfg"));
                
                JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
                JSONObject server = jsonCfg.getJSONObject("SERVER");
                
                String url = server.optString("url", "");
                String username = fastLogin.getString("username");
                String token = fastLogin.getString("token_session");

                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("username", username);
                jsonResponse.put("text_post", postTextValue); // Usando o parâmetro seguro
                jsonResponse.put("datetime", TimeUtils.getDateTime());

                return request.requestHTTP(url + "/new", "post", jsonResponse, newPost.this);

            } catch (Exception e) {
                // Isso vai printar exatamente onde estourou no logcat se falhar!
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Toast.makeText(newPost.this, "post was send!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(newPost.this, "Error in post sending.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}