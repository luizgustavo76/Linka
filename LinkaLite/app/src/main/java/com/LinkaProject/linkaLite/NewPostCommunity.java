package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class NewPostCommunity extends Activity {
    private Button btnSend;
    private String url = "";
    private String username = "";
    private EditText textPost;
    private String community_id = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_post_community);

        btnSend = (Button) findViewById(R.id.btnSend);
        textPost = (EditText) findViewById(R.id.textPost);

        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(NewPostCommunity.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            url = server.optString("url", "");
            username = fastLogin.optString("username", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("community_id")) {
            community_id = intent.getStringExtra("community_id");
        }

        btnSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String text_post = textPost.getText().toString().trim();

                if (text_post.length() == 0) {
                    Toast.makeText(NewPostCommunity.this, "Write something!", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnSend.setEnabled(false); 

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final JSONObject jsonNew = new JSONObject();
                            jsonNew.put("username", username);
                            jsonNew.put("text_post", text_post);
                            jsonNew.put("community_id", community_id);

                            request.requestHTTP(url + "/new-post-community", "post", jsonNew, NewPostCommunity.this);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                }
                            });
                        } catch (final Exception e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnSend.setEnabled(true);
                                    Toast.makeText(NewPostCommunity.this, "Error in posting", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        });
    }
}