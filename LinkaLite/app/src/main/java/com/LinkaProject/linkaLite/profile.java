package com.LinkaProject.linkaLite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class profile extends Activity{
    private ImageView imgProfilePicture;
    private TextView username;
    private Button btnEdit;
    private Button btnExit;
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_profile);
        String username = "";
        try{
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(profile.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            username = fastLogin.getString("username").toString();
        }catch(JSONException e){
            e.printStackTrace();
        }
        btnEdit = (Button) findViewById(R.id.btnEdit);
        btnExit = (Button) findViewById(R.id.btnExit);
        ImageView imgProfile = (ImageView) findViewById(R.id.imgProfilePicture);
        ImageLoader loader = new ImageLoader();
        loader.viewProfilePicture(profile.this, username, imgProfile);
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    config cfg = new config();
                    cfg.deleteFileLinka(profile.this, "config.cfg");
                    cfg.createDefaultConfig(profile.this, "config.cfg");
                    Intent intent = new Intent(profile.this, LoginActivity.class);
                    startActivity(intent);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
}
