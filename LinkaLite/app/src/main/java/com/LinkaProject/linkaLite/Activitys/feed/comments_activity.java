package com.LinkaProject.linkaLite;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class comments_activity extends BaseActivity {

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
    private ImageView postAvatar;
    private View postHeader;
    private TextView postUsername;
    private TextView txtCommunity;
    private TextView postDate;
    private TextView postText;
    private TextView txtCommentCount;
    private ImageView imgPost;
    private View containerMedia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

            if (externalUrl == null) externalUrl = "";
            if (uniqueId == null) uniqueId = "";
            if (originalUrl == null) originalUrl = "";
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

        View fixedHeader = findViewById(R.id.postHeader);
        if (fixedHeader != null && lvComments != null) {
            android.view.ViewGroup parent = (android.view.ViewGroup) fixedHeader.getParent();
            if (parent != null) {
                parent.removeView(fixedHeader);
            }
            fixedHeader.setLayoutParams(new android.widget.AbsListView.LayoutParams(
                android.widget.AbsListView.LayoutParams.FILL_PARENT,
                android.widget.AbsListView.LayoutParams.WRAP_CONTENT
            ));
            postHeader = fixedHeader;
            lvComments.addHeaderView(postHeader, null, false);
        }

        setupPostHeader(intent);

        boolean isGuest = intent != null && intent.getBooleanExtra("isGuest", false);

        if (isGuest && layoutComment != null && layoutGuest != null) {
            layoutComment.setVisibility(View.GONE);
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

                    new SendCommentTask().execute(commentJson.toString());
                } catch (JSONException e) {
                    Toast.makeText(comments_activity.this, "Erro ao criar JSON", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (isExternal && uniqueId.length() > 0) {
            new FetchCommentsTask().execute(uniqueId);
        } else if (postId != null && postId.trim().length() > 0) {
            new FetchCommentsTask().execute(postId);
        }
    }

    private void setupPostHeader(Intent intent) {
        if (postHeader == null) return;

        postUsername = (TextView) postHeader.findViewById(R.id.postUsername);
        txtCommunity = (TextView) postHeader.findViewById(R.id.txtCommunity);
        postDate = (TextView) postHeader.findViewById(R.id.postDate);
        postText = (TextView) postHeader.findViewById(R.id.postText);
        txtCommentCount = (TextView) postHeader.findViewById(R.id.txtCommentCount);
        postAvatar = (ImageView) postHeader.findViewById(R.id.postAvatar);
        containerMedia = postHeader.findViewById(R.id.containerMedia);
        imgPost = (ImageView) postHeader.findViewById(R.id.imgPost);
        ImageButton btnComments = (ImageButton) postHeader.findViewById(R.id.btnComments);
        Button btnPostOptions = (Button) postHeader.findViewById(R.id.btnPostOptions);
        if (btnComments != null) btnComments.setEnabled(false);
        if (btnPostOptions != null) btnPostOptions.setVisibility(View.GONE);
        String urlImage = "";
        if (intent != null) {
            String author = intent.getStringExtra("post_author");
            String date = intent.getStringExtra("post_date");
            String content = intent.getStringExtra("post_text");
            String community = intent.getStringExtra("post_community");
            int commentCount = intent.getIntExtra("comment_count", 0);
            String avatarUrl = intent.getStringExtra("avatar_url");
            if (content != null) {
                int idx = content.indexOf("[IMAGE]");
                if (idx != -1) {
                    urlImage = content.substring(idx + "[IMAGE]".length()).trim();
                    content = content.substring(0, idx).trim();
                }
            }
            if (!urlImage.equals("") && imgPost != null) {
                if (containerMedia != null) {
                    int maxPx = (int) (180 * getResources().getDisplayMetrics().density);
                    ViewGroup.LayoutParams lp = containerMedia.getLayoutParams();
                    lp.height = maxPx;
                    containerMedia.setLayoutParams(lp);
                    containerMedia.setVisibility(View.VISIBLE);
                }
                imgPost.setVisibility(View.VISIBLE);
                ImageLoader imageLoader = new ImageLoader();
                imageLoader.LoadImageUrl(urlImage, imgPost);
            }
            if (avatarUrl == null) avatarUrl = "";
            if (!avatarUrl.equals("")) {
                ImageLoader imageLoader = new ImageLoader();
                imageLoader.LoadImageUrl(avatarUrl, postAvatar);
            }
            if (postUsername != null) {
                postUsername.setText(author != null ? author : "");
            }
            if (postDate != null) {
                if (date != null && date.trim().length() > 0) {
                    postDate.setText(date);
                    postDate.setVisibility(View.VISIBLE);
                } else {
                    postDate.setVisibility(View.GONE);
                }
            }
            if (postText != null) {
                if (content != null && content.trim().length() > 0) {
                    postText.setText(content);
                    postText.setVisibility(View.VISIBLE);
                } else {
                    postText.setVisibility(View.GONE);
                }
            }
            if (txtCommunity != null) {
                if (community != null && community.trim().length() > 0) {
                    txtCommunity.setText(" > " + community);
                    txtCommunity.setVisibility(View.VISIBLE);
                } else {
                    txtCommunity.setVisibility(View.GONE);
                }
            }
            if (txtCommentCount != null) {
                txtCommentCount.setText(String.valueOf(commentCount));
            }
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

                String endpoint = "/comments";
                String fullUrl = targetUrl + endpoint;

                return request.requestHTTP(fullUrl, "post", commentJson, comments_activity.this);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            if (response != null && (response.contains("sucess") || response.contains("success"))) {
                commentEdt.setText("");
                Toast.makeText(comments_activity.this, "Commented with sucess!", Toast.LENGTH_SHORT).show();

                String target = isExternal ? uniqueId : postId;
                new FetchCommentsTask().execute(target);
            } else {
                Toast.makeText(comments_activity.this, "Erro in comment send", Toast.LENGTH_LONG).show();
            }
        }
    }

    public List<Comment> parseCommentsResponse(String jsonResponse) {
        List<Comment> commentsList = new ArrayList<Comment>();
        try {
            if (jsonResponse == null || jsonResponse.trim().length() == 0) {
                return commentsList;
            }

            String trimmed = jsonResponse.trim();
            JSONArray commentsArray = null;

            if (trimmed.startsWith("{")) {
                JSONObject rootObject = new JSONObject(trimmed);
                if (rootObject.has("comments")) {
                    commentsArray = rootObject.getJSONArray("comments");
                }
            } else if (trimmed.startsWith("[")) {
                commentsArray = new JSONArray(trimmed);
            }

            if (commentsArray != null) {
                for (int i = 0; i < commentsArray.length(); i++) {
                    JSONObject commentObj = commentsArray.getJSONObject(i);

                    String rawCommentId = commentObj.optString("comment_id", String.valueOf(i));
                    String commentUsername = commentObj.optString("username", "Anonimo");
                    String textComment = commentObj.optString("text_comment", "");

                    int commentId = i;
                    int pId = 0;
                    try {
                        commentId = Integer.parseInt(rawCommentId);
                    } catch (NumberFormatException e) {
                        commentId = i;
                    }

                    if (textComment == null || textComment.trim().length() == 0) {
                        textComment = "[No content]";
                    } else if (textComment.contains("<") && textComment.contains(">")) {
                        textComment = android.text.Html.fromHtml(textComment).toString().trim();
                    }

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

        if (txtCommentCount != null) {
            txtCommentCount.setText(String.valueOf(comments.size()));
        }

        CommentAdapter adapter = new CommentAdapter(
            comments_activity.this,
            comments,
            isExternal,
            externalUrl
        );

        if (lvComments != null) {
            lvComments.setAdapter(adapter);
        }
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

                String endpoint = "/view-comments";
                String fullUrl = targetUrl + endpoint;

                return request.requestHTTP(fullUrl, "post", jsonComment, comments_activity.this);
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