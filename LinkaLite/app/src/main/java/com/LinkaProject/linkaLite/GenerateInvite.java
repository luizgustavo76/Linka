package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GenerateInvite extends Activity {

    private ImageButton btnHome;
    private ImageButton btnProfile;
    private ImageButton btnOptions;
    private ImageButton btnChat;
    private Button btnGenerate;
    
    private String url = "";
    private String username = "";
    private ListView listInvite;
    
    private List<String> inviteList;
    private InviteAdapter adapter;
    private Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.generate_invite);

        handler = new Handler();
        inviteList = new ArrayList<String>();
        
        btnGenerate = (Button) findViewById(R.id.btnGenerate);
        listInvite = (ListView) findViewById(R.id.listInvite);

        adapter = new InviteAdapter(GenerateInvite.this, inviteList);
        listInvite.setAdapter(adapter);

        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(GenerateInvite.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.optJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.optJSONObject("SERVER");

            if (fastLogin != null) {
                username = fastLogin.optString("username", "");
            }
            if (server != null) {
                url = server.optString("url", "");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        btnGenerate.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (username.equals("") || url.equals("")) {
                    Toast.makeText(GenerateInvite.this, "Erro de configuração!", Toast.LENGTH_SHORT).show();
                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonNew = new JSONObject();
                            jsonNew.put("username", username);
                            
                            String resp = request.requestHTTP(url + "/create-invite", "post", jsonNew, GenerateInvite.this);
                            
                            if (resp != null && !resp.trim().equals("")) {
                                JSONObject jsonResp = new JSONObject(resp);
                                String status = jsonResp.optString("status", "");
                                
                                if ("invite created!".equals(status)) {
                                    carregarConvites();
                                } else {
                                    final String msg = jsonResp.optString("status", "Erro ao criar convite");
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(GenerateInvite.this, msg, Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        if (!username.equals("") && !url.equals("")) {
            carregarConvites();
        }
    }

    private void carregarConvites() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonInvite = new JSONObject();
                    jsonInvite.put("username", username);

                    String response = request.requestHTTP(url + "/view-invites", "post", jsonInvite, GenerateInvite.this);

                    if (response != null && !response.trim().equals("")) {
                        final JSONObject jsonResponse = new JSONObject(response);
                        String status = jsonResponse.optString("status", "");

                        if ("success".equals(status)) {
                            JSONArray invitesArray = jsonResponse.optJSONArray("invites");
                            
                            final List<String> novasList = new ArrayList<String>();

                            if (invitesArray != null) {
                                for (int i = 0; i < invitesArray.length(); i++) {
                                    JSONObject item = invitesArray.optJSONObject(i);
                                    if (item != null) {
                                        String code = item.optString("invite_code", item.optString("code", ""));
                                        if (!code.equals("")) {
                                            novasList.add(code);
                                        }
                                    }
                                }
                            }

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    inviteList.clear();
                                    inviteList.addAll(novasList);
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}