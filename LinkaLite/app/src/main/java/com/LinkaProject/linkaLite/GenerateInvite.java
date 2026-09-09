package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.CompoundButton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.widget.ListView;
public class GenerateInvite extends Activity{
    private ImageButton btnHome;
    private ImageButton btnProfile;
    private ImageButton btnOptions;
    private ImageButton btnChat;
    private Button btnGenerate;
    private String url = "";
    private String username = "";
    private ListView listInvite;
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.generate_invite);
        try{
            config cfg = new config();
            JSONObject jsonCfg = new jsonObject(cfg.loadCfgAsJson(GenerateInvite.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            username = fastLogin.optString("username", "");
            url = server.optString("url", "";)
        }catch(JSONException e){
            e.printStackTrace();
        }
        btnGenerate = (Button) findViewById(R.id.btnGenerate);
        listInvite = (ListView) findViewById(R.id.listInvite);
    }
}