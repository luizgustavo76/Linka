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
    private Button btnNewChat;
    private ListView lvFriends;
    private FriendsAdapter adapter;
    private List<FriendItem> friendsList;
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
        btnNewChat = (Button) findViewById(R.id.btnNewChat);
        friendsList = new ArrayList<FriendItem>();
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
            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("username", myUsername);
            String response = request.requestHTTP(url + "/friends", "post", jsonPayload, chatActivity.this);
            String responseGroups = request.requestHTTP(url + "/my-groups", "post", jsonPayload, chatActivity.this);
            if (responseGroups != null && !responseGroups.trim().equals("")) {
                JSONObject jsonGroupsObj = new JSONObject(responseGroups);
                if (jsonGroupsObj.has("groups")) {
                    JSONArray groupsArray = jsonGroupsObj.getJSONArray("groups");
                    for (int i = 0; i < groupsArray.length(); i++) {
                        JSONObject groupObj = groupsArray.getJSONObject(i);
                        String groupName = groupObj.isNull("group_name") ? "Group NONAME" : groupObj.getString("group_name");
                        int id = groupObj.getInt("group_id");
                        String permissions = groupObj.optString("permissions", "");
                        friendsList.add(new FriendItem(id, groupName, permissions));
                    }
                }
            }
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
        adapter = new FriendsAdapter(chatActivity.this, friendsList);
        lvFriends.setAdapter(adapter);
        lvFriends.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FriendItem clickedItem = friendsList.get(position);
                if (clickedItem.getType() == FriendsAdapter.TYPE_FRIEND) {
                    Intent intent = new Intent(chatActivity.this, DmChat.class);
                    intent.putExtra("friend", clickedItem.getUsername());
                    startActivity(intent);
                } else if (clickedItem.getType() == FriendsAdapter.TYPE_GROUP) {
                    Intent intent = new Intent(chatActivity.this, ChannelsGroupActivity.class);
                    intent.putExtra("groupId", clickedItem.getId());
                    startActivity(intent);
                }
            }
        });
        btnNewChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(chatActivity.this, newChat.class);
                startActivity(intent);
            }
        });
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