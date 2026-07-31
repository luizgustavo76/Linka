package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

public class DmChat extends Activity {
    private static final String TAG = "LINKA_DEBUG";

    private ImageButton btnHeaderImage;
    private TextView txtHeaderTitle;
    private Button btnSend;
    private EditText edtInputMessage;
    private LinearLayout chatContainer;
    
    private String username = "";
    private String url = "";
    private String other_user = "";

    private final Handler autoUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable autoUpdateRunnable;
    private static final int UPDATE_INTERVAL = 2000; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.global_chat);

        Log.d(TAG, "=== DmChat Started ===");

        // 1. Resgata o usuário destino vindo da Activity anterior
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

        String cleanOtherUser = other_user.replace("@", "").trim();

        // 2. Carrega configurações locais
        try {
            config cfg = new config();
            String rawCfg = cfg.loadCfgAsJson(this, "config.cfg");

            if (rawCfg != null && !rawCfg.isEmpty()) {
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
        edtInputMessage = (EditText) findViewById(R.id.edtInputMessage);
        chatContainer = (LinearLayout) findViewById(R.id.layoutMessagesContainer);
        if (txtHeaderTitle != null) {
            txtHeaderTitle.setText(cleanOtherUser.isEmpty() ? "Chat" : cleanOtherUser);
        }
        if (btnSend != null) {
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
        }

        if (btnHeaderImage != null) {
            btnHeaderImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish(); // Retorna para a tela anterior
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
                if (url == null || url.isEmpty() || other_user == null || other_user.isEmpty()) {
                    Log.e(TAG, "[Fetch] URL do servidor ou usuário destino estão vazios.");
                    return null;
                }

                String cleanOtherUser = other_user.replace("@", "").trim();
                String fullUrl = url + "/view";
                
                JSONObject json_chat = new JSONObject();
                json_chat.put("user1", username);
                json_chat.put("user2", cleanOtherUser);
                json_chat.put("id", 0);

                Log.d(TAG, "[Fetch] Enviando requisição para: " + fullUrl + " | Payload: " + json_chat.toString());
                
                String response = request.requestHTTP(fullUrl, "post", json_chat, DmChat.this);
                Log.d(TAG, "[Fetch] Resposta do servidor: " + response);

                return response;
            } catch (Exception e) {
                Log.e(TAG, "[Fetch] Erro na requisição HTTP", e);    
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            if (response == null || response.trim().isEmpty() || chatContainer == null) {
                return;
            }

            try {
                JSONObject rootObject = new JSONObject(response.trim());
                
                if (!rootObject.has("messages")) {
                    Log.e(TAG, "[Fetch] Chave 'messages' não encontrada no JSON recebido.");
                    return;
                }

                JSONArray messagesArray = rootObject.getJSONArray("messages");

                // Limpa a tela antes de desenhar as mensagens recebidas
                chatContainer.removeAllViews();

                for (int i = 0; i < messagesArray.length(); i++) {
                    JSONObject msgObj = messagesArray.getJSONObject(i);
                    String text = msgObj.optString("message", "");
                    String sender = msgObj.optString("sender", "");
                    
                    boolean isMe = sender.equalsIgnoreCase(username);
                    String displayText = sender.isEmpty() ? text : sender + ": " + text;

                    ChatBubbleView bubble = new ChatBubbleView(DmChat.this, displayText, isMe);
                    chatContainer.addView(bubble);
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
                if (url == null || url.isEmpty() || other_user == null || other_user.isEmpty()) {
                    Log.e(TAG, "[Send] Dados de envio incompletos.");
                    return false;
                }

                String cleanOtherUser = other_user.replace("@", "").trim();
                String fullUrl = url + "/send-message";

                JSONObject json_chat = new JSONObject();
                json_chat.put("sender", username);
                json_chat.put("receiver", cleanOtherUser);
                json_chat.put("message", messageToSend);

                Log.d(TAG, "[Send] Enviando para: " + fullUrl + " | Payload: " + json_chat.toString());
                
                String response = request.requestHTTP(fullUrl, "post", json_chat, DmChat.this);
                Log.d(TAG, "[Send] Resposta do servidor: " + response);

                return response != null && !response.trim().isEmpty();
            } catch (Exception e) {
                Log.e(TAG, "[Send] Erro ao enviar mensagem", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                new FetchMessagesTask().execute();
            } else {
                Toast.makeText(DmChat.this, "Erro ao enviar mensagem", Toast.LENGTH_SHORT).show();
            }
        }
    }
}