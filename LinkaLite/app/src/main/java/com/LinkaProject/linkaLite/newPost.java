package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
                String postContent = textPost.getText().toString();
                
                if (postContent.trim().isEmpty()) {
                    Toast.makeText(newPost.this, "write something!", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnSend.setEnabled(false);
                new SendPostTask().execute(postContent);
            }
        });
    }

    private class SendPostTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String postTextValue = params[0];

            try {
                config cfg = new config();
                String rawCfg = cfg.loadCfgAsJson(newPost.this, "config.cfg");
                JSONObject jsonCfg = new JSONObject(rawCfg);
                
                JSONObject fastLogin = jsonCfg.optJSONObject("FAST_LOGIN");
                JSONObject server = jsonCfg.optJSONObject("SERVER");
                
                String url = (server != null) ? server.optString("url", "http://linkaProject.pythonanywhere.com") : "http://linkaProject.pythonanywhere.com";
                String username = (fastLogin != null) ? fastLogin.optString("username", "") : "";
                String token = (fastLogin != null) ? fastLogin.optString("token_session", "") : "";
                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("username", username);
                jsonResponse.put("token_session", token);
                jsonResponse.put("text_post", postTextValue);
                jsonResponse.put("datetime", TimeUtils.getDateTime());
                if (url.endsWith("/")) {
                    url = url.substring(0, url.length() - 1);
                }

                return request.requestHTTP(url + "/new", "post", jsonResponse, newPost.this);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            btnSend.setEnabled(true);
            if (result != null && !result.isEmpty()) {
                Toast.makeText(newPost.this, "post was send!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(newPost.this, "Error in post sending.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}