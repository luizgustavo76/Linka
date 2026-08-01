package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
public class feedFinder extends Activity{
    private Button btnNewer;
    private ImageButton btnHome;
    private ImageButton btnChat;
    private ImageButton btnOptions;
    private ImageButton btnProfile;
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_finder);
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent intent = new Intent(feedFinder.this, chatActivity.class);
                startActivity(intent);
            }
        });
        btnOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent intent = new Intent(feedFinder.this, optionActivity.class);
                startActivity(intent);
            }
        });
        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent intent = new Intent(feedFinder.this, profile.class);
                startActivity(intent);
            }
        });
    } 
}
