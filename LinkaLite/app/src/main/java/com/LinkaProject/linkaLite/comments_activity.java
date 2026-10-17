package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
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
    private String externalUrl = "";
    private boolean isExternal = false;
    private LinearLayout layoutComment;
    private String baseUrl = "";
    private String uniqueId = "";
    private LinearLayout layoutGuest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Recebe os parâmetros do Intent
        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null && extras.get("post_id") != null) {
                postId = String.valueOf(extras.get("post_id"));
            }
            isExternal = intent.getBooleanExtra("isExternal", false);
            externalUrl = intent.getStringExtra("externalUrl");
            uniqueId = intent.getStringExtra("uniqueId");

            // Garante que Strings não fiquem null
            if (externalUrl == null) externalUrl = "";
            if (uniqueId == null) uniqueId = "";
        }

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
        layoutComment = (LinearLayout) findViewById(R.id.layoutComment);
        layoutGuest = (LinearLayout) findViewById(R.id.layoutGuest);
        btnSend = (Button) findViewById(R.id.btnSend);

        boolean isGuest = intent.getBooleanExtra("isGuest", false);
        if (isGuest) {
            layoutComment.setVisibility(View.INVISIBLE);
            layoutGuest.setVisibility(View.VISIBLE);
        }

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String textComment = commentEdt.getText().toString().trim();

                if (username == null || username.trim().length() == 0) {
                    Toast.makeText(comments_activity.this, "Erro: user not registered!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // CORREÇÃO 1: Validação de ID flexível para Local vs Externo
                if (!isExternal && (postId == null || postId.trim().length() == 0)) {
                    Toast.makeText(comments_activity.this, "Erro: ID do Post inválido!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isExternal && (uniqueId == null || uniqueId.trim().length() == 0)) {
                    Toast.makeText(comments_activity.this, "Erro: UniqueID federado inválido!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (textComment.length() == 0) {
                    Toast.makeText(comments_activity.this, "Escreva um comentário!", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JSONObject commentJson = new JSONObject();
                    commentJson.put("username", username);
                    commentJson.put("text_comment", textComment);

                    // CORREÇÃO 2: Envia uniqueId se for externo, postId se for local
                    if (isExternal) {
                        commentJson.put("post_id", uniqueId);
                        commentJson.put("unique_id", uniqueId);
                    } else {
                        try {
                            commentJson.put("post_id", Integer.parseInt(postId));
                        } catch (NumberFormatException e) {
                            commentJson.put("post_id", postId);
                        }
                    }

                    new SendCommentTask().execute(commentJson.toString());
                } catch (JSONException e) {
                    Toast.makeText(comments_activity.this, "Erro ao criar JSON", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // CORREÇÃO 3: Dispara a busca se tiver ID Local OU se for Externo com UniqueID
        if (isExternal && uniqueId.length() > 0) {
            new FetchCommentsTask().execute(uniqueId);
        } else if (postId != null && postId.trim().length() > 0) {
            new FetchCommentsTask().execute(postId);
        }
    }

    private class SendCommentTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                String jsonPayload = params[0];
                JSONObject commentJson = new JSONObject(jsonPayload);

                // Garante a barra final na URL do backend se necessário
                String targetUrl = isExternal ? externalUrl : baseUrl;
                if (targetUrl.endsWith("/")) {
                    targetUrl = targetUrl.substring(0, targetUrl.length() - 1);
                }

                return request.requestHTTP(targetUrl + "/comments", "post", commentJson, comments_activity.this);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            if (response != null && (response.contains("sucess") || response.contains("success"))) {
                commentEdt.setText("");
                Toast.makeText(comments_activity.this, "Enviado com sucesso!", Toast.LENGTH_SHORT).show();
                
                String target = isExternal ? uniqueId : postId;
                new FetchCommentsTask().execute(target);
            } else {
                Toast.makeText(comments_activity.this, "Erro ao enviar comentário", Toast.LENGTH_LONG).show();
            }
        }
    }

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
                    String commentUsername = commentObj.optString("username", "Anonimo");
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

        CommentAdapter adapter = new CommentAdapter(
            comments_activity.this, 
            comments, 
            isExternal, 
            externalUrl
        );

        lvComments.setAdapter(adapter);
    }

    private class FetchCommentsTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                String targetPostId = params[0];
                JSONObject jsonComment = new JSONObject();

                if (isExternal) {
                    jsonComment.put("post_id", uniqueId);
                    jsonComment.put("unique_id", uniqueId);
                } else {
                    try {
                        jsonComment.put("post_id", Integer.parseInt(targetPostId));
                    } catch (NumberFormatException e) {
                        jsonComment.put("post_id", targetPostId);
                    }
                }

                String targetUrl = isExternal ? externalUrl : baseUrl;
                if (targetUrl.endsWith("/")) {
                    targetUrl = targetUrl.substring(0, targetUrl.length() - 1);
                }

                return request.requestHTTP(targetUrl + "/view-comments", "post", jsonComment, comments_activity.this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && result.trim().length() > 0) {
                popularLista(result);
            } else {
                Toast.makeText(comments_activity.this, "Erro ao carregar comentários", Toast.LENGTH_SHORT).show();
            }
        }
    }
}