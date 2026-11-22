package com.LinkaProject.linkaLite;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InboxActivity extends BaseActivity {
    private ListView lvInbox;
    private TextView tvInboxCount;
    private InboxAdapter adapter;
    private List<InboxItem> itemList;
    private String url = "";
    private String username = "";

    private ImageButton btnTopChat;
    private ImageButton navHome;
    private ImageButton navCommunities;
    private ImageButton fabNewPost;
    private ImageButton btnNotifications;
    private ImageButton btnProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        lvInbox = (ListView) findViewById(R.id.lvInbox);
        tvInboxCount = (TextView) findViewById(R.id.tvInboxCount);
        itemList = new ArrayList<InboxItem>();

        btnTopChat = (ImageButton) findViewById(R.id.btnTopChat);
        navHome = (ImageButton) findViewById(R.id.navHome);
        navCommunities = (ImageButton) findViewById(R.id.navCommunities);
        fabNewPost = (ImageButton) findViewById(R.id.fabNewPost);
        btnNotifications = (ImageButton) findViewById(R.id.btnNotifications);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);

        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(InboxActivity.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            url = server.getString("url");
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            username = fastLogin.getString("username");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("username", username);

            // 1. Solicitações (/inbox) -> retorna {"inbox": [[id, sender, message], ...]}
            String responseInbox = request.requestHTTP(url + "/inbox", "post", payload, InboxActivity.this);
            parseInboxRequests(responseInbox);

            // 2. Notificações (/notifications) -> retorna JSONArray [{"id": 1, "from_user": "...", "type": "...", "content": "..."}]
            String responseNotifications = request.requestHTTP(url + "/notifications", "post", payload, InboxActivity.this);
            parseNotifications(responseNotifications);

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (tvInboxCount != null) {
            tvInboxCount.setText(String.valueOf(itemList.size()));
        }

        adapter = new InboxAdapter(this, itemList, url, username);
        lvInbox.setAdapter(adapter);

        if (btnTopChat != null) {
            btnTopChat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(InboxActivity.this, chatActivity.class));
                }
            });
        }

        if (navHome != null) {
            navHome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(InboxActivity.this, HomeActivity.class));
                    finish();
                }
            });
        }

        if (navCommunities != null) {
            navCommunities.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        startActivity(new Intent(InboxActivity.this, Class.forName("com.LinkaProject.linkaLite.CommunitiesActivity")));
                    } catch (Exception ignored) {}
                }
            });
        }

        if (fabNewPost != null) {
            fabNewPost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        startActivity(new Intent(InboxActivity.this, Class.forName("com.LinkaProject.linkaLite.NewPostActivity")));
                    } catch (Exception ignored) {}
                }
            });
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(InboxActivity.this, profile.class));
                }
            });
        }
    }

    private void parseInboxRequests(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().length() == 0) return;
        try {
            JSONObject rootObject = new JSONObject(jsonStr);
            JSONArray inboxArray = rootObject.optJSONArray("inbox");
            if (inboxArray == null) return;

            for (int i = 0; i < inboxArray.length(); i++) {
                JSONArray subArray = inboxArray.optJSONArray(i);
                if (subArray != null && subArray.length() >= 2) {
                    int reqId = subArray.optInt(0, -1);
                    String sender = subArray.optString(1, "");
                    String message = subArray.length() >= 3 ? subArray.optString(2, "") : "";
                    if (sender != null && sender.trim().length() > 0 && !"null".equalsIgnoreCase(sender)) {
                        itemList.add(new InboxItem(reqId, sender.trim(), message, "REQUEST", InboxItem.TYPE_REQUEST));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseNotifications(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().length() == 0) return;
        try {
            String trimmed = jsonStr.trim();
            JSONArray notifArray = null;

            if (trimmed.startsWith("[")) {
                notifArray = new JSONArray(trimmed);
            } else if (trimmed.startsWith("{")) {
                JSONObject obj = new JSONObject(trimmed);
                notifArray = obj.optJSONArray("notifications");
            }

            if (notifArray == null) return;

            for (int i = 0; i < notifArray.length(); i++) {
                JSONObject row = notifArray.optJSONObject(i);
                if (row == null) continue;

                int notifId = row.optInt("id", -1);
                String fromUser = row.optString("from_user", "");
                String notifType = row.optString("type", "NOTIFY");
                String content = row.optString("content", "");

                if (fromUser == null || fromUser.trim().length() == 0 || "null".equalsIgnoreCase(fromUser)) {
                    fromUser = "Linka";
                }

                itemList.add(new InboxItem(notifId, fromUser.trim(), content, notifType, InboxItem.TYPE_NOTIFICATION));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}