package com.LinkaProject.linkaLite;

import com.LinkaProject.linkaLite.R;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatGlobalActivity extends Activity {
    private static final String TAG = "LINKA_DEBUG";

    private ImageButton btnHeaderImage;
    private TextView txtHeaderTitle;
    private Button btnSend;
    private LinearLayout chatContainer;
    
    private String username = "";
    private String url = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.global_chat);

        Log.d(TAG, "=== ChatGlobalActivity Started ===");

        // 1. Load Configurations
        try {
            config cfg = new config();
            String rawCfg = cfg.loadCfgAsJson(this, "config.cfg");
            Log.d(TAG, "Raw config.cfg content: " + rawCfg);

            if (rawCfg != null && !rawCfg.isEmpty()) {
                JSONObject jsonCfg = new JSONObject(rawCfg);
                JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
                JSONObject server = jsonCfg.getJSONObject("SERVER");
                
                username = fastLogin.optString("username", "");
                url = server.optString("url", "");

                Log.d(TAG, "Loaded username: " + username);
                Log.d(TAG, "Loaded URL: " + url);
            } else {
                Log.e(TAG, "config.cfg file returned NULL or EMPTY!");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing config.cfg", e);
        }

        btnHeaderImage = (ImageButton) findViewById(R.id.btnHeaderImage);
        txtHeaderTitle = (TextView) findViewById(R.id.txtHeaderTitle);
        btnSend = (Button) findViewById(R.id.btnSend);
        chatContainer = (LinearLayout) findViewById(R.id.layoutMessagesContainer);

        btnHeaderImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatGlobalActivity.this, chatActivity.class);
                startActivity(intent);
            }
        });

        Log.d(TAG, "Executing FetchMessagesTask...");
        new FetchMessagesTask().execute();
    }

    private class FetchMessagesTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            try {
                String fullUrl = url + "/view-global-message";
                Log.d(TAG, "Sending HTTP POST request to: " + fullUrl);

                if (url == null || url.trim().isEmpty()) {
                    Log.e(TAG, "ABORTING: 'url' variable is empty!");
                    return null;
                }

                JSONObject json_chat = new JSONObject();
                json_chat.put("id", 0);
                
                int status_code = 0;
                String response = request.requestHTTP(fullUrl, "post", json_chat, status_code, ChatGlobalActivity.this);
                
                Log.d(TAG, "Server response received: " + response);
                return response;

            } catch (Exception e) {
                Log.e(TAG, "Fatal exception inside doInBackground!", e);    
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            Log.d(TAG, "onPostExecute executed. Is response null or empty? " + (response == null || response.trim().isEmpty()));

            if (response == null || response.trim().isEmpty()) {
                Toast.makeText(ChatGlobalActivity.this, "Error in message loading (Check Logcat)", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                chatContainer.removeAllViews();
                JSONArray jsonArray = new JSONArray(response);
                Log.d(TAG, "Total messages received in JSON: " + jsonArray.length());

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    int id = obj.getInt("id");
                    String text = obj.getString("message");
                    String sender = obj.getString("sender");

                    boolean isMe = sender.equalsIgnoreCase(username);
                    String displayText = sender.isEmpty() ? text : sender + ": " + text;

                    ChatBubbleView bubble = new ChatBubbleView(ChatGlobalActivity.this, displayText, isMe);
                    chatContainer.addView(bubble);
                }
                Log.d(TAG, "Messages rendered successfully on UI!");

            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error on response: " + response, e);
                Toast.makeText(ChatGlobalActivity.this, "Error in JSON parsing", Toast.LENGTH_SHORT).show();
            }
        }
    }
}