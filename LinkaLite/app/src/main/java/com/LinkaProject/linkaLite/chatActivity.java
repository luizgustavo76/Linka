package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class chatActivity extends Activity {

    private Button btnGlobalChat;
    private ImageButton btnHome;
    private ImageButton btnChat;
    private ImageButton btnOptions;
    private ImageButton btnProfile;

    // Elementos da Lista
    private ListView lvFriends;
    private FriendsAdapter adapter;
    private List<FriendItem> friendsList;

    private String response = "";
    private String url = "";
    private String myUsername = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        btnGlobalChat = (Button) findViewById(R.id.btnGlobalChat);
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        lvFriends = (ListView) findViewById(R.id.lvFriends);

        friendsList = new ArrayList<FriendItem>();

        // 2. Carregar configurações
        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(chatActivity.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");

            url = server.getString("url");
            myUsername = fastLogin.getString("username");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            JSONObject jsonFriends = new JSONObject();
            jsonFriends.put("username", myUsername);
            response = request.requestHTTP(url + "/friends", "post", jsonFriends, chatActivity.this);

            if (response != null && !response.trim().equals("")) {
                JSONArray friends = new JSONObject(response).getJSONArray("friends");

                for (int i = 0; i < friends.length(); i++) {
                    JSONArray pair = friends.getJSONArray(i);
                    
                    String friend = pair.getString(0).equalsIgnoreCase(myUsername) ? pair.getString(1) : pair.getString(0);
                    
                    friendsList.add(new FriendItem("@" + friend));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        adapter = new FriendsAdapter(chatActivity.this, friendsList);
        lvFriends.setAdapter(adapter);

        lvFriends.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FriendItem clickedFriend = friendsList.get(position);
                if (clickedFriend.getType() == FriendsAdapter.TYPE_FRIEND) {
                    Intent intent = new Intent(chatActivity.this, DmChat.class); 
                    intent.putExtra("friend", clickedFriend.getUsername());
                    startActivity(intent);
                }
            }
        });

        // 6. Navegação dos botões do menu
        btnGlobalChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(chatActivity.this, ChatGlobalActivity.class);
                startActivity(intent);
            }
        });

        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(chatActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });

        btnOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(chatActivity.this, optionActivity.class);
                startActivity(intent);
            }
        });

        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(chatActivity.this, profile.class);
                startActivity(intent);
            }
        });
    }
}