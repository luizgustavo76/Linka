package com.LinkaProject.linkaLite;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
public class NewGroupActivity extends Activity{
    private ImageButton btnHome;
    private ImageButton btnChat;
    private ImageButton btnOptions;
    private ImageButton btnProfile;
    private EditText edtGroupName;
    private EditText edtDescription;
    private Button btnCreate;
    private String username = "";
    private String url = "";
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.activity_new_group);
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        edtGroupName = (EditText) findViewById(R.id.edtGroupName);
        edtDescription = (EditText) findViewById(R.id.edtDescription);
        btnCreate = (Button) findViewById(R.id.btnCreate);
        try{
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(NewGroupActivity.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.optJSONObject("FAST_LOGIN");
            JSONObject server  = jsonCfg.optJSONObject("SERVER");
            url = server.getString("url").toString();
            username = fastLogin.getString("username").toString();
        }catch(JSONException e){
            e.printStackTrace();
        }
        btnCreate.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                try{
                    JSONObject jsonGroup = new JSONObject();
                    String name_group = edtGroupName.getText().toString();
                    String description = edtDescription.getText().toString();
                    jsonGroup.put("username", username);
                    jsonGroup.put("name_group", name_group);
                    jsonGroup.put("description", description);
                    request.requestHTTP(url + "/create-group", "post", jsonGroup, NewGroupActivity.this);   
                }catch(JSONException e){
                    e.printStackTrace();
                }
            }
        });
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent intent = new Intent(NewGroupActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });
        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent intent = new Intent(NewGroupActivity.this, chatActivity.class);
                startActivity(intent);
            }
        });
        btnOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent intent = new Intent(NewGroupActivity.this, optionActivity.class);
                startActivity(intent);
            }
        });
        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent intent = new Intent(NewGroupActivity.this, profile.class);
                startActivity(intent);
            }
        });
    }
}
