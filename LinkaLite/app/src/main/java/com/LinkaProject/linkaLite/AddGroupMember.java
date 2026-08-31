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
public class AddGroupMember extends Activity {
    private ImageButton btnHome;
    private ImageButton btnChat;
    private ImageButton btnOptions;
    private ImageButton btnProfile;
    private ListView lvFriends;
    private FriendsAdapter adapter;
    private List<FriendItem> friendsList;
    private String url = "";
    private String myUsername = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        lvFriends = (ListView) findViewById(R.id.lvFriends);
        friendsList = new ArrayList<FriendItem>();
        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(AddGroupMember.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            url = server.getString("url");
            myUsername = fastLogin.getString("username");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("username", myUsername);
            String response = request.requestHTTP(url + "/friends", "post", jsonPayload, AddGroupMember.this);
            if (response != null && !response.trim().equals("")) {
                JSONObject jsonFriendsObj = new JSONObject(response);
                if (jsonFriendsObj.has("friends")) {
                    JSONArray friends = jsonFriendsObj.getJSONArray("friends");
                    for (int i = 0; i < friends.length(); i++) {
                        JSONArray pair = friends.getJSONArray(i);
                        String friend = pair.getString(0).equalsIgnoreCase(myUsername) ? pair.getString(1) : pair.getString(0);
                        friendsList.add(new FriendItem(friend));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        adapter = new FriendsAdapter(AddGroupMember.this, friendsList);
        lvFriends.setAdapter(adapter);
        lvFriends.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FriendItem clickedItem = friendsList.get(position);
                String friendName = clickedItem.getUsername();
                try{
                    JSONObject jsonInvite = new JSONObject();
                    Intent intentId = getIntent();
                    int group_id = intentId.getIntExtra("group_id", -1);
                    jsonInvite.put("username", myUsername);
                    jsonInvite.put("acess_limit", 1);
                    jsonInvite.put("expire_at", "never expire");
                    jsonInvite.put("group_id", group_id);
                    JSONObject responseInvite = new JSONObject(request.requestHTTP(url + "/generate-invite", "post", jsonInvite, AddGroupMember.this));
                    String code = responseInvite.optString("code", "");
                    JSONObject jsonMessage = new JSONObject();
                    jsonMessage.put("receiver", friendName);
                    jsonMessage.put("sender", myUsername);
                    jsonMessage.put("message", "[INVITE]" + code);
                    request.requestHTTP(url + "/send-message", "post", jsonMessage, AddGroupMember.this);
                }catch(JSONException e){
                    e.printStackTrace();
                }
                if (clickedItem.getType() == FriendsAdapter.TYPE_FRIEND) {
                    Intent intent = new Intent(AddGroupMember.this, DmChat.class);
                    intent.putExtra("friend", clickedItem.getUsername());
                    startActivity(intent);
                }
            }
        });
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddGroupMember.this, HomeActivity.class);
                startActivity(intent);
            }
        });
        btnOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddGroupMember.this, optionActivity.class);
                startActivity(intent);
            }
        });
        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddGroupMember.this, profile.class);
                startActivity(intent);
            }
        });
    }
}