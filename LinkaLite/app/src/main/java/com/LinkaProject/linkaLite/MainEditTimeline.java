package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.SimpleAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class MainEditTimeline extends Activity{
    private ListView optionsListView;
    private ImageButton btnHome;
    private ImageButton btnChat;
    private ImageButton btnOptions;
    private ImageButton btnProfile;
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_timeline);
        List<Map<String, String>> data = new ArrayList<Map<String, String>>();
        optionsListView = (ListView) findViewById(R.id.optionsListView);
        addItem(data, "Add new sources", "Add new post sources here.");
        addItem(data, "Filters", "Filter the hashtags you want to see—or not—and the order of your feed here.");
        addItem(data, "Manual", "Add the URL here manually..");        
        SimpleAdapter adapter = new SimpleAdapter(
                this,
                data,
                android.R.layout.simple_list_item_2,
                new String[] {"title", "subtitle"},
                new int[] {android.R.id.text1, android.R.id.text2}
        );
        optionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        startActivity(new Intent(MainEditTimeline.this, EditTimeline.class));
                    case 2:
                        startActivity(new Intent(MainEditTimeline.this, AddManualUrl.class));
                }
            }
        });
        optionsListView.setAdapter(adapter);
    }
    private void addItem(List<Map<String, String>> data, String title, String snippet) {
        Map<String, String> temp = new HashMap<String, String>();
        temp.put("title", title);
        temp.put("snippet", snippet);
        data.add(temp);
    }
}