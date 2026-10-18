package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class comments_activity extends Activity {
    private static final String TAG = "[DEBUG_BRUNO]";

    private ListView lvComments;
    private String postId = "";
    private EditText commentEdt;
    private Button btnSend;
    private String username = "";
    private String externalUrl = "";
    private String originalUrl = "";
    private boolean isExternal = false;
    private LinearLayout layoutComment;
    private String baseUrl = "";
    private String uniqueId = "";
    private LinearLayout layoutGuest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "==================================================");
        Log.d(TAG, "comments_activity :: onCreate() INICIADO");

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
            originalUrl = intent.getStringExtra("originalUrl");

            // Garante que Strings não fiquem null
            if (externalUrl == null) externalUrl = "";
            if (uniqueId == null) uniqueId = "";
            if (originalUrl == null) originalUrl = "";

            Log.d(TAG, "INTENT EXTRAS RECEBIDOS:");
            Log.d(TAG, " -> postId: " + postId);
            Log.d(TAG, " -> isExternal: " + isExternal);
            Log.d(TAG, " -> externalUrl: " + externalUrl);
            Log.d(TAG, " -> uniqueId: " + uniqueId);
            Log.d(TAG, " -> originalUrl: " + originalUrl);
        } else {
            Log.e(TAG, "INTENT RECEBIDO VEIO NULO!");
        }

        try {
            config cfg = new config();
            String cfgString = cfg.loadCfgAsJson(comments_activity.this, "config.cfg");
            Log.d(TAG, "CFG RAW LOADED: " + cfgString);
            JSONObject jsonCfg = new JSONObject(cfgString);
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            username = fastLogin.optString("username", "");
            baseUrl = server.optString("url", "");

            Log.d(TAG, "CONFIG PARSED:");
            Log.d(TAG, " -> username: " + username);
            Log.d(TAG, " -> baseUrl: " + baseUrl);
        } catch (JSONException e) {
            Log.e(TAG, "ERRO AO CARREGAR CONFIG.CFG: " + e.getMessage(), e);
        }

        setContentView(R.layout.comments_activity);
        lvComments = (ListView) findViewById(R.id.lvComments);
        commentEdt = (EditText) findViewById(R.id.commentEdt);
        layoutComment = (LinearLayout) findViewById(R.id.layoutComment);
        layoutGuest = (LinearLayout) findViewById(R.id.layoutGuest);
        btnSend = (Button) findViewById(R.id.btnSend);

        boolean isGuest = intent != null && intent.getBooleanExtra("isGuest", false);
        Log.d(TAG, "isGuest: " + isGuest);

        if (isGuest && layoutComment != null && layoutGuest != null) {
            Log.d(TAG, "Ajustando layout para Usuario GUEST");
            layoutComment.setVisibility(View.INVISIBLE);
            layoutGuest.setVisibility(View.VISIBLE);
        }

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String textComment = commentEdt.getText().toString().trim();
                Log.d(TAG, "BTN SEND CLICKED -> Texto digitado: '" + textComment + "'");

                if (username == null || username.trim().length() == 0) {
                    Log.e(TAG, "ERRO: Username vazio/nao registrado no app!");
                    Toast.makeText(comments_activity.this, "Erro: user not registered!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validação de ID flexível para Local vs Externo
                if (!isExternal && (postId == null || postId.trim().length() == 0)) {
                    Log.e(TAG, "ERRO: PostID local invalido!");
                    Toast.makeText(comments_activity.this, "Erro: ID do Post inválido!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isExternal && (uniqueId == null || uniqueId.trim().length() == 0)) {
                    Log.e(TAG, "ERRO: UniqueID externo invalido!");
                    Toast.makeText(comments_activity.this, "Erro: UniqueID federado inválido!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (textComment.length() == 0) {
                    Log.w(TAG, "Tentativa de enviar comentario vazio.");
                    Toast.makeText(comments_activity.this, "Escreva um comentário!", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JSONObject commentJson = new JSONObject();
                    commentJson.put("username", username);
                    commentJson.put("text_comment", textComment);

                    if (isExternal) {
                        commentJson.put("post_id", uniqueId);
                        commentJson.put("unique_id", uniqueId);
                        commentJson.put("original_url", originalUrl);
                    } else {
                        try {
                            commentJson.put("post_id", Integer.parseInt(postId));
                        } catch (NumberFormatException e) {
                            commentJson.put("post_id", postId);
                        }
                    }

                    Log.d(TAG, "PAYLOAD DO NOVO COMENTARIO PREPARADO: " + commentJson.toString());
                    new SendCommentTask().execute(commentJson.toString());
                } catch (JSONException e) {
                    Log.e(TAG, "ERRO JSON AO CRIAR COMENTARIO: " + e.getMessage(), e);
                    Toast.makeText(comments_activity.this, "Erro ao criar JSON", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Dispara a busca se tiver ID Local OU se for Externo com UniqueID
        if (isExternal && uniqueId.length() > 0) {
            Log.d(TAG, "DISPARANDO FetchCommentsTask EXTERNO -> uniqueId: " + uniqueId);
            new FetchCommentsTask().execute(uniqueId);
        } else if (postId != null && postId.trim().length() > 0) {
            Log.d(TAG, "DISPARANDO FetchCommentsTask LOCAL -> postId: " + postId);
            new FetchCommentsTask().execute(postId);
        } else {
            Log.e(TAG, "NENHUM ID VALIDO ENCONTRADO PARA CARREGAR COMENTARIOS!");
        }
    }

    private class SendCommentTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                String jsonPayload = params[0];
                JSONObject commentJson = new JSONObject(jsonPayload);

                String targetUrl = isExternal ? externalUrl : baseUrl;
                if (targetUrl.endsWith("/")) {
                    targetUrl = targetUrl.substring(0, targetUrl.length() - 1);
                }

                String endpoint = isExternal ? "/comments" : "/comments";
                String fullUrl = targetUrl + endpoint;

                Log.d(TAG, "SendCommentTask :: POST HTTP -> URL: " + fullUrl);
                Log.d(TAG, "SendCommentTask :: BODY -> " + commentJson.toString());

                String resp = request.requestHTTP(fullUrl, "post", commentJson, comments_activity.this);
                Log.d(TAG, "SendCommentTask :: RESPOSTA DO SERVIDOR -> " + resp);
                return resp;
            } catch (Exception e) {
                Log.e(TAG, "SendCommentTask EXCECAO: " + e.getMessage(), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            Log.d(TAG, "SendCommentTask :: onPostExecute() -> resposta: " + response);
            if (response != null && (response.contains("sucess") || response.contains("success"))) {
                commentEdt.setText("");
                Toast.makeText(comments_activity.this, "Enviado com sucesso!", Toast.LENGTH_SHORT).show();
                
                String target = isExternal ? uniqueId : postId;
                Log.d(TAG, "Re-buscando comentarios atualizados para ID: " + target);
                new FetchCommentsTask().execute(target);
            } else {
                Log.e(TAG, "Falha ao enviar comentario!");
                Toast.makeText(comments_activity.this, "Erro ao enviar comentário", Toast.LENGTH_LONG).show();
            }
        }
    }

    public List<Comment> parseCommentsResponse(String jsonResponse) {
        Log.d(TAG, "--------------------------------------------------");
        Log.d(TAG, "parseCommentsResponse() INICIADO");
        Log.d(TAG, "RAW JSON RECEBIDO DO BACKEND: " + jsonResponse);

        List<Comment> commentsList = new ArrayList<Comment>();
        try {
            if (jsonResponse == null || jsonResponse.trim().length() == 0) {
                Log.e(TAG, "parseCommentsResponse :: JSON RECEBIDO ESTA NULO OU VAZIO!");
                return commentsList;
            }

            String trimmed = jsonResponse.trim();
            JSONArray commentsArray = null;

            if (trimmed.startsWith("{")) {
                Log.d(TAG, "JSON e um JSONObject RAIZ");
                JSONObject rootObject = new JSONObject(trimmed);
                if (rootObject.has("comments")) {
                    Log.d(TAG, "Chave 'comments' ENCONTRADA no JSONObject!");
                    commentsArray = rootObject.getJSONArray("comments");
                } else {
                    Log.w(TAG, "Chave 'comments' NAO encontrada no JSONObject raiz!");
                }
            } else if (trimmed.startsWith("[")) {
                Log.d(TAG, "JSON e um JSONArray DIRETO");
                commentsArray = new JSONArray(trimmed);
            }

            if (commentsArray != null) {
                Log.d(TAG, "QUANTIDADE DE COMENTARIOS NO ARRAY: " + commentsArray.length());

                for (int i = 0; i < commentsArray.length(); i++) {
                    JSONObject commentObj = commentsArray.getJSONObject(i);
                    Log.d(TAG, "--- PARSEANDO ITEM [" + i + "] ---");
                    Log.d(TAG, "Item raw: " + commentObj.toString());

                    // Ler IDs como String para evitar crash/overflow no Android
                    String rawCommentId = commentObj.optString("comment_id", String.valueOf(i));
                    String rawPostId = commentObj.optString("post_id", "0");
                    String commentUsername = commentObj.optString("username", "Anonimo");
                    String textComment = commentObj.optString("text_comment", "");

                    Log.d(TAG, "   > rawCommentId: " + rawCommentId);
                    Log.d(TAG, "   > rawPostId: " + rawPostId);
                    Log.d(TAG, "   > commentUsername: " + commentUsername);
                    Log.d(TAG, "   > textComment (raw): '" + textComment + "'");

                    int commentId = i;
                    int pId = 0;
                    try {
                        commentId = Integer.parseInt(rawCommentId);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "   > comment_id e uma String de 64-bits/Snowflake (" + rawCommentId + "). Usando fallback do loop ID: " + i);
                        commentId = i;
                    }

                    if (textComment == null || textComment.trim().length() == 0) {
                        Log.w(TAG, "   > ALERTA: text_comment veio VAZIO no JSON!");
                        textComment = "[Sem conteúdo]";
                    } else if (textComment.contains("<") && textComment.contains(">")) {
                        Log.d(TAG, "   > Sanitizando tags HTML do textComment...");
                        textComment = android.text.Html.fromHtml(textComment).toString().trim();
                        Log.d(TAG, "   > textComment (limpo): '" + textComment + "'");
                    }

                    Comment comment = new Comment(commentId, pId, commentUsername, textComment);
                    commentsList.add(comment);
                    Log.d(TAG, "   > Objeto Comment ADICIONADO na lista com sucesso!");
                }
            } else {
                Log.e(TAG, "commentsArray FICOU NULO! Nenhum array de comentarios extraido.");
            }
        } catch (JSONException e) {
            Log.e(TAG, "ERRO GRAVE DE PARSER JSON EM parseCommentsResponse: " + e.getMessage(), e);
        }

        Log.d(TAG, "TOTAL DE COMENTARIOS PROCESSADOS NA LISTA FINAL: " + commentsList.size());
        Log.d(TAG, "--------------------------------------------------");
        return commentsList;
    }

    public void popularLista(String jsonResult) {
        Log.d(TAG, "popularLista() CHAMADO -> iniciando parse e criacao do Adapter");
        List<Comment> comments = parseCommentsResponse(jsonResult);

        Log.d(TAG, "Criando CommentAdapter -> Tamanho da lista: " + comments.size());
        CommentAdapter adapter = new CommentAdapter(
            comments_activity.this, 
            comments, 
            isExternal, 
            externalUrl
        );

        if (lvComments != null) {
            lvComments.setAdapter(adapter);
            Log.d(TAG, "SUCCESS :: Adapter setado no ListView lvComments!");
        } else {
            Log.e(TAG, "CRITICAL ERROR :: lvComments (ListView) ESTA NULO!");
        }
    }

    private class FetchCommentsTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                String targetPostId = params[0];
                JSONObject jsonComment = new JSONObject();

                Log.d(TAG, "FetchCommentsTask :: doInBackground() -> targetPostId param: " + targetPostId);

                if (isExternal) {
                    jsonComment.put("post_id", uniqueId);
                    jsonComment.put("unique_id", uniqueId);
                    jsonComment.put("original_url", originalUrl);
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

                String endpoint = isExternal ? "/comments" : "/view-comments";
                String fullUrl = targetUrl + endpoint;

                Log.d(TAG, "FetchCommentsTask :: HTTP POST REQUEST");
                Log.d(TAG, " -> URL: " + fullUrl);
                Log.d(TAG, " -> PAYLOAD JSON: " + jsonComment.toString());

                String result = request.requestHTTP(fullUrl, "post", jsonComment, comments_activity.this);
                Log.d(TAG, "FetchCommentsTask :: RESPOSTA RAW RECEBIDA DA REDE -> " + result);
                return result;
            } catch (Exception e) {
                Log.e(TAG, "FetchCommentsTask EXCECAO DE REDE/JSON: " + e.getMessage(), e);
            }
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "FetchCommentsTask :: onPostExecute()");
            if (result != null && result.trim().length() > 0) {
                Log.d(TAG, "Tamanho do resultado recebido: " + result.length() + " chars. Chamando popularLista()...");
                popularLista(result);
            } else {
                Log.e(TAG, "FetchCommentsTask :: Resultado veio VAZIO ou NULO!");
                Toast.makeText(comments_activity.this, "Erro ao carregar comentários", Toast.LENGTH_SHORT).show();
            }
        }
    }
}