package com.LinkaProject.linkaLite;

import com.LinkaProject.linkaLite.R;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatGlobalActivity extends Activity {
    private static final String TAG = "LINKA_DEBUG";
    private static final int REQ_PICK_IMAGE = 1001;

    private ImageButton btnHeaderImage;
    private TextView txtHeaderTitle;
    private Button btnSend;
    private EditText edtInputMessage;
    private ScrollView scrollMessages;
    private LinearLayout chatContainer;
    private String username = "";
    private String media = "";
    private ImageButton btnImage;
    private ImageButton btnOptions;
    private String url = "";
    private Handler autoUpdateHandler = new Handler();
    private Runnable autoUpdateRunnable;
    private static final int UPDATE_INTERVAL = 2000;
    private Boolean isGuest;
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

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Escolher imagem"), REQ_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedUri = data.getData();
            if (selectedUri != null) {
                new UploadChatImageTask(selectedUri).execute();
            } else {
                Toast.makeText(this, "Imagem inválida", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String extractUrlFromResponse(String rawResponse) {
        if (rawResponse == null) return null;
        String trimmed = rawResponse.trim();
        if (trimmed.length() == 0) return null;

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        try {
            JSONObject obj = new JSONObject(trimmed);
            if (obj.has("error") && !obj.isNull("error")) {
                return null;
            }
            String[] keys = {"url", "image_url", "file_url", "link", "path"};
            for (int i = 0; i < keys.length; i++) {
                String candidate = obj.optString(keys[i], "").trim();
                if (candidate.length() > 0) {
                    return candidate;
                }
            }
            JSONObject dataObj = obj.optJSONObject("data");
            if (dataObj != null) {
                for (int i = 0; i < keys.length; i++) {
                    String candidate = dataObj.optString(keys[i], "").trim();
                    if (candidate.length() > 0) {
                        return candidate;
                    }
                }
            }
        } catch (JSONException ignored) {
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.global_chat);
        Log.d(TAG, "=== ChatGlobalActivity Started ===");
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
        btnImage = (ImageButton) findViewById(R.id.btnImage);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        btnHeaderImage = (ImageButton) findViewById(R.id.btnHeaderImage);
        txtHeaderTitle = (TextView) findViewById(R.id.txtHeaderTitle);
        btnSend = (Button) findViewById(R.id.btnSend);
        edtInputMessage = (EditText) findViewById(R.id.edtInputMessage);
        scrollMessages = (ScrollView) findViewById(R.id.scrollMessages);
        chatContainer = (LinearLayout) findViewById(R.id.layoutMessagesContainer);
        isGuest = getIntent().getBooleanExtra("isGuest", false);

        if (isGuest) {
            edtInputMessage.setVisibility(View.GONE);
            btnSend.setVisibility(View.GONE);
            if (btnImage != null) {
                btnImage.setVisibility(View.GONE);
            }
        }

        if (btnImage != null) {
            btnImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openImagePicker();
                }
            });
        }

        if (btnSend != null) {
            btnSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String text_message = edtInputMessage.getText().toString();
                    if (text_message.trim().length() > 0 || media.length() > 0) {
                        String mediaToSend = media;
                        edtInputMessage.setText("");
                        media = "";
                        new SendMessageTask(text_message, mediaToSend).execute();
                    }
                }
            });
        }
        if (btnHeaderImage != null) {
            btnHeaderImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ChatGlobalActivity.this, chatActivity.class);
                    startActivity(intent);
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

    private class UploadChatImageTask extends AsyncTask<Void, Void, String> {
        private Uri imageUri;

        public UploadChatImageTask(Uri imageUri) {
            this.imageUri = imageUri;
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(ChatGlobalActivity.this, "Enviando imagem...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(Void... params) {
            String baseUrl = getNormalizedBaseUrl();
            if (baseUrl.length() == 0) return null;
            String uploadUrl = baseUrl + "/upload-image";
            String rawResponse = UploadTask.uploadProfilePicture(ChatGlobalActivity.this, imageUri, uploadUrl, username);
            Log.d(TAG, "[Upload] Resposta bruta: " + rawResponse);
            return extractUrlFromResponse(rawResponse);
        }

        @Override
        protected void onPostExecute(String uploadedUrl) {
            if (uploadedUrl != null && uploadedUrl.length() > 0) {
                media = uploadedUrl;
                Log.d(TAG, "[Upload] media salva: " + media);
                Toast.makeText(ChatGlobalActivity.this, "Mídia anexada: " + media, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ChatGlobalActivity.this, "Falha ao obter URL da imagem", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class FetchMessagesTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                String baseUrl = getNormalizedBaseUrl();
                if (baseUrl.length() == 0) return null;
                String fullUrl = baseUrl + "/view-global-message";
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
            if (response == null || response.trim().length() == 0 || chatContainer == null) return;
            try {
                JSONArray jsonArray = new JSONArray(response);
                boolean shouldScroll = isFirstLoad || forceScrollNextFetch || isNearBottom();

                chatContainer.removeAllViews();
                String previousSender = null;

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String text = obj.optString("message", "");
                    String sender = obj.optString("sender", "");
                    boolean isMe = sender.equalsIgnoreCase(username);
                    boolean isRepeated = (previousSender != null && previousSender.equals(sender));

                    ChatBubbleView bubble = new ChatBubbleView(ChatGlobalActivity.this, text, isMe, sender, isRepeated);
                    chatContainer.addView(bubble);

                    previousSender = sender;
                }

                if (shouldScroll) {
                    scrollToBottom();
                    isFirstLoad = false;
                    forceScrollNextFetch = false;
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error", e);
            }
        }
    }

    private class SendMessageTask extends AsyncTask<Void, Void, Boolean> {
        private String messageToSend;
        private String mediaToSend;

        public SendMessageTask(String message) {
            this(message, "");
        }

        public SendMessageTask(String message, String mediaUrl) {
            this.messageToSend = message != null ? message : "";
            this.mediaToSend = mediaUrl != null ? mediaUrl : "";
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                String baseUrl = getNormalizedBaseUrl();
                if (baseUrl.length() == 0) return false;
                JSONObject json_chat = new JSONObject();
                json_chat.put("sender", username);
                json_chat.put("message", messageToSend);
                if (mediaToSend.length() > 0) {
                    json_chat.put("media", mediaToSend);
                }
                request.requestHTTP(baseUrl + "/send-global-message", "post", json_chat, 0, ChatGlobalActivity.this);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error sending message", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                forceScrollNextFetch = true;
                new FetchMessagesTask().execute();
            } else {
                Toast.makeText(ChatGlobalActivity.this, "Erro ao enviar mensagem", Toast.LENGTH_SHORT).show();
            }
        }
    }
}