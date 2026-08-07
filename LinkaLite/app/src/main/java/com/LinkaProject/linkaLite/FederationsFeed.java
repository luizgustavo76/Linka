package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FederationsFeed extends Activity {

    private ImageButton btnHome;
    private ImageButton btnProfile;
    private ImageButton btnOptions;
    private ImageButton btnChat;
    private Button newPost;
    private ListView listViewPosts;
    private PostAdapter postAdapter;
    private ArrayList<JSONObject> postsList;
    private String currentUrl = "";
    private ScheduledExecutorService scheduler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("url")) {
            currentUrl = intent.getStringExtra("url");
            if (currentUrl != null) {
                if (!currentUrl.startsWith("http://") && !currentUrl.startsWith("https://")) {
                    currentUrl = "http://" + currentUrl;
                }
                if (currentUrl.endsWith("/")) {
                    currentUrl = currentUrl.substring(0, currentUrl.length() - 1);
                }
            }
        }


        try {   
            config cfg = new config();
            String rawJson = cfg.loadCfgAsJson(this, "config.cfg");
            JSONObject jsonCfg = new JSONObject(rawJson);
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            
            if (currentUrl.isEmpty()) {
                currentUrl = server.optString("url", "");
            }
            
            String token = fastLogin.optString("token_session", "");
            if (!token.isEmpty()) {
                tokenManager.valideToken(token, currentUrl, FederationsFeed.this);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        newPost = (Button) findViewById(R.id.newPost);
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);

        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chatIntent = new Intent(FederationsFeed.this, chatActivity.class);
                startActivity(chatIntent);
            }
        });

        btnOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent optionsIntent = new Intent(FederationsFeed.this, optionActivity.class);
                startActivity(optionsIntent);
            }
        });

        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent profileIntent = new Intent(FederationsFeed.this, profile.class);
                startActivity(profileIntent);
            }
        });

        newPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent newPostIntent = new Intent(FederationsFeed.this, newPost.class);
                startActivity(newPostIntent);
            }
        });

        listViewPosts = (ListView) findViewById(R.id.listViewPosts);
        postsList = new ArrayList<JSONObject>();
        postAdapter = new PostAdapter(this, postsList);
        listViewPosts.setAdapter(postAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUrl == null || currentUrl.isEmpty()) {
            try {
                config cfg = new config();
                JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(FederationsFeed.this, "config.cfg"));
                JSONObject server = jsonCfg.getJSONObject("SERVER");
                currentUrl = server.getString("url");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        new FetchFeedTask().execute(currentUrl);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    private class FetchFeedTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return requestHTTP(urls[0], "GET", new JSONObject());
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null || result.trim().isEmpty()) {
                Toast.makeText(FederationsFeed.this, "Erro in feed loading", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONArray jsonArray = new JSONArray(result);
                postsList.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                    postsList.add(jsonArray.getJSONObject(i));
                }

                postAdapter.notifyDataSetChanged();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(FederationsFeed.this, "Error in parsing posts", Toast.LENGTH_SHORT).show();
            }
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
                final String id = post.optString("id", "");
                String stars = post.optString("stars", "0");

                btnComments.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent commentIntent = new Intent(FederationsFeed.this, comments_activity.class);
                        commentIntent.putExtra("post_id", id);
                        startActivity(commentIntent);
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
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlParam);
            connection = (HttpURLConnection) url.openConnection();
            
            method = method.toUpperCase();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            connection.setRequestProperty("bypass-tunnel-remainder", "true");
            
            if (method.equals("POST") || method.equals("PUT")) {
                connection.setDoOutput(true);
                OutputStream os = connection.getOutputStream();
                os.write(json_body.toString().getBytes("UTF-8"));
                os.flush();
                os.close();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                return response.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return ""; 
    }
}