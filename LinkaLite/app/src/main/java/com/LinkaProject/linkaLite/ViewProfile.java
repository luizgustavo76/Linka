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
public class ViewProfile extends Activity{
    private ImageView imgProfilePicture;
    private String username = "";
    private String url = "";
    private String biography = "";
    private TextView txtUsername;
    private TextView txtBio;
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_profile);
        try{
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(ViewProfile.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            username = fastLogin.getString("username").toString();
        }catch(JSONException e){
            e.printStackTrace();
        }
        Intent intent = getIntent();
        String usernameProfile = intent.getStringExtra("usernameProfile");
        try{
            JSONObject jsonProfile = new JSONObject();
            JSONObject response = new JSONObject(request.requestHTTP(url + "/view-profile/" + usernameProfile, "get", jsonProfile, ViewProfile.this));
            biography = response.optString("bio", "");
        }catch(Exception e){
            e.printStackTrace();
        }
        imgProfilePicture = (ImageView) findViewById(R.id.imgProfilePicture);
        txtUsername = (TextView) findViewById(R.id.txtUsername);
        txtBio = (TextView) findViewById(R.id.txtBio);
        txtUsername.setText(usernameProfile);
        txtBio.setText(biography);
        ImageLoader imageLoader = new ImageLoader();
        imageLoader.viewProfilePicture(ViewProfile.this, usernameProfile, imgProfilePicture);
    }
}