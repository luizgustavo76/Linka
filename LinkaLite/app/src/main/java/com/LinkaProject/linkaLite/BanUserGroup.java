package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BanUserGroup extends Activity {
    private ImageButton btnHome;
    private ImageButton btnChat;
    private ImageButton btnOptions;
    private ImageButton btnProfile;
    private ListView lvFriends;
    private FriendsAdapter adapter;
    private List<FriendItem> friendsList;
    private String url = "";
    private String myUsername = "";
    private int groupId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        lvFriends = (ListView) findViewById(R.id.lvFriends);

        Intent intent = getIntent();
        groupId = intent.getIntExtra("groupId", -1);

        friendsList = new ArrayList<FriendItem>();
        adapter = new FriendsAdapter(BanUserGroup.this, friendsList);
        lvFriends.setAdapter(adapter);

        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(BanUserGroup.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            url = server.getString("url");
            myUsername = fastLogin.getString("username");
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("BanUserGroup", "Erro ao carregar arquivo de configuracao config.cfg");
        }

        fetchMembers();

        lvFriends.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final FriendItem clickedItem = friendsList.get(position);
                final String friendName = clickedItem.getUsername();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonRemove = new JSONObject();
                            jsonRemove.put("username", myUsername);
                            jsonRemove.put("group_id", groupId);
                            jsonRemove.put("target_user", friendName);
                            request.requestHTTP(url + "/remove-user", "post", jsonRemove, BanUserGroup.this);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

                if (clickedItem.getType() == FriendsAdapter.TYPE_FRIEND) {
                    Intent intentRemove = new Intent(BanUserGroup.this, DmChat.class);
                    intentRemove.putExtra("friend", clickedItem.getUsername());
                    startActivity(intentRemove);
                }
            }
        });

        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BanUserGroup.this, HomeActivity.class);
                startActivity(intent);
            }
        });

        btnOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BanUserGroup.this, optionActivity.class);
                startActivity(intent);
            }
        });

        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BanUserGroup.this, profile.class);
                startActivity(intent);
            }
        });
    }

    private void fetchMembers() {
        if (groupId == -1 || url.isEmpty() || myUsername.isEmpty()) {
            Log.e("BanUserGroup", "Parametros invalidos: url=" + url + ", myUsername=" + myUsername + ", groupId=" + groupId);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonPayload = new JSONObject();
                    jsonPayload.put("username", myUsername);
                    jsonPayload.put("group_id", groupId);

                    Log.d("BanUserGroup", "Enviando payload: " + jsonPayload.toString() + " para " + url + "/members");

                    final String response = request.requestHTTP(url + "/members", "post", jsonPayload, BanUserGroup.this);

                    Log.d("BanUserGroup", "Resposta do servidor: " + response);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response == null || response.trim().isEmpty()) {
                                Log.e("BanUserGroup", "Resposta do servidor veio nula ou vazia");
                                return;
                            }

                            try {
                                JSONArray membersArray = null;

                                // Trata caso o servidor retorne { "members": [...] } ou direto [...]
                                if (response.trim().startsWith("{")) {
                                    JSONObject jsonObj = new JSONObject(response);
                                    if (jsonObj.has("members")) {
                                        membersArray = jsonObj.getJSONArray("members");
                                    }
                                } else if (response.trim().startsWith("[")) {
                                    membersArray = new JSONArray(response);
                                }

                                if (membersArray != null) {
                                    friendsList.clear();
                                    for (int i = 0; i < membersArray.length(); i++) {
                                        // Suporta tanto lista de Strings quanto lista de Objetos JSON
                                        String memberName;
                                        if (membersArray.optJSONObject(i) != null) {
                                            memberName = membersArray.getJSONObject(i).getString("username");
                                        } else {
                                            memberName = membersArray.getString(i);
                                        }
                                        friendsList.add(new FriendItem(memberName));
                                    }
                                    adapter.notifyDataSetChanged();
                                    Log.d("BanUserGroup", "Membros carregados com sucesso: " + friendsList.size());
                                } else {
                                    Log.e("BanUserGroup", "Chave 'members' nao encontrada na resposta.");
                                }

                            } catch (JSONException e) {
                                Log.e("BanUserGroup", "Erro ao fazer parse do JSON: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    });

                } catch (JSONException e) {
                    Log.e("BanUserGroup", "Erro ao criar JSON de envio: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }
}