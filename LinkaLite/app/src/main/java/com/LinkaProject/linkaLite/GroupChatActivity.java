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
    private int groupId = -1;
    private static final int UPDATE_INTERVAL = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.global_chat);

        Log.d(TAG, "GroupChatActivity onCreate iniciado");

        try {
            config cfg = new config();
            String rawCfg = cfg.loadCfgAsJson(this, "config.cfg");
            Log.d(TAG, "Config bruto lido: " + rawCfg);

            if (rawCfg != null && !rawCfg.isEmpty()) {
                JSONObject jsonCfg = new JSONObject(rawCfg);
                JSONObject fastLogin = jsonCfg.optJSONObject("FAST_LOGIN");
                JSONObject server = jsonCfg.optJSONObject("SERVER");

                if (fastLogin != null) username = fastLogin.optString("username", "");
                if (server != null) url = server.optString("url", "");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao ler/processar config.cfg", e);
        }

        Log.d(TAG, "Config carregado -> Username: '" + username + "' | URL: '" + url + "'");

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
        Log.d(TAG, "onResume: iniciando polling de mensagens");
        autoUpdateHandler.post(autoUpdateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: parando polling de mensagens");
        autoUpdateHandler.removeCallbacks(autoUpdateRunnable);
    }

    private class FetchMessagesTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                if (url == null || url.trim().isEmpty()) {
                    Log.e(TAG, "FetchMessagesTask abortado: URL esta VAZIA ou NULA!");
                    return null;
                }

                String fullUrl = url + "/view-group-message";
                Intent intent = getIntent();
                String channel = intent.getStringExtra("channel");
                int groupId = intent.getIntExtra("group_id", -1);

                Log.d(TAG, "FetchMessagesTask -> URL: " + fullUrl + " | GroupID: " + groupId + " | Channel: " + channel);

                JSONObject jsonChat = new JSONObject();
                jsonChat.put("id", 0);
                jsonChat.put("channel", channel);
                jsonChat.put("username", username);
                jsonChat.put("group_id", groupId);

                Log.d(TAG, "FetchMessagesTask enviando JSON: " + jsonChat.toString());

                String response = request.requestHTTP(fullUrl, "post", jsonChat, GroupChatActivity.this);
                Log.d(TAG, "FetchMessagesTask resposta bruta: " + response);
                return response;

            } catch (Exception e) {
                Log.e(TAG, "Excecao dentro de FetchMessagesTask doInBackground", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            if (response == null || response.trim().isEmpty()) {
                Log.e(TAG, "FetchMessagesTask onPostExecute: Resposta veio NULA ou VAZIA do servidor.");
                return;
            }

            try {
                JSONObject rootJson = new JSONObject(response);

                if (rootJson.has("messages")) {
                    JSONArray jsonArray = rootJson.getJSONArray("messages");
                    Log.d(TAG, "Mensagens recebidas: " + jsonArray.length() + " item(ns)");

                    chatContainer.removeAllViews();

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
                } else {
                    Log.e(TAG, "JSON do servidor nao contem a chave 'messages'. Resposta: " + response);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Erro de JSON ao processar resposta do servidor. Resposta foi: " + response, e);
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
                jsonChat.put("sender", username);   // O Flask exige este campo em /send-group-message
                jsonChat.put("username", username);
                jsonChat.put("group_id", groupId);

                String fullUrl = url + "/send-group-message";
                Log.d(TAG, "SendMessageTask enviando mensagem para: " + fullUrl + " Payload: " + jsonChat.toString());

                String res = request.requestHTTP(fullUrl, "post", jsonChat, GroupChatActivity.this);
                Log.d(TAG, "SendMessageTask resposta: " + res);

                return res != null && res.contains("success");
            } catch (Exception e) {
                Log.e(TAG, "Erro em SendMessageTask doInBackground", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Log.d(TAG, "Mensagem enviada com sucesso. Forcando atualizacao...");
                new FetchMessagesTask().execute();
            } else {
                Log.e(TAG, "Falha ao enviar mensagem.");
                Toast.makeText(GroupChatActivity.this, "Error sending message", Toast.LENGTH_SHORT).show();
            }
        }
    }
}