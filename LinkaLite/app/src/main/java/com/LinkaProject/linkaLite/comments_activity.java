package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class comments_activity extends Activity {

    private ListView lvComments;
    private String postId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comments_activity);

        lvComments = (ListView) findViewById(R.id.lvComments);

        // Resgata o post_id como STRING enviado da HomeActivity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            postId = extras.getString("post_id");
        }

        if (postId != null && !postId.isEmpty()) {
            new FetchCommentsTask().execute("http://linkaProject.pythonanywhere.com/comments?post_id=" + postId);
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
                    String username = commentObj.optString("username", "IdontKnowMyName;)");
                    String textComment = commentObj.optString("text_comment", "");

                    Comment comment = new Comment(commentId, pId, username, textComment);
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

    // Task para buscar comentários sem travar a UI (Compatível com Android 1.5)
    private class FetchCommentsTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urls[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
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
                if (connection != null) connection.disconnect();
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