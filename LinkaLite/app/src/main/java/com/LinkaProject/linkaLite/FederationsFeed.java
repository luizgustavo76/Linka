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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
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
    private String username = "";
    private ArrayList<JSONObject> postsList;
    private String currentUrl = "";
    private String baseUrl = "";
    private ScheduledExecutorService scheduler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        AppConfig config = new AppConfig(this);
        username = config.getUsername();
        try{
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(FederationsFeed.this, "config.cfg"));
            JSONObject server = jsonCfg.getJSONObject("server");
            baseUrl = server.optString("url", "";)
        }catch(JSONException e){
            e.printStackTrace();
        }
        // 1. Obtém a URL da Intent ou recupera do AppConfig
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("url") && intent.getStringExtra("url") != null && !intent.getStringExtra("url").isEmpty()) {
            currentUrl = intent.getStringExtra("url");
        } else {
            currentUrl = config.getUrl();
        }

        // 2. Formatação correta da URL base
        currentUrl = formatUrl(currentUrl);

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
            AppConfig config = new AppConfig(this);
            currentUrl = formatUrl(config.getUrl());
        }

        Log.d("LINKA_FEED", "Executando FetchFeedTask para: " + currentUrl);
        new FetchFeedTask().execute(currentUrl);
    }

    private String formatUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty()) return "";
        
        String formatted = rawUrl.trim();
        if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
            formatted = "http://" + formatted;
        }
        if (formatted.endsWith("/")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    private class FetchFeedTask extends AsyncTask<String, Void, String> {
        private String requestedUrl;

        @Override
        protected String doInBackground(String... urls) {
            requestedUrl = urls[0];
            Log.d("LINKA_FEED", "Requisitando feed na URL: " + requestedUrl);
            return requestHTTP(requestedUrl, "GET", new JSONObject());
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("LINKA_FEED", "Resultado recebido (tam: " + (result != null ? result.length() : 0) + ")");
            Log.d("LINKA_FEED", "Conteudo RAW: " + result);

            if (result == null || result.trim().length() == 0) {
                Log.e("LINKA_FEED", "Resposta nula ou vazia do servidor.");
                Toast.makeText(FederationsFeed.this, "Erro: Servidor nao respondeu", Toast.LENGTH_SHORT).show();
                return;
            }

            String trimmed = result.trim();
            if (trimmed.startsWith("<")) {
                Log.e("LINKA_FEED", "O servidor retornou HTML em vez de JSON! Verifique o backend.");
                Toast.makeText(FederationsFeed.this, "Erro: Servidor retornou HTML em vez de JSON", Toast.LENGTH_LONG).show();
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
                    } else {
                        Log.e("LINKA_FEED", "Objeto JSON sem array 'posts'/'feed'/'data'. Chaves: " + jsonObject.names());
                    }
                } else {
                    Log.e("LINKA_FEED", "Formato invalido de resposta: " + trimmed);
                }

                if (jsonArray != null) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        postsList.add(jsonArray.getJSONObject(i));
                    }
                    Log.d("LINKA_FEED", "Sucesso! " + postsList.size() + " posts renderizados.");
                    postAdapter.notifyDataSetChanged();
                } else {
                    Log.e("LINKA_FEED", "Nao foi possivel extrair o JSONArray dos posts.");
                    Toast.makeText(FederationsFeed.this, "Erro: Estrutura JSON incompativel", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Log.e("LINKA_FEED", "JSONException ao processar posts: " + e.getMessage());
                e.printStackTrace();
                Toast.makeText(FederationsFeed.this, "Erro de sintaxe JSON", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("LINKA_FEED", "Excecao geral no parsing: " + e.getMessage());
                e.printStackTrace();
                Toast.makeText(FederationsFeed.this, "Erro no processamento dos posts", Toast.LENGTH_SHORT).show();
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

            // Reseta limpo os estados da imagem para reuso eficiente de views no ListView
            imgPost.setImageDrawable(null);
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

                // --- TRATAMENTO E EXTRAÇÃO DA IMAGEM ---
                if (textPost != null && textPost.contains("[IMAGE]")) {
                    // Regex delimitada para ignorar espacos em branco e quebras de linha
                    Pattern pattern = Pattern.compile("\\[IMAGE\\](https?://[^\\s\n\r]+)");
                    Matcher matcher = pattern.matcher(textPost);

                    if (matcher.find()) {
                        String imageUrl = matcher.group(1).trim();

                        if (!imageUrl.isEmpty()) {
                            imgPost.setVisibility(View.VISIBLE);

                            // Aplica o encoding na URL recebida para nao truncar os parametros de querystring (?width=...&auto=...)
                            String encodedImageUrl;
                            try {
                                encodedImageUrl = URLEncoder.encode(imageUrl, "UTF-8");
                            } catch (Exception e) {
                                encodedImageUrl = imageUrl;
                            }

                            // Formata a URL final apontando para o proxy de renderização
                            final String urlProxy = currentUrl + "/lite-render?url=" + encodedImageUrl;

                            // Carrega a imagem pelo ImageLoader
                            new ImageLoader().LoadImageUrl(urlProxy, imgPost);

                            imgPost.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(context, ViewPicture.class);
                                    intent.putExtra("type", "Image");
                                    intent.putExtra("url", urlProxy);
                                    context.startActivity(intent);
                                }
                            });

                            // Remove a tag [IMAGE] e o link original do texto principal
                            textPost = textPost.replaceAll("\\[IMAGE\\]https?://[^\\s\n\r]+", "").trim();
                        }
                    }
                }

                // Exibe o texto limpo sem as marcas [IMAGE]
                tvText.setText(textPost);

            } catch (Exception e) {
                Log.e("LINKA_ADAPTER", "Erro ao renderizar item na posicao " + position + ": " + e.getMessage());
                e.printStackTrace();
            }

            return convertView;
        }
    }

    public String requestHTTP(String urlParam, String method, JSONObject json_body) {
        HttpURLConnection connection = null;
        Log.d("LINKA_HTTP", "Iniciando requisicao HTTP [" + method + "] -> " + urlParam);

        try {
            URL url = new URL(urlParam);
            connection = (HttpURLConnection) url.openConnection();
            HttpURLConnection.setFollowRedirects(true);
            connection.setInstanceFollowRedirects(true);

            method = method.toUpperCase();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "LinkaLiteApp/1.0");

            if (method.equals("POST") || method.equals("PUT")) {
                connection.setDoOutput(true);
                OutputStream os = connection.getOutputStream();
                os.write(json_body.toString().getBytes("UTF-8"));
                os.flush();
                os.close();
            }

            int responseCode = connection.getResponseCode();
            Log.d("LINKA_HTTP", "Response Code: " + responseCode + " para " + urlParam);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                return response.toString();
            } else {
                InputStream errStream = connection.getErrorStream();
                if (errStream != null) {
                    BufferedReader inErr = new BufferedReader(new InputStreamReader(errStream, "UTF-8"));
                    StringBuilder errResponse = new StringBuilder();
                    String errLine;
                    while ((errLine = inErr.readLine()) != null) {
                        errResponse.append(errLine);
                    }
                    inErr.close();
                    Log.e("LINKA_HTTP", "Corpo do erro HTTP " + responseCode + ": " + errResponse.toString());
                } else {
                    Log.e("LINKA_HTTP", "Erro HTTP sem corpo de resposta: " + responseCode);
                }
            }
        } catch (java.net.UnknownHostException e) {
            Log.e("LINKA_HTTP", "DNS Error (Host nao encontrado): " + e.getMessage());
        } catch (java.net.SocketTimeoutException e) {
            Log.e("LINKA_HTTP", "Timeout na conexao (10s expirados): " + e.getMessage());
        } catch (javax.net.ssl.SSLException e) {
            Log.e("LINKA_HTTP", "Erro TLS/SSL Handshake: " + e.getMessage());
        } catch (Exception e) {
            Log.e("LINKA_HTTP", "Excecao inesperada na conexao: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return "";
    }
}