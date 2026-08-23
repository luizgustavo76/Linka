package com.LinkaProject.linkaLite;

import com.LinkaProject.linkaLite.R;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
public class ChannelsGroupActivity extends Activity{
    private Button btnBack;
    private TextView textGroup;
    private Button btnAdd;
    private Button btnConfig;
    private ImageButton btnHome;
    private ImageButton btnChat;
    private ImageButton btnOptions;
    private ImageButton btnProfile;
    private String url="";
    private String username="";
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channels);
        try{
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(ChannelsGroupActivity, "config.cfg"));
            JSONOject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            url = server.getString("url").toString();
            username = fastLogin.getString("username").toString()
        }catch(JSONException e){
            e.printStackTrace();
        }
        btnBack = (Button) findViewById(R.id.btnBack);
        textGroup = (TextView) findViewById(R.id.textGroup);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        btnConfig = (Button) findViewById(R.id.btnConfig);
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        JSONObject jsonChannels;
        Intent intent = getIntent();
        int groupId = intent.getIntExtra("groupId", -1);
        jsonChannels.put("username", username);
        jsonChannels.put("group_id", groupId);
        String request.requestHTTP(url + "/view-channels", "post", jsonChannels);
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChannelsGroupActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });

        btnOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChannelsGroupActivity.this, optionActivity.class);
                startActivity(intent);
            }
        });

        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChannelsGroupActivity.this, profile.class);
                startActivity(intent);
            }
        });
        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChannelsGroupActivity.this, chatActivity.class);
                startActivity(intent);
            }
        });
    }
}