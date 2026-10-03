package com.LinkaProject.linkaLite;

import android.app.TabActivity;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HomeActivity extends TabActivity {

    private ImageButton btnHome;
    private ImageButton btnProfile;
    private ImageButton btnOptions;
    private ImageButton btnChat;
    private Button newPost;
    private Button btnClose;
    private Button btnLogin;

    private ListView listViewForYou;
    private ListView listViewNewest;
    private PostAdapter adapterForYou;
    private PostAdapter adapterNewest;
    private ArrayList<JSONObject> postsForYou = new ArrayList<JSONObject>();
    private ArrayList<JSONObject> postsNewest = new ArrayList<JSONObject>();

    private View footerForYou;
    private View footerNewest;

    private boolean isGuest = false;
    private ScheduledExecutorService scheduler;
    private ScheduledExecutorService schedulerNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        TabHost tabHost = getTabHost();

        TabHost.TabSpec specForYou = tabHost.newTabSpec("foryou");
        specForYou.setIndicator("For You");
        specForYou.setContent(R.id.tabForYou);
        tabHost.addTab(specForYou);

        TabHost.TabSpec specNewest = tabHost.newTabSpec("newest");
        specNewest.setIndicator("Newest");
        specNewest.setContent(R.id.tabNewest);
        tabHost.addTab(specNewest);

        tabHost.setCurrentTab(0);

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                loadFeedForTab(tabId);
            }
        });

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

        btnClose = (Button) findViewById(R.id.btnClose);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        newPost = (Button) findViewById(R.id.newPost);
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        LinearLayout layoutGuest = (LinearLayout) findViewById(R.id.layoutGuest);

        Intent intent = getIntent();
        isGuest = intent.getBooleanExtra("isGuest", false);
        if (isGuest && layoutGuest != null) {
            layoutGuest.setVisibility(View.VISIBLE);
        }

        if (btnLogin != null) {
            btnLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (layoutGuest != null) layoutGuest.setVisibility(View.INVISIBLE);
                }
            });
        }

        if (btnChat != null) {
            btnChat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(HomeActivity.this, chatActivity.class));
                }
            });
        }

        if (btnOptions != null) {
            btnOptions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(HomeActivity.this, optionActivity.class));
                }
            });
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(HomeActivity.this, profile.class));
                }
            });
        }

        if (newPost != null) {
            newPost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(HomeActivity.this, newPost.class));
                }
            });
        }

        listViewForYou = (ListView) findViewById(R.id.listViewForYou);
        listViewNewest = (ListView) findViewById(R.id.listViewNewest);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        footerForYou = inflater.inflate(R.layout.footer_federations, null);
        footerNewest = inflater.inflate(R.layout.footer_federations, null);
        if (listViewForYou != null) {
            listViewForYou.addFooterView(footerForYou);
            adapterForYou = new PostAdapter(this, postsForYou);
            listViewForYou.setAdapter(adapterForYou);
        }

        if (listViewNewest != null) {
            listViewNewest.addFooterView(footerNewest);
            adapterNewest = new PostAdapter(this, postsNewest);
            listViewNewest.setAdapter(adapterNewest);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentTab = getTabHost().getCurrentTabTag();
        loadFeedForTab(currentTab != null ? currentTab : "foryou");
    }

    private void loadFeedForTab(String tabId) {
        Log.d("LINKA_DEBUG", "Iniciando loadFeedForTab para a aba: " + tabId);
        String baseUrl = "http://linkaProject.pythonanywhere.com";
        JSONArray urlsArray = new JSONArray();

        try {
            config cfg = new config();
            
            String rawCfg = cfg.loadCfgAsJson(HomeActivity.this, "config.cfg");
            Log.d("LINKA_DEBUG", "Conteudo bruto de config.cfg: " + rawCfg);
            
            if (rawCfg != null && !rawCfg.isEmpty()) {
                JSONObject jsonCfg = new JSONObject(rawCfg);
                JSONObject server = jsonCfg.optJSONObject("SERVER");
                if (server != null) {
                    baseUrl = server.optString("url", baseUrl);
                }
            }

            String rawTimeline = cfg.loadCfgAsJson(HomeActivity.this, "config-timeline.cfg");
            Log.d("LINKA_DEBUG", "Conteudo bruto de config-timeline.cfg: " + rawTimeline);

            if (rawTimeline != null && !rawTimeline.isEmpty()) {
                JSONObject jsonTimeline = new JSONObject(rawTimeline);
                JSONObject fedSection = jsonTimeline.optJSONObject("FEDERATION-TIMELINE");
                
                if (fedSection != null) {
                    String urlsValue = fedSection.optString("urls", "");
                    
                    if (urlsValue.contains("#")) {
                        urlsValue = urlsValue.split("#")[0].trim();
                    }
                    
                    if (urlsValue.contains(":/data/data/")) {
                        urlsValue = urlsValue.split(":/data/data/")[0].trim();
                    }

                    String[] parts = urlsValue.split(",");
                    for (String part : parts) {
                        String cleanUrl = part.trim();
                        if (!cleanUrl.isEmpty()) {
                            urlsArray.put(cleanUrl);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("LINKA_DEBUG", "Excecao ao ler arquivos de configuracao", e);
        }

        Log.d("LINKA_DEBUG", "URLs extraidas para o ForYou: " + urlsArray.toString());

        if ("foryou".equals(tabId)) {
            JSONObject body = new JSONObject();
            try {
                body.put("urls", urlsArray);
            } catch (JSONException e) {
                Log.e("LINKA_DEBUG", "Erro JSON ao colocar urls no body", e);
            }
            Log.d("LINKA_DEBUG", "Enviando POST para " + baseUrl + "/view-external-posts com body: " + body.toString());
            new FetchFeedTask("foryou", baseUrl + "/view-external-posts", "POST", body).execute();
        } else {
            Log.d("LINKA_DEBUG", "Enviando GET para " + baseUrl + "/feed");
            new FetchFeedTask("newest", baseUrl + "/feed", "GET", null).execute();
        }
    }

    private class FetchFeedTask extends AsyncTask<Void, Void, String> {
        private String tabId;
        private String url;
        private String method;
        private JSONObject jsonBody;

        public FetchFeedTask(String tabId, String url, String method, JSONObject jsonBody) {
            this.tabId = tabId;
            this.url = url;
            this.method = method;
            this.jsonBody = jsonBody;
        }

        @Override
        protected String doInBackground(Void... params) {
            return requestHTTP(url, method, jsonBody);
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("LINKA_DEBUG", "onPostExecute para a aba [" + tabId + "] com resposta: " + result);

            ArrayList<JSONObject> targetList = "foryou".equals(tabId) ? postsForYou : postsNewest;
            PostAdapter targetAdapter = "foryou".equals(tabId) ? adapterForYou : adapterNewest;

            if (targetList == null || targetAdapter == null) {
                Log.e("LINKA_DEBUG", "targetList ou targetAdapter e nulo para a aba: " + tabId);
                return;
            }

            targetList.clear();

            if (result == null || result.trim().isEmpty()) {
                Log.e("LINKA_DEBUG", "Erro: Resposta HTTP retornou nula ou vazia para a aba: " + tabId);
                targetAdapter.notifyDataSetChanged();
                Toast.makeText(HomeActivity.this, "Error loading feed", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                String trimmed = result.trim();
                if (trimmed.startsWith("[")) {
                    JSONArray jsonArray = new JSONArray(trimmed);
                    Log.d("LINKA_DEBUG", "Quantidade de posts recebidos no JSONArray (" + tabId + "): " + jsonArray.length());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        targetList.add(jsonArray.getJSONObject(i));
                    }
                } else if (trimmed.startsWith("{")) {
                    JSONObject errObj = new JSONObject(trimmed);
                    String msg = errObj.optString("error", errObj.optString("status", "Error loading feed"));
                    Log.e("LINKA_DEBUG", "Servidor retornou objeto JSON de erro (" + tabId + "): " + msg);
                    Toast.makeText(HomeActivity.this, msg, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("LINKA_DEBUG", "Resposta invalida do servidor (nao e JSON Array nem Object): " + trimmed);
                }
            } catch (Exception e) {
                Log.e("LINKA_DEBUG", "Excecao no parse dos posts na aba (" + tabId + ")", e);
                Toast.makeText(HomeActivity.this, "Error parsing posts", Toast.LENGTH_SHORT).show();
            }

            targetAdapter.notifyDataSetChanged();
        }
    }

    private class PostAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<JSONObject> list;
        private ImageLoader imageLoader; // Instância única para evitar múltiplos pools de Threads

        public PostAdapter(Context context, ArrayList<JSONObject> list) {
            this.context = context;
            this.list = list;
            this.imageLoader = new ImageLoader();
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
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.item_post, null);
            }

            ImageView avatarPost = (ImageView) convertView.findViewById(R.id.postAvatar);
            ImageView imgPost = (ImageView) convertView.findViewById(R.id.imgPost);
            TextView tvUsername = (TextView) convertView.findViewById(R.id.postUsername);
            TextView tvText = (TextView) convertView.findViewById(R.id.postText);
            TextView tvDate = (TextView) convertView.findViewById(R.id.postDate);
            Button btnComments = (Button) convertView.findViewById(R.id.btnComments);

            // Reset obrigatório de ambas as ImageViews para não manter imagens de views recicladas
            avatarPost.setImageBitmap(null);
            imgPost.setImageBitmap(null);
            imgPost.setVisibility(View.GONE);

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

            try {
                JSONObject post = list.get(position);
                String username = post.optString("username", post.optString("user", "entity404"));
                String textPost = post.optString("text_post", post.optString("text", ""));
                String datetime = post.optString("datetime", post.optString("date", ""));
                final String id = post.optString("id", "");

                btnComments.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(HomeActivity.this, comments_activity.class);
                        intent.putExtra("isGuest", isGuest);
                        intent.putExtra("post_id", id);
                        startActivity(intent);
                    }
                });

                tvUsername.setText("@" + username);
                tvDate.setText(datetime);

                // Carrega foto de perfil reutilizando o ImageLoader único
                imageLoader.viewProfilePicture(context, username, avatarPost);

                if (textPost.contains("[IMAGE]")) {
                    String[] lines = textPost.split("\n");
                    for (String line : lines) {
                        if (line.contains("[IMAGE]")) {
                            final String newUrl = line.replace("[IMAGE]", "").trim();
                            if (!newUrl.isEmpty()) {
                                imgPost.setVisibility(View.VISIBLE);
                                final String urlProxy = "http://linkaProject.pythonanywhere.com/lite-render?url=" + newUrl;

                                imgPost.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(HomeActivity.this, ViewPicture.class);
                                        intent.putExtra("type", "Image");
                                        intent.putExtra("url", urlProxy);
                                        startActivity(intent);
                                    }
                                });

                                // Carrega imagem do post reutilizando o ImageLoader único
                                imageLoader.LoadImageUrl(urlProxy, imgPost);
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