package com.LinkaProject.linkaLite;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DmChat extends BaseActivity {
    private static final String TAG = "LINKA_DEBUG";
    private ImageButton btnHeaderImage;
    private TextView txtHeaderTitle;
    private Button btnSend;
    private EditText edtInputMessage;
    private LinearLayout chatContainer;
    private String username = "";
    private String url = "";
    private String other_user = "";
    private ImageView imgProfilePicture;
    private String cleanOtherUser = "";
    private final Handler autoUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable autoUpdateRunnable;
    private static final int UPDATE_INTERVAL = 2000;
    private ScrollView scrollMessages;
    private boolean isFirstLoad = true;
    private boolean forceScrollNextFetch = false;

    private String getNormalizedBaseUrl() {
        if (url == null || url.trim().length() == 0) return "";
        String clean = url.trim();
        if (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    private boolean isNearBottom() {
        if (scrollMessages == null || chatContainer == null) return true;
        int childCount = chatContainer.getChildCount();
        if (childCount == 0) return true;
        View lastChild = chatContainer.getChildAt(childCount - 1);
        if (lastChild == null) return true;
        int diff = lastChild.getBottom() - (scrollMessages.getHeight() + scrollMessages.getScrollY());
        return diff <= 120;
    }

    private void scrollToBottom() {
        if (scrollMessages != null) {
            scrollMessages.post(new Runnable() {
                @Override
                public void run() {
                    scrollMessages.fullScroll(View.FOCUS_DOWN);
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.global_chat);
        Log.d(TAG, "=== DmChat Started ===");
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("target_user")) {
                other_user = intent.getStringExtra("target_user");
            } else if (intent.hasExtra("friend")) {
                other_user = intent.getStringExtra("friend");
            }
        }
        if (other_user == null) {
            other_user = "";
        }
        cleanOtherUser = other_user.replace("@", "").trim();
        try {
            config cfg = new config();
            String rawCfg = cfg.loadCfgAsJson(this, "config.cfg");
            if (rawCfg != null && rawCfg.length() > 0) {
                JSONObject jsonCfg = new JSONObject(rawCfg);
                JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
                JSONObject server = jsonCfg.getJSONObject("SERVER");
                username = fastLogin.optString("username", "").replace("@", "").trim();
                url = server.optString("url", "").trim();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing config.cfg", e);
        }
        btnHeaderImage = (ImageButton) findViewById(R.id.btnHeaderImage);
        txtHeaderTitle = (TextView) findViewById(R.id.txtHeaderTitle);
        btnSend = (Button) findViewById(R.id.btnSend);
        imgProfilePicture = (ImageView) findViewById(R.id.imgProfilePicture);
        ImageLoader imageLoader = new ImageLoader();
        imageLoader.viewProfilePicture(DmChat.this, other_user, imgProfilePicture);
        scrollMessages = (ScrollView) findViewById(R.id.scrollMessages);
        edtInputMessage = (EditText) findViewById(R.id.edtInputMessage);
        chatContainer = (LinearLayout) findViewById(R.id.layoutMessagesContainer);
        if (txtHeaderTitle != null) {
            txtHeaderTitle.setText(cleanOtherUser.length() == 0 ? "Chat" : cleanOtherUser);
        }
        if (btnSend != null) {
            btnSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String text_message = edtInputMessage.getText().toString();
                    if (text_message.trim().length() > 0) {
                        edtInputMessage.setText("");
                        new SendMessageTask(text_message).execute();
                    }
                }
            });
        }
        if (btnHeaderImage != null) {
            btnHeaderImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
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
                String baseUrl = getNormalizedBaseUrl();
                if (baseUrl.length() == 0) return null;
                String fullUrl = baseUrl + "/view";
                JSONObject json_chat = new JSONObject();
                json_chat.put("user1", username);
                json_chat.put("user2", cleanOtherUser);
                json_chat.put("id", 0);
                String response = request.requestHTTP(fullUrl, "post", json_chat, DmChat.this);
                Log.d(TAG, "[Fetch] Resposta do servidor: " + response);
                return response;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            if (response == null || response.trim().length() == 0 || chatContainer == null) {
                return;
            }
            try {
                JSONObject rootObject = new JSONObject(response.trim());
                if (!rootObject.has("messages")) {
                    return;
                }
                JSONArray messagesArray = rootObject.getJSONArray("messages");
                boolean shouldScroll = isFirstLoad || forceScrollNextFetch || isNearBottom();

                chatContainer.removeAllViews();
                String previousSender = null;

                for (int i = 0; i < messagesArray.length(); i++) {
                    JSONObject msgObj = messagesArray.getJSONObject(i);
                    String text = msgObj.optString("message", "");
                    String sender = msgObj.optString("sender", "");
                    boolean isMe = sender.equalsIgnoreCase(username);
                    boolean isRepeated = (previousSender != null && previousSender.equals(sender));

                    String displayText = sender.length() == 0 ? text : sender + ": " + text;
                    ChatBubbleView bubble = new ChatBubbleView(DmChat.this, displayText, isMe, sender, isRepeated);
                    chatContainer.addView(bubble);

                    previousSender = sender;
                }

                if (shouldScroll) {
                    scrollToBottom();
                    isFirstLoad = false;
                    forceScrollNextFetch = false;
                }
            } catch (JSONException e) {
                Log.e(TAG, "[Fetch] Falha ao processar o JSON de resposta: " + response, e);
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
                String baseUrl = getNormalizedBaseUrl();
                if (baseUrl.length() == 0 || cleanOtherUser.length() == 0) {
                    return false;
                }
                String fullUrl = baseUrl + "/send-message";
                JSONObject json_chat = new JSONObject();
                json_chat.put("sender", username);
                json_chat.put("receiver", cleanOtherUser);
                json_chat.put("message", messageToSend);
                String response = request.requestHTTP(fullUrl, "post", json_chat, DmChat.this);
                return response != null && response.trim().length() > 0;
            } catch (Exception e) {
                return false;
            }
        }
        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                forceScrollNextFetch = true;
                new FetchMessagesTask().execute();
            } else {
                Toast.makeText(DmChat.this, "Error in mesage sending", Toast.LENGTH_SHORT).show();
            }
        }
    }
}