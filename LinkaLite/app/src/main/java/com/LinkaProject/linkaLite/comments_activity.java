package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class comments_activity extends Activity {
    private ListView lvComments;
    private String postId = "";
    private EditText commentEdt;
    private Button btnSend;
    private String username = "";
    private String baseUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 1. Recebe o post_id aceitando tanto String quanto Int
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Object idObj = extras.get("post_id");
            if (idObj != null) {
                postId = String.valueOf(idObj);
            }
        }

        // 2. Carrega configurações do app
        try {
            config cfg = new config();
            String cfgString = cfg.loadCfgAsJson(comments_activity.this, "config.cfg");
            JSONObject jsonCfg = new JSONObject(cfgString);
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            username = fastLogin.optString("username", "");
            baseUrl = server.optString("url", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.comments_activity);
        lvComments = (ListView) findViewById(R.id.lvComments);
        commentEdt = (EditText) findViewById(R.id.commentEdt);
        btnSend = (Button) findViewById(R.id.btnSend);

        // 3. Ação do Botão de Enviar
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String textComment = commentEdt.getText().toString().trim();

                // Validação para barrar o 401 antes mesmo de chamar a rede
                if (username == null || username.trim().isEmpty()) {
                    Toast.makeText(comments_activity.this, "Erro: Usuário não identificado!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (postId == null || postId.trim().isEmpty()) {
                    Toast.makeText(comments_activity.this, "Erro: ID do Post inválido!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (textComment.isEmpty()) {
                    Toast.makeText(comments_activity.this, "Escreva um comentário!", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JSONObject commentJson = new JSONObject();
                    commentJson.put("username", username);
                    commentJson.put("text_comment", textComment);

                    // Tenta converter para número se for numérico, caso contrário envia String
                    try {
                        commentJson.put("post_id", Integer.parseInt(postId));
                    } catch (NumberFormatException e) {
                        commentJson.put("post_id", postId);
                    }

                    new SendCommentTask().execute(commentJson.toString());

                } catch (JSONException e) {
                    Toast.makeText(comments_activity.this, "Erro ao criar JSON", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (postId != null && !postId.trim().isEmpty()) {
            new FetchCommentsTask().execute(postId);
        }
    }

    // =======================================================
    // 1. Task para ENVIAR o comentário em Background (POST)
    // =======================================================
    private class SendCommentTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                String jsonPayload = params[0];
                JSONObject commentJson = new JSONObject(jsonPayload);

                // Executa o POST e traz o corpo da resposta do Flask
                return request.requestHTTP(baseUrl + "/comments", "post", commentJson);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            // Checa a resposta real do backend
            if (response != null && response.contains("sucess")) {
                commentEdt.setText("");
                Toast.makeText(comments_activity.this, "Enviado com sucesso!", Toast.LENGTH_SHORT).show();
                
                // Recarrega a lista de comentários
                new FetchCommentsTask().execute(postId);
            } else {
                Toast.makeText(comments_activity.this, "Erro ao enviar (Verifique se o usuário bate com a sessão)", Toast.LENGTH_LONG).show();
            }
        }
    }

    // =======================================================
    // 2. Parsers e Task de BUSCAR comentários
    // =======================================================
    public List<Comment> parseCommentsResponse(String jsonResponse) {
        List<Comment> commentsList = new ArrayList<Comment>();

        try {
            JSONObject rootObject = new JSONObject(jsonResponse);

            if (rootObject.has("comments")) {
                JSONArray commentsArray = rootObject.getJSONArray("comments");

                for (int i = 0; i < commentsArray.length(); i++) {
                    JSONObject commentObj = commentsArray.getJSONObject(i);

                    int commentId = commentObj.optInt("comment_id", -1);
                    int pId = commentObj.optInt("post_id", -1);
                    String commentUsername = commentObj.optString("username", "IdontKnowMyName;)");
                    String textComment = commentObj.optString("text_comment", "");

                    Comment comment = new Comment(commentId, pId, commentUsername, textComment);
                    commentsList.add(comment);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return commentsList;
    }

    public void popularLista(String jsonResult) {
        List<Comment> comments = parseCommentsResponse(jsonResult);
        CommentAdapter adapter = new CommentAdapter(comments_activity.this, comments);
        lvComments.setAdapter(adapter);
    }

    private class FetchCommentsTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                String targetPostId = params[0];
                JSONObject jsonComment = new JSONObject();

                try {
                    jsonComment.put("post_id", Integer.parseInt(targetPostId));
                } catch (NumberFormatException e) {
                    jsonComment.put("post_id", targetPostId);
                }

                return request.requestHTTP(baseUrl + "/view-comments", "post", jsonComment);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && !result.trim().isEmpty()) {
                popularLista(result);
            } else {
                Toast.makeText(comments_activity.this, "Erro ao carregar comentários", Toast.LENGTH_SHORT).show();
            }
        }
    }
}