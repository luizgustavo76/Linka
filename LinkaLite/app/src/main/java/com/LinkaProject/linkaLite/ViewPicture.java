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
public class ViewPicture extends Activity{
    private ImageView imageView;
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_picture);
        Intent intent = getIntent();
        String type = intent.getStringExtra("type");
        ImageLoader ImageClass = new ImageLoader();
        imageView = (ImageView) findViewById(R.id.imageView);
        if ("profile picture".equals(type)) {
            String username = intent.getStringExtra("username");
            ImageClass.viewProfilePicture(ViewPicture.this, username, imageView);
        }else{
            String url = intent.getStringExtra("url");
            ImageClass.LoadImageUrl(url, imageView);
        }
    }
}