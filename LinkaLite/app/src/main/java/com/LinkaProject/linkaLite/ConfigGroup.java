package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
public class ConfigGroup extends Activity{
    private Button btnBanUser;
    private Button btnChangeName;
    private Button btnExit;
    private String username="";
    private String url="";
    private int groupId = -1;
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_group);
        btnBanUser = (Button) findViewById(R.id.btnBanUser);
        btnChangeName = (Button) findViewById(R.id.btnChangeName);
        btnExit = (Button) findViewById(R.id.btnExit);
        try{
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(ConfigGroup.this, "config.cfg"));
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST-LOGIN");
            url = server.optString("url", "");
            username = fastLogin.optString("username", "");
        }catch(JSONException e){
            e.printStackTrace();
        }

    }
}