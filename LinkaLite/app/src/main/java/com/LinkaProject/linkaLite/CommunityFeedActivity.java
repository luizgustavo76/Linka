package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.widget.ImageButton;
public class CommunityFeedActivity extends Activity {

    private LinearLayout layoutFeedContainer;
    private String communityId = "";
    private String serverUrl = "http://patient-hall-2915.luizsgustavo76.workers.dev/";
    private ImageLoader imageLoader;
    private String iconUrl = "";
    private String bannerUrl = "";
    private ImageView imgBanner;
    private ImageView imgCommunityAvatar;
    private TextView txtDescription;
    private String description = "";
    private ImageButton newPost;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_feed);
        newPost = (ImageButton) findViewById(R.id.newPost);
        layoutFeedContainer = (LinearLayout) findViewById(R.id.layoutFeedContainer);
        imageLoader = new ImageLoader();
        imgBanner = (ImageView) findViewById(R.id.imgBanner);
        imgCommunityAvatar = (ImageView) findViewById(R.id.imgCommunityAvatar);
        txtDescription = (TextView) findViewById(R.id.txtDescription);
        newPost.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent(CommunityFeedActivity.this, NewPostCommunity.class);
                startActivity(intent);
            }
        });
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("community_id")) {
            communityId = intent.getStringExtra("community_id");
            iconUrl = intent.getStringExtra("icon_url");
            bannerUrl = intent.getStringExtra("banner_url");
            description = intent.getStringExtra("description");
        }
        txtDescription.setText(description);
        imageLoader.LoadImageUrl(iconUrl, imgCommunityAvatar);
        imageLoader.LoadImageUrl(bannerUrl, imgBanner);
        try {
            config cfg = new config();
            String rawCfg = cfg.loadCfgAsJson(this, "config.cfg");
            if (rawCfg != null && rawCfg.length() > 0) {
                JSONObject jsonCfg = new JSONObject(rawCfg);
                JSONObject server = jsonCfg.optJSONObject("SERVER");
                if (server != null) {
                    String cfgUrl = server.optString("url", "").trim();
                    if (cfgUrl.length() > 0) {
                        serverUrl = cfgUrl;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        new FetchCommunityPostsTask().execute();
    }

    private class FetchCommunityPostsTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                String baseUrl = serverUrl.trim();
                while (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                String targetUrl = baseUrl + "/view-posts";

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("community_id", communityId);

                return request.requestHTTP(targetUrl, "POST", jsonParam, 0, CommunityFeedActivity.this);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (isFinishing()) return;

            if (layoutFeedContainer != null) {
                layoutFeedContainer.removeAllViews();
            }

            if (result != null && result.trim().length() > 0) {
                try {
                    String trimmed = result.trim();
                    if (trimmed.startsWith("[")) {
                        JSONArray jsonArray = new JSONArray(trimmed);
                        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject post = jsonArray.getJSONObject(i);

                            View postView = inflater.inflate(R.layout.item_post, layoutFeedContainer, false);

                            ImageView avatarPost = (ImageView) postView.findViewById(R.id.postAvatar);
                            TextView tvUsername = (TextView) postView.findViewById(R.id.postUsername);
                            TextView tvText = (TextView) postView.findViewById(R.id.postText);
                            TextView tvDate = (TextView) postView.findViewById(R.id.postDate);

                            String username = post.optString("username", post.optString("user", "entity404"));
                            String textPost = post.optString("text_post", post.optString("text", ""));
                            String datetime = post.optString("datetime", post.optString("date", ""));

                            if (tvUsername != null) {
                                tvUsername.setText(username.startsWith("@") ? username : "@" + username);
                            }
                            if (tvText != null) {
                                tvText.setText(textPost);
                            }
                            if (tvDate != null) {
                                tvDate.setText(datetime);
                            }

                            if (avatarPost != null) {
                                imageLoader.viewProfilePicture(CommunityFeedActivity.this, username, avatarPost);
                            }

                            layoutFeedContainer.addView(postView);
                        }
                    } else if (trimmed.startsWith("{")) {
                        JSONObject errObj = new JSONObject(trimmed);
                        String msg = errObj.optString("error", errObj.optString("status", "Error loading community posts"));
                        Toast.makeText(CommunityFeedActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(CommunityFeedActivity.this, "Failed to load community feed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}