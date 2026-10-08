package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class optionActivity extends Activity {

    private ListView optionsListView;
    private ImageButton btnHome;
    private ImageButton btnChat;
    private ImageButton btnProfile;
    private ImageButton btnOptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        optionsListView = (ListView) findViewById(R.id.optionsListView);
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);

        // Monta os itens do menu no formato Título + Subtítulo/Descrição
        List<Map<String, String>> data = new ArrayList<Map<String, String>>();
        
        addItem(data, "Generate a invite", "Create invitation codes for new users");
        addItem(data, "Edit timeline", "Customize feeds, federations and post sources");
        addItem(data, "Inbox", "View messages and notifications");
        addItem(data, "Friends", "Manage your contacts and federation friends");
        addItem(data, "Change server", "Switch or configure your current node server");

        // Utiliza o layout NATIVO do Android (simple_list_item_2) para renderizar 2 linhas
        SimpleAdapter adapter = new SimpleAdapter(
                this,
                data,
                android.R.layout.simple_list_item_2,
                new String[] {"title", "subtitle"},
                new int[] {android.R.id.text1, android.R.id.text2}
        );

        optionsListView.setAdapter(adapter);

        // Eventos de clique para a lista de opções
        optionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: // Generate a invite
                        startActivity(new Intent(optionActivity.this, GenerateInvite.class));
                        break;
                    case 1: // Edit timeline
                        startActivity(new Intent(optionActivity.this, MainEditTimeline.class));
                        break;
                    case 2: 
                        startActivity(new Intent(optionActivity.this, InboxActivity.class));
                        break;
                    case 3:
                        startActivity(new Intent(optionActivity.this, addFriendActivity.class));
                        break;
                    case 4: // Change server
                        startActivity(new Intent(optionActivity.this, ChangeServer.class));
                        break;
                }
            }
        });

        // Botões de Navegação Inferior
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(optionActivity.this, HomeActivity.class));
            }
        });

        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(optionActivity.this, chatActivity.class));
            }
        });

        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(optionActivity.this, profile.class));
            }
        });
    }

    private void addItem(List<Map<String, String>> list, String title, String subtitle) {
        Map<String, String> item = new HashMap<String, String>();
        item.put("title", title);
        item.put("subtitle", subtitle);
        list.add(item);
    }
}