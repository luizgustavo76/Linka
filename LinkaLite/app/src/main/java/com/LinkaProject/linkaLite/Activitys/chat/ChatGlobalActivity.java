package com.LinkaProject.linkaLite;
import com.LinkaProject.linkaLite.R;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class ChatGlobalActivity extends Activity {
    private static final String TAG = "LINKA_DEBUG";
    private ImageButton btnHeaderImage;
    private TextView txtHeaderTitle;
    private TextView txtOnlineSummary;
    private Button btnSend;
    private EditText edtInputMessage;
    private LinearLayout chatContainer;
    private String username = "";
    private String url = "";
    private Handler autoUpdateHandler = new Handler();
    private Runnable autoUpdateRunnable;
    private static final int UPDATE_INTERVAL = 2000;
    private Boolean isGuest;
    private final Map<String, JSONObject> statusCache = new HashMap<String, JSONObject>();

    private String getNormalizedBaseUrl() {
        if (url == null || url.trim().length() == 0) return "";
        String clean = url.trim();
        if (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    private void pushMyStatus(final String statusValue, final String actionValue) {
        if (Boolean.TRUE.equals(isGuest) || username == null || username.replace("@", "").trim().length() == 0) {
            return;
        }
        final String cleanUser = username.replace("@", "").trim();
        final String baseUrl = getNormalizedBaseUrl();
        if (baseUrl.length() == 0) return;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("username", cleanUser);
                    payload.put("status", statusValue);
                    payload.put("action", actionValue);
                    request.requestHTTP(baseUrl + "/send-status", "post", payload, 0, ChatGlobalActivity.this);
                } catch (Exception e) {
                    Log.e(TAG, "Error sending global chat status", e);
                }
                return null;
            }
        }.execute();
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
        isGuest = getIntent().getBooleanExtra("isGuest", false);

        if (txtHeaderTitle != null && txtHeaderTitle.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) txtHeaderTitle.getParent();
            int idx = parent.indexOfChild(txtHeaderTitle);
            txtOnlineSummary = new TextView(this);
            txtOnlineSummary.setTextSize(12);
            txtOnlineSummary.setTextColor(Color.parseColor("#2E7D32"));
            txtOnlineSummary.setText("Online: ...");
            parent.addView(txtOnlineSummary, idx + 1);
        }

        if (isGuest) {
            edtInputMessage.setVisibility(View.GONE);
            btnSend.setVisibility(View.GONE);
        }
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
                new FetchGlobalOnlineTask().execute();
                autoUpdateHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        pushMyStatus("online", "no chat global");
        autoUpdateHandler.post(autoUpdateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        autoUpdateHandler.removeCallbacks(autoUpdateRunnable);
        pushMyStatus("offline", "saiu do chat global");
    }

    private class FetchGlobalOnlineTask extends AsyncTask<Void, Void, JSONArray> {
        @Override
        protected JSONArray doInBackground(Void... params) {
            try {
                String baseUrl = getNormalizedBaseUrl();
                if (baseUrl.length() == 0) return null;
                JSONObject body = new JSONObject();
                body.put("target", "global");
                String resp = request.requestHTTP(baseUrl + "/view-online", "post", body, 0, ChatGlobalActivity.this);
                if (resp != null && resp.trim().startsWith("{")) {
                    JSONObject obj = new JSONObject(resp.trim());
                    return obj.optJSONArray("online_users");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching online users", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONArray users) {
            if (txtOnlineSummary == null || users == null) return;
            int count = users.length();
            if (count == 0) {
                txtOnlineSummary.setText("0 online");
                return;
            }
            StringBuilder sb = new StringBuilder();
            int limit = Math.min(count, 4);
            for (int i = 0; i < limit; i++) {
                if (i > 0) sb.append(", ");
                String u = users.optString(i, "");
                sb.append(u);
                JSONObject stObj = statusCache.get(u.replace("@", "").trim());
                if (stObj != null) {
                    String act = stObj.optString("action", "").trim();
                    if (!act.isEmpty()) {
                        sb.append(" (").append(act).append(")");
                    }
                }
            }
            if (count > limit) {
                sb.append(" +").append(count - limit);
            }
            txtOnlineSummary.setText("Online (" + count + "): " + sb.toString());
        }
    }

    private class FetchSenderStatusTask extends AsyncTask<Void, Void, JSONObject> {
        private final String cleanSender;
        private final TextView targetView;

        public FetchSenderStatusTask(String cleanSender, TextView targetView) {
            this.cleanSender = cleanSender;
            this.targetView = targetView;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                String baseUrl = getNormalizedBaseUrl();
                if (baseUrl.length() == 0 || cleanSender.length() == 0) return null;
                JSONObject body = new JSONObject();
                body.put("username_target", cleanSender);
                String resp = request.requestHTTP(baseUrl + "/view-status", "post", body, 0, ChatGlobalActivity.this);
                if (resp != null && resp.trim().startsWith("{")) {
                    JSONObject obj = new JSONObject(resp.trim());
                    if (obj.has("status") && !"status not found".equalsIgnoreCase(obj.optString("status"))) {
                        statusCache.put(cleanSender, obj);
                        return obj;
                    }
                }
            } catch (Exception ignored) {}
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject obj) {
            if (obj != null && targetView != null && cleanSender.equals(targetView.getTag())) {
                String st = obj.optString("status", "").trim();
                String act = obj.optString("action", "").trim();
                if (!st.isEmpty() || !act.isEmpty()) {
                    String label = act.isEmpty() ? st : (st + " · " + act);
                    targetView.setText("[" + label + "]");
                    targetView.setTextColor("online".equalsIgnoreCase(st)
                            ? Color.parseColor("#2E7D32")
                            : Color.parseColor("#757575"));
                    targetView.setVisibility(View.VISIBLE);
                }
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
            if (response == null || response.trim().length() == 0) return;
            try {
                chatContainer.removeAllViews();
                JSONArray jsonArray = new JSONArray(response);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String text = obj.getString("message");
                    String sender = obj.getString("sender");
                    boolean isMe = sender.equalsIgnoreCase(username);
                    String displayText = sender.length() == 0 ? text : sender + ": " + text;

                    LinearLayout row = new LinearLayout(ChatGlobalActivity.this);
                    row.setOrientation(LinearLayout.VERTICAL);

                    ChatBubbleView bubble = new ChatBubbleView(ChatGlobalActivity.this, displayText, isMe, sender);
                    row.addView(bubble);

                    String cleanSender = sender.replace("@", "").trim();
                    if (cleanSender.length() > 0) {
                        TextView statusTag = new TextView(ChatGlobalActivity.this);
                        statusTag.setTextSize(10);
                        statusTag.setPadding(isMe ? 0 : 12, 0, isMe ? 12 : 0, 6);
                        statusTag.setGravity(isMe ? android.view.Gravity.RIGHT : android.view.Gravity.LEFT);
                        statusTag.setTag(cleanSender);
                        statusTag.setVisibility(View.GONE);
                        row.addView(statusTag);

                        JSONObject cached = statusCache.get(cleanSender);
                        if (cached != null) {
                            String st = cached.optString("status", "").trim();
                            String act = cached.optString("action", "").trim();
                            if (!st.isEmpty() || !act.isEmpty()) {
                                statusTag.setText("[" + (act.isEmpty() ? st : st + " · " + act) + "]");
                                statusTag.setTextColor("online".equalsIgnoreCase(st)
                                        ? Color.parseColor("#2E7D32")
                                        : Color.parseColor("#757575"));
                                statusTag.setVisibility(View.VISIBLE);
                            }
                        } else {
                            new FetchSenderStatusTask(cleanSender, statusTag).execute();
                        }
                    }

                    chatContainer.addView(row);
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
                String baseUrl = getNormalizedBaseUrl();
                if (baseUrl.length() == 0) return false;
                JSONObject json_chat = new JSONObject();
                json_chat.put("sender", username);
                json_chat.put("message", messageToSend);
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
                pushMyStatus("online", "sended message in global chat");
                new FetchMessagesTask().execute();
            } else {
                Toast.makeText(ChatGlobalActivity.this, "Erro ao enviar mensagem", Toast.LENGTH_SHORT).show();
            }
        }
    }
}