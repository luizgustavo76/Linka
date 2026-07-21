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
import android.widget.EditText;
import org.json.JSONArray;
import org.json.JSONObject;
import android.widget.Button;
import android.content.Intent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class HomeActivity extends Activity {
    private ImageButton btnHome;
    private ImageButton btnProfile;
    private ImageButton btnOptions;
    private ImageButton btnChat;
    private Button newPost;
    private ListView listViewPosts;
    private PostAdapter postAdapter;
    private ArrayList<JSONObject> postsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
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
        // Configura a nossa ListView do Feed
        listViewPosts = (ListView) findViewById(R.id.listViewPosts);
        postsList = new ArrayList<JSONObject>();
        postAdapter = new PostAdapter(this, postsList);
        listViewPosts.setAdapter(postAdapter);

        // Dispara a busca do feed na inicialização
        new FetchFeedTask().execute("http://linkaProject.pythonanywhere.com/feed"); // Troque pela sua URL do Flask
    }

    // A AsyncTask garante que a requisição de rede rode fora da Main Thread
    private class FetchFeedTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return requestHTTP(urls[0], "GET", new JSONObject());
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null || result.equals("")) {
                Toast.makeText(HomeActivity.this, "Erro ao carregar o feed", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // Transforma a string de resposta no Array de objetos que o servidor mandou
                JSONArray jsonArray = new JSONArray(result);
                postsList.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                    postsList.add(jsonArray.getJSONObject(i));
                }

                // Avisa o Adapter que chegaram dados novos para ele atualizar a tela
                postAdapter.notifyDataSetChanged();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(HomeActivity.this, "Erro ao parsear os posts", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // O Adapter gerencia o reaproveitamento dos componentes visuais do layout (Padrão ViewHolder para poupar RAM)
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
            ImageView imgPost = (ImageView) convertView.findViewById(R.id.imgPost);
            TextView tvUsername = (TextView) convertView.findViewById(R.id.postUsername);
            TextView tvText = (TextView) convertView.findViewById(R.id.postText);
            TextView tvDate = (TextView) convertView.findViewById(R.id.postDate);
            TextView tvStarCount = (TextView) convertView.findViewById(R.id.starCount);

            try {
                JSONObject post = list.get(position);
                tvUsername.setText("@" + post.getString("username"));
                tvText.setText(post.getString("text_post"));
                tvDate.setText(post.getString("datetime"));
                String complet = post.getString("text_post");
                String[] lines = complet.split("\n");
                imgPost.setImageBitmap(null);
                imgPost.setVisibility(View.GONE);

                // Variável para sabermos se já achamos a imagem (evita conflito se tiver mais de um [IMAGE])
                boolean achouImagem = false;

                for (int i = 0; i < lines.length; i++) {
                    String actual_line = lines[i];
                    
                    if (actual_line.contains("[IMAGE]")) {
                        String newUrl = actual_line.replace("[IMAGE]", "").trim();
                        
                        if (!newUrl.equals("")) {
                            achouImagem = true;
                            imgPost.setVisibility(View.VISIBLE);
                            String urlProxy = "http://linkaProject.pythonanywhere.com/lite-render?url=" + newUrl;
                            
                            // Dispara o download usando a rota intermediária
                            new ImageLoader().LoadImageUrl(urlProxy, imgPost);
                            
                            // Se já achou a tag de imagem, podemos parar o loop aqui (opcional)
                            break; 
                        }
                    }
                }
                tvStarCount.setText("0"); // Se o seu JSON ainda não tem estrelas, deixamos travado em 0 por enquanto
            } catch (Exception e) {
                e.printStackTrace();
            }

            return convertView;
        }
    }

    // Sua função original modificada com segurança contra escrita em requisições GET
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
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
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