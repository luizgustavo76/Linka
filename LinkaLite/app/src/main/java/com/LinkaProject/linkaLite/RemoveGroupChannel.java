package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RemoveGroupChannel extends Activity {

    private ImageButton btnBack;
    private TextView textGroup;
    private Button btnAdd;
    private Button btnConfig;
    private ImageButton btnHome;
    private ImageButton btnChat;
    private ImageButton btnOptions;
    private ImageButton btnProfile;
    private ListView listChannelsView;
    private ListView listViewMembers;

    private String url = "";
    private String username = "";
    private int groupId = -1;
    private List<Channel> channelList;
    private ChannelAdapter adapter;
    private Button btnAddMember;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channels);

        Intent intent = getIntent();
        groupId = intent.getIntExtra("groupId", -1);
        if (groupId == -1) {
            Toast.makeText(this, "ID do grupo inválido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            url = server.getString("url");
            username = fastLogin.getString("username");
        } catch (Exception e) {
            e.printStackTrace();
        }

        btnBack = (ImageButton) findViewById(R.id.btnBack);
        textGroup = (TextView) findViewById(R.id.textGroup);
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        listChannelsView = (ListView) findViewById(R.id.listChannels);
        listViewMembers = (ListView) findViewById(R.id.listViewMembers);
        channelList = new ArrayList<Channel>();

        new FetchMembersTask().execute();
        new FetchChannelsTask().execute();
        
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RemoveGroupChannel.this, HomeActivity.class));
            }
        });

        btnOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RemoveGroupChannel.this, optionActivity.class));
            }
        });

        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RemoveGroupChannel.this, profile.class));
            }
        });

        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RemoveGroupChannel.this, chatActivity.class));
            }
        });
    }

    private class FetchMembersTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                JSONObject jsonMembers = new JSONObject();
                jsonMembers.put("username", username);
                jsonMembers.put("group_id", groupId);
                return request.requestHTTP(url + "/members", "POST", jsonMembers, RemoveGroupChannel.this);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null || result.length() == 0) {
                return;
            }
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONArray membersArray = jsonObject.getJSONArray("members");
                List<Member> memberList = new ArrayList<Member>();

                for (int i = 0; i < membersArray.length(); i++) {
                    String memberName = membersArray.getString(i);
                    ImageView imageView = new ImageView(RemoveGroupChannel.this);
                    ImageLoader imageLoader = new ImageLoader();
                    imageLoader.viewProfilePicture(RemoveGroupChannel.this, memberName, imageView);

                    memberList.add(new Member(memberName, imageView));
                }

                MemberAdapter memberAdapter = new MemberAdapter(RemoveGroupChannel.this, memberList);
                listViewMembers.setAdapter(memberAdapter);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class FetchChannelsTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                JSONObject jsonChannels = new JSONObject();
                jsonChannels.put("username", username);
                jsonChannels.put("group_id", groupId);
                return request.requestHTTP(url + "/view-channels", "POST", jsonChannels, RemoveGroupChannel.this);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null || result.length() == 0) {
                Toast.makeText(RemoveGroupChannel.this, "Erro ao carregar canais", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                JSONArray array = new JSONArray(result);
                channelList.clear();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String channelName = obj.optString("channel_name", "General");
                    channelList.add(new Channel(channelName, "C"));
                }
                adapter = new ChannelAdapter(RemoveGroupChannel.this, channelList);
                listChannelsView.setAdapter(adapter);
                listChannelsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        try{
                            Channel selectedChannel = channelList.get(position);
                            JSONObject jsonRemove = new JSONObject();
                            jsonRemove.put("username", username);
                            jsonRemove.put("channel_name", selectedChannel.getName());
                            jsonRemove.put("group_id", groupId);
                            request.requestHTTP(url + "/remove-channel", "post", jsonRemove, RemoveGroupChannel.this);
                            Intent intentGroup = new Intent(RemoveGroupChannel.this, ChannelsGroupActivity.class);
                            intentGroup.putExtra("groupId", groupId);
                            startActivity(intentGroup);
                        }catch(JSONException e){
                            e.printStackTrace();
                        }    
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}