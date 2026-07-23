package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
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
public class HomeActivity extends Activity {

    private ImageButton btnHome;
    private ImageButton btnProfile;
    private ImageButton btnOptions;
    private ImageButton btnChat;
    private Button newPost;
    private ListView listViewPosts;
    private PostAdapter postAdapter;
    private ArrayList<JSONObject> postsList;
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    Runnable tokenTask = new Runnable() {
        @Override
        public void run() {
            try{
                config cfg = new config();
                JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(HomeActivity.this, "config.cfg"));
                JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
                JSONObject server = jsonCfg.getJSONObject("SERVER");
                String token = fastLogin.getString("token").toString();
                String url = server.getString("url").toString();
                String token_response = tokenManager.valideToken(url, token, HomeActivity.this);
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

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

        newPost = (Button) findViewById(R.id.newPost);
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);

        newPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, newPost.class);
                startActivity(intent);
            }
        });

        // Configura a ListView do Feed
        listViewPosts = (ListView) findViewById(R.id.listViewPosts);
        postsList = new ArrayList<JSONObject>();
        postAdapter = new PostAdapter(this, postsList);
        listViewPosts.setAdapter(postAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new FetchFeedTask().execute("http://linkaProject.pythonanywhere.com/feed");
    }

    // AsyncTask para rodar requisição de rede fora da Thread de UI
    private class FetchFeedTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return requestHTTP(urls[0], "GET", new JSONObject());
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null || result.trim().isEmpty()) {
                Toast.makeText(HomeActivity.this, "Erro in feed loading", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONArray jsonArray = new JSONArray(result);
                postsList.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                    postsList.add(jsonArray.getJSONObject(i));
                }

                // Notifica o Adapter para atualizar a interface
                postAdapter.notifyDataSetChanged();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(HomeActivity.this, "Error in parsing posts", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Adapter do Feed
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
                config cfg = new config();
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
                                textPost.replace(line, "");
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