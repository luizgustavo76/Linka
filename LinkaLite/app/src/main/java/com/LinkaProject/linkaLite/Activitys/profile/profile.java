package com.LinkaProject.linkaLite;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class profile extends BaseActivity {
    private ImageView imgProfilePicture;
    private String username = "";
    private String url = "";
    private String biography = "";
    private TextView txtUsername;
    private TextView txtBio;
    private LinearLayout postsContainer;
    private Button btnEdit;
    private Button btnExit;
    private String usernameProfile = "";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_profile);

        TextView backButton = (TextView) findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(profile.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            usernameProfile = fastLogin.getString("username").toString();
            url = server.optString("url", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            JSONObject jsonProfile = new JSONObject();
            JSONObject response = new JSONObject(request.requestHTTP(url + "/view_profile/" + usernameProfile, "get", jsonProfile, profile.this));
            biography = response.optString("bio", "");
        } catch (Exception e) {
            e.printStackTrace();
        }

        imgProfilePicture = (ImageView) findViewById(R.id.imgProfilePicture);
        txtUsername = (TextView) findViewById(R.id.txtUsername);
        txtBio = (TextView) findViewById(R.id.txtBio);
        postsContainer = (LinearLayout) findViewById(R.id.postsContainer);
        btnExit = (Button) findViewById(R.id.btnExit);
        txtUsername.setText(usernameProfile);
        txtBio.setText(biography);
        btnEdit = (Button) findViewById(R.id.btnEdit);
        final TextView btnReadMore = (TextView) findViewById(R.id.btnReadMore);

        txtBio.post(new Runnable() {
            @Override
            public void run() {
                int lineCount = txtBio.getLineCount();
                if (lineCount >= 5) {
                    btnReadMore.setVisibility(View.VISIBLE);
                } else {
                    btnReadMore.setVisibility(View.GONE);
                }
            }
        });

        btnReadMore.setOnClickListener(new View.OnClickListener() {
            boolean isExpanded = false;
            @Override
            public void onClick(View v) {
                if (isExpanded) {
                    txtBio.setMaxLines(2);
                    btnReadMore.setText("Read More...");
                    isExpanded = false;
                } else {
                    txtBio.setMaxLines(Integer.MAX_VALUE);
                    btnReadMore.setText("Show Less");
                    isExpanded = true;
                }
            }
        });

        ImageLoader imageLoader = new ImageLoader();
        imageLoader.viewProfilePicture(profile.this, usernameProfile, imgProfilePicture);

        imgProfilePicture.post(new Runnable() {
            @Override
            public void run() {
                Drawable drawable = imgProfilePicture.getDrawable();
                if (drawable instanceof BitmapDrawable) {
                    Bitmap originalBitmap = ((BitmapDrawable) drawable).getBitmap();
                    Bitmap circularBitmap = getCircularBitmap(originalBitmap);
                    if (circularBitmap != null) {
                        imgProfilePicture.setImageBitmap(circularBitmap);
                    }
                }
            }
        });

        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                config cfg = new config();
                cfg.updateCfg(profile.this, "config.cfg", "FAST_LOGIN", "username", "");
                cfg.updateCfg(profile.this, "config.cfg", "FAST_LOGIN", "password", "");
                Intent intent = new Intent(profile.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        imgProfilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(profile.this, ViewPicture.class);
                intent.putExtra("type", "profile picture");
                intent.putExtra("username", usernameProfile);
                startActivity(intent);
            }
        });

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(profile.this, EditAccount.class);
                startActivity(intent);
            }
        });

        loadPosts(usernameProfile);
    }

    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = Math.min(width, height);

        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(output);

        final android.graphics.Paint paint = new android.graphics.Paint();
        final android.graphics.Rect rect = new android.graphics.Rect((width - size) / 2, (height - size) / 2, (width + size) / 2, (height + size) / 2);
        final android.graphics.Rect targetRect = new android.graphics.Rect(0, 0, size, size);
        final android.graphics.RectF rectF = new android.graphics.RectF(targetRect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(0xFF424242);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, targetRect, paint);

        return output;
    }

    private void loadPosts(String usernameProfile) {
        try {
            JSONObject jsonReq = new JSONObject();
            jsonReq.put("username", usernameProfile);

            String responseStr = request.requestHTTP(url + "/view-profile-posts", "post", jsonReq, profile.this);
            JSONArray postsArray = new JSONArray(responseStr);

            postsContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(profile.this);
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

                imageLoader.viewProfilePicture(profile.this, usernameProfile, postAvatar);

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
                            Intent intent = new Intent(profile.this, ViewPicture.class);
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
                            Intent intent = new Intent(profile.this, comments_activity.class);
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