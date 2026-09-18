package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FederationsFeed extends Activity {

    private ImageButton btnHome;
    private ImageButton btnProfile;
    private ImageButton btnOptions;
    private ImageButton btnChat;
    private Button newPost;

    private ListView listViewPosts;
    private PostAdapter postAdapter;
    private ArrayList<JSONObject> postsList;

    private String username = "";
    private String currentUrl = "";
    private String baseUrl = "";

    /*
     * Aceita os dois formatos que o Linka pode receber:
     *
     * [IMAGE]https://site.com/imagem.jpg
     * [IMAGE](https://site.com/imagem.jpg)
     *
     * O formato sem parenteses e o que aparece no feed atual do projeto.
     */
    private static final Pattern IMAGE_PATTERN = Pattern.compile(
            "\\[IMAGE\\]\\s*\\(?((?:https?://)[^\\s\\)]+)\\)?",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        AppConfig config = new AppConfig(this);
        username = config.getUsername();

        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(
                    cfg.loadCfgAsJson(
                            FederationsFeed.this,
                            "config.cfg"
                    )
            );

            JSONObject server = null;

            if (jsonCfg.has("SERVER")) {
                server = jsonCfg.getJSONObject("SERVER");
            } else if (jsonCfg.has("server")) {
                server = jsonCfg.getJSONObject("server");
            }

            if (server != null) {
                baseUrl = server.optString("url", "");
            }
        } catch (JSONException e) {
            Log.e(
                    "LINKA_FEED",
                    "Error parsing config.cfg: " + e.getMessage()
            );
        }

        Intent intent = getIntent();

        if (intent != null
                && intent.hasExtra("url")
                && intent.getStringExtra("url") != null
                && !intent.getStringExtra("url").isEmpty()) {

            currentUrl = intent.getStringExtra("url");
        } else {
            currentUrl = config.getUrl();
        }

        currentUrl = formatUrl(currentUrl);

        newPost = (Button) findViewById(R.id.newPost);
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);

        btnChat.setOnClickListener(
                v -> startActivity(
                        new Intent(
                                FederationsFeed.this,
                                chatActivity.class
                        )
                )
        );

        btnOptions.setOnClickListener(
                v -> startActivity(
                        new Intent(
                                FederationsFeed.this,
                                optionActivity.class
                        )
                )
        );

        btnProfile.setOnClickListener(
                v -> startActivity(
                        new Intent(
                                FederationsFeed.this,
                                profile.class
                        )
                )
        );

        newPost.setOnClickListener(
                v -> startActivity(
                        new Intent(
                                FederationsFeed.this,
                                newPost.class
                        )
                )
        );

        listViewPosts = (ListView) findViewById(R.id.listViewPosts);

        postsList = new ArrayList<JSONObject>();
        postAdapter = new PostAdapter(this, postsList);
        listViewPosts.setAdapter(postAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (currentUrl == null || currentUrl.isEmpty()) {
            AppConfig config = new AppConfig(this);
            currentUrl = formatUrl(config.getUrl());
        }

        Log.d(
                "LINKA_FEED",
                "Executing FetchFeedTask for: " + currentUrl
        );

        new FetchFeedTask().execute(currentUrl);
    }

    private String formatUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return "";
        }

        String formatted = rawUrl.trim();

        if (!formatted.startsWith("http://")
                && !formatted.startsWith("https://")) {
            formatted = "http://" + formatted;
        }

        while (formatted.endsWith("/")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }

        return formatted;
    }

    private String sanitizarUrlImagem(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return "";
        }

        return rawUrl.replace("&amp;", "&").trim();
    }

    /**
     * Extrai a primeira imagem marcada no texto do post.
     */
    private String extractImageUrl(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Matcher matcher = IMAGE_PATTERN.matcher(text);

        if (!matcher.find()) {
            return "";
        }

        return sanitizarUrlImagem(matcher.group(1));
    }

    /**
     * Remove todas as marcacoes [IMAGE]URL do texto que sera exibido.
     */
    private String removeImageMarkers(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return IMAGE_PATTERN
                .matcher(text)
                .replaceAll("")
                .trim();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private class FetchFeedTask extends AsyncTask<String, Void, String> {

        private String requestedUrl;

        @Override
        protected String doInBackground(String... urls) {
            requestedUrl = urls[0];

            Log.d(
                    "LINKA_FEED",
                    "Requesting feed at URL: " + requestedUrl
            );

            return requestHTTP(
                    requestedUrl,
                    "GET",
                    new JSONObject()
            );
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null || result.trim().length() == 0) {
                Toast.makeText(
                        FederationsFeed.this,
                        "Error: Server did not respond",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            String trimmed = result.trim();

            if (trimmed.startsWith("<")) {
                Toast.makeText(
                        FederationsFeed.this,
                        "Error: Server returned HTML",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            try {
                postsList.clear();

                JSONArray jsonArray = null;

                if (trimmed.startsWith("[")) {
                    jsonArray = new JSONArray(trimmed);
                } else if (trimmed.startsWith("{")) {
                    JSONObject jsonObject = new JSONObject(trimmed);

                    if (jsonObject.has("posts")) {
                        jsonArray = jsonObject.getJSONArray("posts");
                    } else if (jsonObject.has("feed")) {
                        jsonArray = jsonObject.getJSONArray("feed");
                    } else if (jsonObject.has("data")) {
                        jsonArray = jsonObject.getJSONArray("data");
                    }
                }

                if (jsonArray != null) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        if (jsonArray.opt(i) instanceof JSONObject) {
                            postsList.add(jsonArray.getJSONObject(i));
                        }
                    }

                    postAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(
                            FederationsFeed.this,
                            "Error: Incompatible JSON structure",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            } catch (JSONException e) {
                Log.e(
                        "LINKA_FEED",
                        "JSONException while processing posts: "
                                + e.getMessage()
                );
            }
        }
    }

    private class PostAdapter extends BaseAdapter {

        private final Context context;
        private final ArrayList<JSONObject> list;

        public PostAdapter(
                Context context,
                ArrayList<JSONObject> list
        ) {
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
            JSONObject post = list.get(position);
            return post.optLong("id", position);
        }

        private class ViewHolder {
            ImageView avatarPost;
            ImageView imgPost;
            TextView tvUsername;
            TextView tvText;
            TextView tvDate;
            TextView tvStarCount;
            Button btnComments;
        }

        @Override
        public View getView(
                int position,
                View convertView,
                ViewGroup parent
        ) {
            ViewHolder holder;

            if (convertView == null) {
                LayoutInflater inflater =
                        (LayoutInflater) context.getSystemService(
                                Context.LAYOUT_INFLATER_SERVICE
                        );

                convertView = inflater.inflate(
                        R.layout.item_post,
                        parent,
                        false
                );

                holder = new ViewHolder();

                holder.avatarPost =
                        (ImageView) convertView.findViewById(
                                R.id.postAvatar
                        );

                holder.imgPost =
                        (ImageView) convertView.findViewById(
                                R.id.imgPost
                        );

                holder.tvUsername =
                        (TextView) convertView.findViewById(
                                R.id.postUsername
                        );

                holder.tvText =
                        (TextView) convertView.findViewById(
                                R.id.postText
                        );

                holder.tvDate =
                        (TextView) convertView.findViewById(
                                R.id.postDate
                        );

                holder.tvStarCount =
                        (TextView) convertView.findViewById(
                                R.id.starCount
                        );

                holder.btnComments =
                        (Button) convertView.findViewById(
                                R.id.btnComments
                        );

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // Limpa completamente o estado de uma View reciclada.
            holder.imgPost.setImageDrawable(null);
            holder.imgPost.setVisibility(View.GONE);
            holder.imgPost.setTag(null);
            holder.imgPost.setOnClickListener(null);

            try {
                JSONObject post = list.get(position);

                String user = post.optString(
                        "username",
                        post.optString("user", "entity404")
                );

                String textPost = post.optString(
                        "text_post",
                        post.optString("text", "")
                );

                String datetime = post.optString(
                        "datetime",
                        post.optString("date", "")
                );

                final String id = post.optString("id", "");
                String stars = post.optString("stars", "0");

                holder.btnComments.setOnClickListener(v -> {
                    Intent commentIntent = new Intent(
                            FederationsFeed.this,
                            comments_activity.class
                    );

                    commentIntent.putExtra(
                            "post_id",
                            id
                    );

                    startActivity(commentIntent);
                });

                holder.tvUsername.setText(
                        user.startsWith("@") ? user : "@" + user
                );

                holder.tvDate.setText(datetime);
                holder.tvStarCount.setText(stars);

                new ImageLoader().viewProfilePicture(
                        context,
                        user,
                        holder.avatarPost
                );

                /*
                 * ===== IMAGEM DO POST =====
                 *
                 * O backend manda, por exemplo:
                 *
                 * [IMAGE]http://servidor/imagem.jpg
                 *
                 * O LinkaLite detecta a marca [IMAGE], extrai somente a URL
                 * e envia a imagem para o ImageLoader, que sempre usa o lite-render HTTP.
                 */
                final String imageUrl = extractImageUrl(textPost);

                if (!imageUrl.isEmpty()) {
                    holder.imgPost.setVisibility(View.VISIBLE);

                    // A URL original vai para o ImageLoader.
                    // O ImageLoader e o unico responsavel por encaminhar tudo ao /lite-render.
                    holder.imgPost.setTag(imageUrl);

                    Log.d(
                            "LINKA_FEED",
                            "[IMAGE] detectada. Original=" + imageUrl
                                    + " | enviando para ImageLoader"
                    );

                    new ImageLoader().LoadImageUrl(imageUrl, holder.imgPost);

                    holder.imgPost.setOnClickListener(v -> {
                        Intent intent = new Intent(
                                context,
                                ViewPicture.class
                        );

                        intent.putExtra(
                                "type",
                                "Image"
                        );

                        // Usa a mesma URL da imagem.
                        // O ImageLoader continua sendo responsavel pelo lite-render.
                        intent.putExtra(
                                "url",
                                imageUrl
                        );

                        context.startActivity(intent);
                    });
                }

                // Nao deixa a URL da imagem poluir o texto do post.
                textPost = removeImageMarkers(textPost);

                holder.tvText.setText(textPost);

            } catch (Exception e) {
                Log.e(
                        "LINKA_ADAPTER",
                        "Error at position "
                                + position
                                + ": "
                                + e.getMessage()
                );
            }

            return convertView;
        }
    }

    public String requestHTTP(
            String urlParam,
            String method,
            JSONObject json_body
    ) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(urlParam);
            connection = (HttpURLConnection) url.openConnection();

            HttpURLConnection.setFollowRedirects(true);
            connection.setInstanceFollowRedirects(true);

            method = method.toUpperCase();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
            );

            connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Android) LinkaLite"
            );

            if (method.equals("POST") || method.equals("PUT")) {
                connection.setDoOutput(true);

                OutputStream os = connection.getOutputStream();
                os.write(json_body.toString().getBytes("UTF-8"));
                os.flush();
                os.close();
            }

            int responseCode = connection.getResponseCode();

            InputStream inputStream;

            if (responseCode >= 200 && responseCode < 300) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }

            if (inputStream == null) {
                return "";
            }

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            inputStream,
                            "UTF-8"
                    )
            );

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line);
            }

            in.close();

            if (responseCode < 200 || responseCode >= 300) {
                Log.e(
                        "LINKA_HTTP",
                        "HTTP " + responseCode + ": " + response
                );
            }

            return response.toString();

        } catch (Exception e) {
            Log.e(
                    "LINKA_HTTP",
                    "Connection exception: " + e.getMessage(),
                    e
            );
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return "";
    }
}
