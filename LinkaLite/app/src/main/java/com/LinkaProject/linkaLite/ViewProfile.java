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

public class ViewProfile extends Activity {
    private ImageView imgProfilePicture;
    private String username = "";
    private String url = "";
    private String biography = "";
    private boolean isFriend = false;
    private TextView txtUsername;
    private TextView txtBio;
    private LinearLayout postsContainer;
    private ToggleButton btnToggleFriend;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_profile);

        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(ViewProfile.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            username = fastLogin.getString("username").toString();
            url = server.optString("url", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final String usernameProfile = getIntent().getStringExtra("usernameProfile");

        try {
            JSONObject jsonProfile = new JSONObject();
            JSONObject response = new JSONObject(request.requestHTTP(url + "/view_profile/" + usernameProfile, "get", jsonProfile, ViewProfile.this));
            biography = response.optString("bio", "");
            isFriend = response.optBoolean("is_friend", false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        imgProfilePicture = (ImageView) findViewById(R.id.imgProfilePicture);
        txtUsername = (TextView) findViewById(R.id.txtUsername);
        txtBio = (TextView) findViewById(R.id.txtBio);
        postsContainer = (LinearLayout) findViewById(R.id.postsContainer);
        btnToggleFriend = (ToggleButton) findViewById(R.id.btnFriendToggle);

        txtUsername.setText(usernameProfile);
        txtBio.setText(biography);

        btnToggleFriend.setChecked(isFriend);

        ImageLoader imageLoader = new ImageLoader();
        imageLoader.viewProfilePicture(ViewProfile.this, usernameProfile, imgProfilePicture);

        btnToggleFriend.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    JSONObject jsonToggle = new JSONObject();
                    jsonToggle.put("sender", username);
                    jsonToggle.put("receiver", usernameProfile);
                    request.requestHTTP(url + "/toggle-friend", "post", jsonToggle, ViewProfile.this);
                } catch (JSONException e) {
                    e.printStackTrace();
                }            
            }
        });

        loadPosts(usernameProfile);
    }

    private void loadPosts(String usernameProfile) {
        try {
            JSONObject jsonReq = new JSONObject();
            jsonReq.put("username", usernameProfile);

            String responseStr = request.requestHTTP(url + "/view-profile-posts", "post", jsonReq, ViewProfile.this);
            JSONArray postsArray = new JSONArray(responseStr);

            postsContainer.removeAllViews();

            for (int i = 0; i < postsArray.length(); i++) {
                JSONObject post = postsArray.getJSONObject(i);
                String textPost = post.optString("text_post", "");
                String datetime = post.optString("datetime", "");

                LinearLayout postCard = new LinearLayout(ViewProfile.this);
                postCard.setOrientation(LinearLayout.VERTICAL);

                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                cardParams.setMargins(0, 0, 0, 24);
                postCard.setLayoutParams(cardParams);
                postCard.setPadding(16, 16, 16, 16);

                TextView txtDate = new TextView(ViewProfile.this);
                txtDate.setText(datetime);
                txtDate.setTextColor(0x88FFFFFF);
                txtDate.setTextSize(12);

                TextView txtContent = new TextView(ViewProfile.this);
                txtContent.setText(textPost);
                txtContent.setTextColor(0xFFFFFFFF);
                txtContent.setTextSize(14);
                txtContent.setPadding(0, 8, 0, 0);

                postCard.addView(txtDate);
                postCard.addView(txtContent);

                postsContainer.addView(postCard);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}