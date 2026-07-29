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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
public class addFriendActivity extends Activity{
    private EditText edtUsername;
    private EditText edtMessage;
    private Button btnSend;
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        edtUsername = (EditText) findViewById(R.id.edtUsername);
        edtMessage = (EditText) findViewById(R.id.edtMessage);
        btnSend = (Button) findViewById(R.id.btnSend);
        String username = "";
        String url = "";
        try{
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(addFriendActivity.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            username = fastLogin.getString("username").toString();
            url = server.getString("url").toString();
        }catch(JSONException e){
            e.printStackTrace();
        }
        btnSend.OnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String receiver = edtUsername.getText().toString();
                String message = edtMessage.getText().toString();
                try{
                    JSONObject jsonAdd;
                    jsonAdd.put("receiver", receiver);
                    jsonAdd.put("remittee", username);
                    jsonAdd.put("message", message);
                    request.requestHTTP(url + "/send-friend", "post", jsonAdd, addFriendActivity.this);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
}
