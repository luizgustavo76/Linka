package com.LinkaProject.linkaLite;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ViewProfile extends BaseActivity {
    private ImageView imgProfilePicture;
    private View headerColor;
    private TextView backButton;
    private TextView txtHeaderTitle;
    private TextView txtUsername;
    private TextView txtBio;
    private TextView txtFollowersCount;
    private TextView txtFollowingCount;
    private LinearLayout postsContainer;
    private ToggleButton btnToggleFriend;
    private ToggleButton btnFollowToggle;

    private String username = "";
    private String url = "";
    private String biography = "";
    private String bannerColor = "#99D6FF";
    private boolean isFriend = false;
    private boolean isFollowing = false;
    private int followersCount = 0;
    private int followingCount = 0;
    private JSONArray followersArray = new JSONArray();
    private JSONArray followingArray = new JSONArray();

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
            bannerColor = response.optString("profile_banner_color", "#99D6FF");
            isFriend = response.optBoolean("is_friend", false);
            isFollowing = response.optBoolean("is_following", false);
            followersCount = response.optInt("followers_count", 0);
            followingCount = response.optInt("following_count", 0);
            if (response.has("followers")) {
                followersArray = response.getJSONArray("followers");
                for (int i = 0; i < followersArray.length(); i++) {
                    if (followersArray.optString(i, "").equalsIgnoreCase(username)) {
                        isFollowing = true;
                        break;
                    }
                }
            }
            if (response.has("following")) {
                followingArray = response.getJSONArray("following");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        backButton = (TextView) findViewById(R.id.backButton);
        txtHeaderTitle = (TextView) findViewById(R.id.txtHeaderTitle);
        headerColor = findViewById(R.id.headerColor);
        imgProfilePicture = (ImageView) findViewById(R.id.imgProfilePicture);
        txtUsername = (TextView) findViewById(R.id.txtUsername);
        txtBio = (TextView) findViewById(R.id.txtBio);
        txtFollowersCount = (TextView) findViewById(R.id.txtFollowersCount);
        txtFollowingCount = (TextView) findViewById(R.id.txtFollowingCount);
        postsContainer = (LinearLayout) findViewById(R.id.postsContainer);
        btnFollowToggle = (ToggleButton) findViewById(R.id.btnFollowToggle);
        btnToggleFriend = (ToggleButton) findViewById(R.id.btnFriendToggle);

        txtHeaderTitle.setText(usernameProfile);
        txtUsername.setText(usernameProfile);
        txtBio.setText(biography);
        updateCountersText();

        try {
            if (bannerColor != null && !bannerColor.startsWith("#")) {
                bannerColor = "#" + bannerColor;
            }
            headerColor.setBackgroundColor(Color.parseColor(bannerColor));
        } catch (Exception e) {
            headerColor.setBackgroundColor(Color.parseColor("#99D6FF"));
        }

        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        txtFollowersCount.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showUsersDialog("Followers", followersArray);
            }
        });

        txtFollowingCount.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showUsersDialog("Following", followingArray);
            }
        });

        btnFollowToggle.setChecked(isFollowing);
        if (isFollowing) {
            btnFollowToggle.setBackgroundColor(Color.parseColor("#E4E7EC"));
            btnFollowToggle.setTextColor(Color.parseColor("#2C333A"));
        } else {
            btnFollowToggle.setBackgroundColor(Color.parseColor("#00BCD4"));
            btnFollowToggle.setTextColor(Color.parseColor("#FFFFFF"));
        }

        btnToggleFriend.setChecked(isFriend);

        ImageLoader imageLoader = new ImageLoader();
        imageLoader.viewProfilePicture(ViewProfile.this, usernameProfile, imgProfilePicture);

        imgProfilePicture.post(new Runnable() {
            @Override
            public void run() {
                Drawable drawable = imgProfilePicture.getDrawable();
                if (drawable instanceof BitmapDrawable) {
                    Bitmap originalBitmap = ((BitmapDrawable) drawable).getBitmap();
                    Bitmap circularBitmap = profile.getCircularBitmap(originalBitmap);
                    if (circularBitmap != null) {
                        imgProfilePicture.setImageBitmap(circularBitmap);
                    }
                }
            }
        });

        imgProfilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewProfile.this, ViewPicture.class);
                intent.putExtra("type", "profile picture");
                intent.putExtra("username", usernameProfile);
                startActivity(intent);
            }
        });

        btnFollowToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    JSONObject jsonFollow = new JSONObject();
                    jsonFollow.put("username", username);
                    jsonFollow.put("follow_username", usernameProfile);
                    request.requestHTTP(url + "/follow", "post", jsonFollow, ViewProfile.this);

                    if (isChecked) {
                        followersCount++;
                        btnFollowToggle.setBackgroundColor(Color.parseColor("#E4E7EC"));
                        btnFollowToggle.setTextColor(Color.parseColor("#2C333A"));
                    } else {
                        if (followersCount > 0) followersCount--;
                        btnFollowToggle.setBackgroundColor(Color.parseColor("#00BCD4"));
                        btnFollowToggle.setTextColor(Color.parseColor("#FFFFFF"));
                    }
                    updateCountersText();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

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

    private void updateCountersText() {
        txtFollowersCount.setText(followersCount + " Followers");
        txtFollowingCount.setText(followingCount + " Following");
    }

    private void showUsersDialog(String title, JSONArray list) {
        String[] items = new String[list.length()];
        if (list.length() == 0) {
            items = new String[]{"No users yet"};
        } else {
            for (int i = 0; i < list.length(); i++) {
                items[i] = list.optString(i, "");
            }
        }
        new AlertDialog.Builder(ViewProfile.this)
                .setTitle(title)
                .setItems(items, null)
                .setPositiveButton("Close", null)
                .show();
    }

    private void loadPosts(String usernameProfile) {
        try {
            JSONObject jsonReq = new JSONObject();
            jsonReq.put("username", usernameProfile);

            String responseStr = request.requestHTTP(url + "/view-profile-posts", "post", jsonReq, ViewProfile.this);
            JSONArray postsArray = new JSONArray(responseStr);

            postsContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(ViewProfile.this);
            ImageLoader imageLoader = new ImageLoader();
            Pattern imgPattern = Pattern.compile("\\[IMAGE\\]\\s*(\\S+)", Pattern.CASE_INSENSITIVE);

            for (int i = 0; i < postsArray.length(); i++) {
                JSONObject post = postsArray.getJSONObject(i);
                final String postId = post.optString("id", post.optString("post_id", ""));
                String rawText = post.optString("text_post", "");
                String datetime = post.optString("datetime", "");
                String commentsCount = post.optString("comments_count", post.optString("comments", "0"));

                View itemView = inflater.inflate(R.layout.item_post, postsContainer, false);

                ImageView postAvatar = (ImageView) itemView.findViewById(R.id.postAvatar);
                TextView postUsername = (TextView) itemView.findViewById(R.id.postUsername);
                TextView postDate = (TextView) itemView.findViewById(R.id.postDate);
                TextView postText = (TextView) itemView.findViewById(R.id.postText);
                Button btnPostOptions = (Button) itemView.findViewById(R.id.btnPostOptions);
                FrameLayout containerMedia = (FrameLayout) itemView.findViewById(R.id.containerMedia);
                GifImageView imgPost = (GifImageView) itemView.findViewById(R.id.imgPost);
                TextView txtCommentCount = (TextView) itemView.findViewById(R.id.txtCommentCount);
                ImageButton btnComments = (ImageButton) itemView.findViewById(R.id.btnComments);

                postUsername.setText(usernameProfile);
                postDate.setText(datetime);
                txtCommentCount.setText(commentsCount);
                btnPostOptions.setVisibility(View.GONE);

                imageLoader.viewProfilePicture(ViewProfile.this, usernameProfile, postAvatar);

                String imageUrl = null;
                Matcher matcher = imgPattern.matcher(rawText);
                if (matcher.find()) {
                    imageUrl = matcher.group(1).trim();
                }

                String cleanText = rawText.replaceAll("(?i)\\[IMAGE\\]\\s*\\S+", "").trim();
                if (cleanText.length() > 0) {
                    postText.setText(cleanText);
                    postText.setVisibility(View.VISIBLE);
                } else {
                    postText.setVisibility(View.GONE);
                }

                if (imageUrl != null && imageUrl.length() > 0) {
                    containerMedia.setVisibility(View.VISIBLE);
                    imgPost.setVisibility(View.VISIBLE);
                    imageLoader.LoadImageUrl(imageUrl, imgPost);

                    final String finalImageUrl = imageUrl;
                    imgPost.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(ViewProfile.this, ViewPicture.class);
                            intent.putExtra("url", finalImageUrl);
                            startActivity(intent);
                        }
                    });
                } else {
                    containerMedia.setVisibility(View.GONE);
                }

                if (postId.length() > 0 && btnComments != null) {
                    btnComments.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(ViewProfile.this, comments_activity.class);
                            intent.putExtra("post_id", postId);
                            startActivity(intent);
                        }
                    });
                }

                postsContainer.addView(itemView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}