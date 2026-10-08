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
public class AddManualUrl extends Activity{
    private ImageButton btnHome;
    private ImageButton btnChat;
    private ImageButton btnOptions;
    private ImageButton btnProfile;
    private EditText edtUrl;
    private Button btnSend;
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_manual);
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        btnHome.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                startActivity(new Intent(AddManualUrl.this, HomeActivity.class));
            }
        });
        btnChat.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                startActivity(new Intent(AddManualUrl.this, chatActivity.class));
            }
        });
        btnOptions.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                startActivity(new Intent(AddManualUrl.this, optionActivity.class));
            }
        });
        btnProfile.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                startActivity(new Intent(AddManualUrl.this, profile.class));
            }
        });
    }   
}