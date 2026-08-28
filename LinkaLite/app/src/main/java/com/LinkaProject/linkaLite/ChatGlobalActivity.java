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
public class ChatGlobalActivity extends Activity {
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
        Log.d(TAG, "=== ChatGlobalActivity Started ===");
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
                String text_message = edtInputMessage.getText().toString();
                if (!text_message.trim().isEmpty()) {
                    edtInputMessage.setText(""); 
                    new SendMessageTask(text_message).execute();
                }
            }
        });
        btnHeaderImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatGlobalActivity.this, chatActivity.class);
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
                String fullUrl = url + "/view-global-message";
                if (url == null || url.trim().isEmpty()) return null;
                JSONObject json_chat = new JSONObject();
                json_chat.put("id", 0);
                return request.requestHTTP(fullUrl, "post", json_chat, 0, ChatGlobalActivity.this);
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
                    String text = obj.getString("message");
                    String sender = obj.getString("sender");
                    boolean isMe = sender.equalsIgnoreCase(username);
                    String displayText = sender.isEmpty() ? text : sender + ": " + text;
                    ChatBubbleView bubble = new ChatBubbleView(ChatGlobalActivity.this, displayText, isMe);
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
                JSONObject json_chat = new JSONObject();
                json_chat.put("sender", username);
                json_chat.put("message", messageToSend);
                request.requestHTTP(url + "/send-global-message", "post", json_chat, 0, ChatGlobalActivity.this);
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
                Toast.makeText(ChatGlobalActivity.this, "Erro ao enviar mensagem", Toast.LENGTH_SHORT).show();
            }
        }
    }
}