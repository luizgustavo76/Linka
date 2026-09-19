package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HomeActivity extends Activity {
    private Button federationButton;
    private ImageButton btnHome;
    private ImageButton btnProfile;
    private ImageButton btnOptions;
    private ImageButton btnChat;
    private Button newPost;
    private ListView listViewPosts;
    private PostAdapter postAdapter;
    private ArrayList<JSONObject> postsList;

    private View footerContainer;
    private Button btnFooterFederations;

    private ScheduledExecutorService scheduler;
    private ScheduledExecutorService schedulerNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        schedulerNotifications = Executors.newSingleThreadScheduledExecutor();
        scheduler = Executors.newSingleThreadScheduledExecutor();

        Runnable notificationsTask = new Runnable() {
            @Override
            public void run() {
                notificationManager.createNotification(HomeActivity.this);
            }
        };

        Runnable tokenTask = new Runnable() {
            @Override
            public void run() {
                try {
                    config cfg = new config();
                    JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(HomeActivity.this, "config.cfg"));
                    JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
                    JSONObject server = jsonCfg.getJSONObject("SERVER");
                    String token = fastLogin.getString("token_session");
                    String url = server.getString("url");
                    tokenManager.valideToken(token, url, HomeActivity.this);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        schedulerNotifications.scheduleAtFixedRate(notificationsTask, 0, 15, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(tokenTask, 0, 2, TimeUnit.MINUTES);

        config cfg = new config();
        try {   
            String rawJson = cfg.loadCfgAsJson(this, "config.cfg");
            JSONObject jsonCfg = new JSONObject(rawJson);
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            String url = server.optString("url", "http://linkaProject.pythonanywhere.com");
            String token = fastLogin.optString("token_session", "");
            if (!token.isEmpty()) {
                tokenManager.valideToken(token, url, HomeActivity.this);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        federationButton = (Button) findViewById(R.id.federationsButton);
        newPost = (Button) findViewById(R.id.newPost);
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);

        View.OnClickListener goFederationsListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, feedFinder.class);
                startActivity(intent);
            }
        };

        federationButton.setOnClickListener(goFederationsListener);

        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, chatActivity.class);
                startActivity(intent);
            }
        });

        btnOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, optionActivity.class);
                startActivity(intent);
            }
        });

        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, profile.class);
                startActivity(intent);
            }
        });

        newPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, newPost.class);
                startActivity(intent);
            }
        });

        listViewPosts = (ListView) findViewById(R.id.listViewPosts);

        // --- INFLA E ADICIONA O FOOTER NA LISTA (DEVE SER ANTES DO setAdapter) ---
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        footerContainer = inflater.inflate(R.layout.footer_federations, null);
        btnFooterFederations = (Button) footerContainer.findViewById(R.id.btnFooterFederations);
        btnFooterFederations.setOnClickListener(goFederationsListener);

        listViewPosts.addFooterView(footerContainer);

        postsList = new ArrayList<JSONObject>();
        postAdapter = new PostAdapter(this, postsList);
        listViewPosts.setAdapter(postAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String url = "";
        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(HomeActivity.this, "config.cfg"));
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            url = server.getString("url");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        new FetchFeedTask().execute(url + "/feed");
    }

    private class FetchFeedTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return requestHTTP(urls[0], "GET", new JSONObject());
        }

        @Override
        protected void onPostExecute(String result) {
            postsList.clear();

            if (result == null || result.trim().isEmpty()) {
                postAdapter.notifyDataSetChanged();
                Toast.makeText(HomeActivity.this, "Error in feed loading", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONArray jsonArray = new JSONArray(result);
                for (int i = 0; i < jsonArray.length(); i++) {
                    postsList.add(jsonArray.getJSONObject(i));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(HomeActivity.this, "Error in parsing posts", Toast.LENGTH_SHORT).show();
            }

            postAdapter.notifyDataSetChanged();
        }
    }

    private class PostAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<JSONObject> list;

        public PostAdapter(Context context, ArrayList<JSONObject> list) {
            this.context = context;
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.item_post, null);
            }

            ImageView avatarPost = (ImageView) convertView.findViewById(R.id.postAvatar);
            ImageView imgPost = (ImageView) convertView.findViewById(R.id.imgPost);
            TextView tvUsername = (TextView) convertView.findViewById(R.id.postUsername);

            tvUsername.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HomeActivity.this, ViewProfile.class);
                    JSONObject post = list.get(position);
                    String usernameProfile = post.optString("username", post.optString("user", "entity404"));
                    intent.putExtra("usernameProfile", usernameProfile);
                    startActivity(intent);
                }
            });

            TextView tvText = (TextView) convertView.findViewById(R.id.postText);
            TextView tvDate = (TextView) convertView.findViewById(R.id.postDate);
            TextView tvStarCount = (TextView) convertView.findViewById(R.id.starCount);
            Button btnComments = (Button) convertView.findViewById(R.id.btnComments);

            imgPost.setImageBitmap(null);
            imgPost.setVisibility(View.GONE);

            try {
                JSONObject post = list.get(position);
                String username = post.optString("username", post.optString("user", "entity404"));
                String textPost = post.optString("text_post", post.optString("text", ""));
                String datetime = post.optString("datetime", post.optString("date", ""));
                String id = post.optString("id", "");
                String stars = post.optString("stars", "0");

                btnComments.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(HomeActivity.this, comments_activity.class);
                        intent.putExtra("post_id", id);
                        startActivity(intent);
                    }
                });

                tvUsername.setText("@" + username);
                tvDate.setText(datetime);
                tvStarCount.setText(stars);

                ImageLoader imageLoader = new ImageLoader();
                imageLoader.viewProfilePicture(context, username, avatarPost);

                if (textPost.contains("[IMAGE]")) {
                    String[] lines = textPost.split("\n");
                    for (String line : lines) {
                        if (line.contains("[IMAGE]")) {
                            String newUrl = line.replace("[IMAGE]", "").trim();
                            if (!newUrl.isEmpty()) {
                                imgPost.setVisibility(View.VISIBLE);
                                String urlProxy = "http://linkaProject.pythonanywhere.com/lite-render?url=" + newUrl;

                                imgPost.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(HomeActivity.this, ViewPicture.class);
                                        intent.putExtra("type", "Image");
                                        intent.putExtra("url", urlProxy);
                                        startActivity(intent);
                                    }
                                });

                                new ImageLoader().LoadImageUrl(urlProxy, imgPost);
                                textPost = textPost.replace(line, "").trim();
                                break;
                            }
                        }
                    }
                }
                tvText.setText(textPost);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return convertView;
        }
    }

    public String requestHTTP(String urlParam, String method, JSONObject json_body) {
        return request.requestHTTP(urlParam, method, json_body, HomeActivity.this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        if (schedulerNotifications != null && !schedulerNotifications.isShutdown()) {
            schedulerNotifications.shutdownNow();
        }
    }
}