package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
public class TimelineThemes extends Activity{
    private String url = "";
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.id.themes_timeline);
        try{
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(TimelineThemes.this, "config.cfg"));
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            url = server.optString("url", "";)
        }catch(JSONException e){
            e.printStackTrace();
        }
        try{
            JSONObject jsonIndex = new JSONObject(request.requestHTTP())
        }
    }
}