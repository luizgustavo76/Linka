package com.LinkaProject.linkaLite;
import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.List;
import android.widget.ImageButton;
import org.json.JSONObject;
import org.json.JSONArray;
import android.view.View;
import android.content.Intent;
import org.json.JSONException;
public class InboxActivity extends Activity {
    private ListView lvInbox;
    private InboxAdapter adapter;
    private List<InboxItem> itemList;
    private String url = "";
    private String username = "";
    private ImageButton btnHome;
    private ImageButton btnChat;
    private ImageButton btnOptions;
    private ImageButton btnProfile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);
        lvInbox = (ListView) findViewById(R.id.lvInbox);
        itemList = new ArrayList<InboxItem>();
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(InboxActivity.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            url = server.getString("url");
            username = fastLogin.getString("username");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            JSONObject jsonInbox = new JSONObject();
            jsonInbox.put("username", username);
            String responseStr = request.requestHTTP(url + "/inbox", "post", jsonInbox, InboxActivity.this);
            if (responseStr != null && !responseStr.trim().equals("")) {
                JSONObject rootObject = new JSONObject(responseStr);
                JSONArray inboxArray = rootObject.getJSONArray("inbox");
                for (int i = 0; i < inboxArray.length(); i++) {
                    JSONArray subArray = inboxArray.getJSONArray(i);
                    if (subArray.length() >= 3) {
                        String sender = subArray.getString(1);
                        String message = subArray.getString(2);
                        InboxItem item = new InboxItem(sender, message);
                        itemList.add(item);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        adapter = new InboxAdapter(this, itemList);
        lvInbox.setAdapter(adapter);
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent intent = new Intent(InboxActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });
        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent intent = new Intent(InboxActivity.this, chatActivity.class);
                startActivity(intent);
            }
        });
        btnOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent intent = new Intent(InboxActivity.this, optionActivity.class);
                startActivity(intent);
            }
        });
        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent intent = new Intent(InboxActivity.this, profile.class);
                startActivity(intent);
            }
        });
    }
}