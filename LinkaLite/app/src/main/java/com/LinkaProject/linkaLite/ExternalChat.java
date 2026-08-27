package com.LinkaProject.linkaLite;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.content.Intent;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.widget.EditText;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
public class ExternalChat extends Activity{
    private String username = "";
    private String url = "";
    private ImageButton btnHome;
    private ImageButton btnChat;
    private ImageButton btnOptions;
    private ImageButton btnProfile;
    private EditText edtUrl;
    private EditText edtUsername;
    private Button btnAdd;
    public void onCreate(Bundle savedInstanceState){
        try{
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(ExternalChat.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            url = server.optString("url", "");
            username = fastLogin.optString("username", "");
        }catch(JSONException e){
            e.printStackTrace();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.external_chat);
        edtUrl = (EditText) findViewById(R.id.edtUrl);
        edtUsername = (EditText) findViewById(R.id.edtUsername);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        btnAdd.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                try{
                    JSONObject jsonAdd = new JSONObject();
                    jsonAdd.put("destiny", edtUsername.getText());
                    jsonAdd.put("urlDestiny", edtUrl.getText());
                    jsonAdd.put("username", username);
                    request.requestHTTP(url + "/add-external-contact", "post", jsonAdd, ExternalChat.this);
                }catch(Exception e){
                    e.printStackTrace();
                }
                Intent intent = new Intent(ExternalChat.this, DmExternalChat.class);
                intent.putExtra("destiny", edtUsername.getText());
                intent.putExtra("urlDestiny", edtUrl.getText());
                startActivity(intent);
            }
        });
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ExternalChat.this, HomeActivity.class);
                startActivity(intent);
            }
        });
        btnOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ExternalChat.this, optionActivity.class);
                startActivity(intent);
            }
        });
        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ExternalChat.this, profile.class);
                startActivity(intent);
            }
        });
        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ExternalChat.this, chatActivity.class);
                startActivity(intent);
            }
        });
    }
}