package com.LinkaProject.linkaLite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class profile {
    private ImageView imgProfilePicture;
    private TextView username;
    private Button btnEdit;
    private Button btnExit;
    public void onCreate(Bundle savedInstanceState){
        String username = "";
        try{
            config cfg = new config(profile.this);
            JSONObject jsonCfg = cfg.loadCfgAsJson("config.cfg");
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            username = fastLogin.getString("username").toString();
        }catch(JSONException e){
            e.printStackTrace();
        }
        btnEdit = (Button) findViewById(R.id.btnEdit);
        btnExit = (Button) findViewById(R.id.btnExit);
        ImageView imgProfile = (ImageView) findViewById(R.id.imgProfilePicture);
        ImageLoader.viewProfilePicture(profile.this, username, imgProfile);
        btnExit.OnClickListenernew(View.OnClickListener()) {
            @Override
            public void onClick(View v) {
                try{
                    config cfg = new config();
                    cfg.deleteFileLinka();
                    cfg.createDefaultConfig();
                    Intent intent = new Intent(profile.this, LoginActivity.class);
                    startActivity(intent);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
