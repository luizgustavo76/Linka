package com.LinkaProject.linkaLite;

import com.LinkaProject.linkaLite.R;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GroupChatActivity extends Activity {
    private static final String TAG = "LINKA_DEBUG";
    private ImageButton btnHeaderImage;
    private TextView txtHeaderTitle;
    private Button btnSend;
    private EditText edtInputMessage;
    private LinearLayout chatContainer;
    private String username = "";
    private String url = "";
    private Handler autoUpdateHandler = new Handler();
    private Runnable autoUpdateRunnable;
    private static final int UPDATE_INTERVAL = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.global_chat);

        try {
            config cfg = new config();
            String rawCfg = cfg.loadCfgAsJson(this, "config.cfg");
            if (rawCfg != null && !rawCfg.isEmpty()) {
                JSONObject jsonCfg = new JSONObject(rawCfg);
                JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
                JSONObject server = jsonCfg.getJSONObject("SERVER");
                username = fastLogin.optString("username", "");
                url = server.optString("url", "");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing config.cfg", e);
        }

        btnHeaderImage = (ImageButton) findViewById(R.id.btnHeaderImage);
        txtHeaderTitle = (TextView) findViewById(R.id.txtHeaderTitle);
        btnSend = (Button) findViewById(R.id.btnSend);
        edtInputMessage = (EditText) findViewById(R.id.edtInputMessage);
        chatContainer = (LinearLayout) findViewById(R.id.layoutMessagesContainer);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String textMessage = edtInputMessage.getText().toString();
                if (!textMessage.trim().isEmpty()) {
                    edtInputMessage.setText("");
                    new SendMessageTask(textMessage).execute();
                }
            }
        });

        btnHeaderImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GroupChatActivity.this, chatActivity.class);
                startActivity(intent);
            }
        });

        autoUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                new FetchMessagesTask().execute();
                autoUpdateHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        autoUpdateHandler.post(autoUpdateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        autoUpdateHandler.removeCallbacks(autoUpdateRunnable);
    }

    private class FetchMessagesTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                String fullUrl = url + "/view-group-message";
                if (url == null || url.trim().isEmpty()) return null;

                Intent intent = getIntent();
                String channel = intent.getStringExtra("channel");
                int groupId = intent.getIntExtra("group_id", -1);

                JSONObject jsonChat = new JSONObject();
                jsonChat.put("id", 0);
                jsonChat.put("channel", channel);
                jsonChat.put("username", username);
                jsonChat.put("group_id", groupId);

                return request.requestHTTP(fullUrl, "post", jsonChat, GroupChatActivity.this);
            } catch (Exception e) {
                Log.e(TAG, "Exception in FetchMessagesTask", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            if (response == null || response.trim().isEmpty()) return;

            try {
                chatContainer.removeAllViews();
                JSONArray jsonArray = new JSONArray(response);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String text = obj.optString("message", "");
                    String sender = obj.optString("sender", "");

                    boolean isMe = sender.equalsIgnoreCase(username);
                    String displayText = sender.isEmpty() ? text : sender + ": " + text;

                    ChatBubbleView bubble = new ChatBubbleView(GroupChatActivity.this, displayText, isMe);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 6, 0, 6);
                    bubble.setLayoutParams(params);

                    chatContainer.addView(bubble);
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error", e);
            }
        }
    }

    private class SendMessageTask extends AsyncTask<Void, Void, Boolean> {
        private String messageToSend;

        public SendMessageTask(String message) {
            this.messageToSend = message;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                JSONObject jsonChat = new JSONObject();
                Intent intent = getIntent();
                String channel = intent.getStringExtra("channel");
                int groupId = intent.getIntExtra("group_id", -1);

                jsonChat.put("message", messageToSend);
                jsonChat.put("channel", channel);
                jsonChat.put("username", username);
                jsonChat.put("group_id", groupId);

                request.requestHTTP(url + "/send-group-message", "post", jsonChat, GroupChatActivity.this);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error sending message", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                new FetchMessagesTask().execute();
            } else {
                Toast.makeText(GroupChatActivity.this, "Error sending message", Toast.LENGTH_SHORT).show();
            }
        }
    }
}