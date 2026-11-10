package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class optionActivity extends BaseActivity {

    private ListView optionsListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        optionsListView = (ListView) findViewById(R.id.optionsListView);
        List<Map<String, String>> data = new ArrayList<Map<String, String>>();
        
        addItem(data, "Edit timeline", "Customize feeds, federations and post sources");
        addItem(data, "Inbox", "View messages and notifications");
        addItem(data, "Friends", "Manage your contacts and federation friends");
        addItem(data, "Change server", "Switch or configure your current node server");
        addItem(data, "Themes", "Change your theme, Linka is colorful now");

        // Utiliza o layout customizado da v3.0 (R.layout.item_option)
        SimpleAdapter adapter = new SimpleAdapter(
                this,
                data,
                R.layout.item_option,
                new String[] {"title", "subtitle"},
                new int[] {R.id.txtOptionTitle, R.id.txtOptionSubtitle}
        );

        optionsListView.setAdapter(adapter);

        // Eventos de clique para a lista de opções
        optionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: 
                        startActivity(new Intent(optionActivity.this, MainEditTimeline.class));
                        break;
                    case 1: 
                        startActivity(new Intent(optionActivity.this, InboxActivity.class));
                        break;
                    case 2:
                        startActivity(new Intent(optionActivity.this, addFriendActivity.class));
                        break;
                    case 3: // Change server
                        startActivity(new Intent(optionActivity.this, ChangeServer.class));
                        break;
                    case 4:
                        startActivity(new Intent(optionActivity.this, ThemesViewer.class));
                        break;
                }
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