package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

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
        listInvite = (ListView) findViewById(R.id.listInvite); // ID alinhado com o XML

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

        carregarConvites();
    }

    private void carregarConvites() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Declaração do jsonInvite DENTRO da Thread onde ele é usado
                    JSONObject jsonInvite = new JSONObject();
                    jsonInvite.put("username", username);

                    // A variavel jsonInvite é visivel aqui no mesmo escopo
                    String response = request.requestHTTP(url + "/view-invites", "post", jsonInvite, GenerateInvite.this);

                    if (response != null && !response.trim().equals("")) {
                        final JSONObject jsonResponse = new JSONObject(response);
                        String status = jsonResponse.optString("status", "");

                        if ("success".equals(status)) {
                            JSONArray invitesArray = jsonResponse.optJSONArray("invites");
                            inviteList.clear();

                            if (invitesArray != null) {
                                for (int i = 0; i < invitesArray.length(); i++) {
                                    JSONObject item = invitesArray.optJSONObject(i);
                                    if (item != null) {
                                        String code = item.optString("code", item.optString("invite_code", ""));
                                        if (!code.equals("")) {
                                            inviteList.add(code);
                                        }
                                    }
                                }
                            }

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
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