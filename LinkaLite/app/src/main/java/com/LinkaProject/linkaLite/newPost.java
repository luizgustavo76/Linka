package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class newPost extends Activity {
    private static final int PICK_IMAGE_REQUEST = 1001;
    private TextView newPostText;
    private EditText textPost;
    private Button btnSend;
    private Button btnImage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.new_post_activity);

        newPostText = (TextView) findViewById(R.id.newPostText);
        textPost = (EditText) findViewById(R.id.textPost);
        btnSend = (Button) findViewById(R.id.btnSend);
        btnImage = (Button) findViewById(R.id.btnImage);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String postContent = textPost.getText().toString();
                if (postContent.trim().isEmpty()) {
                    Toast.makeText(newPost.this, "write something!", Toast.LENGTH_SHORT).show();
                    return;
                }
                btnSend.setEnabled(false);
                new SendPostTask().execute(postContent);
            }
        });

        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            btnImage.setEnabled(false);
            new UploadImageTask().execute(selectedImageUri);
        }
    }

    private class UploadImageTask extends AsyncTask<Uri, Void, String> {
        @Override
        protected String doInBackground(Uri... uris) {
            Uri selectedImageUri = uris[0];
            try {
                config cfg = new config();
                String rawCfg = cfg.loadCfgAsJson(newPost.this, "config.cfg");
                JSONObject jsonCfg = new JSONObject(rawCfg);
                JSONObject server = jsonCfg.optJSONObject("SERVER");
                String url = (server != null) ? server.optString("url", "http://linkaProject.pythonanywhere.com") : "http://linkaProject.pythonanywhere.com";

                if (url.endsWith("/")) {
                    url = url.substring(0, url.length() - 1);
                }

                String fullUrl = url + "/upload-image";
                return UploadTask.uploadProfilePicture(newPost.this, selectedImageUri, fullUrl);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            btnImage.setEnabled(true);
            if (result != null && !result.isEmpty()) {
                try {
                    JSONObject jsonResponse = new JSONObject(result);
                    String imageUrl = jsonResponse.optString("image_url", "");

                    if (!imageUrl.isEmpty()) {
                        textPost.append("\n[IMAGE]" + imageUrl);
                        Toast.makeText(newPost.this, "Image uploaded!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(newPost.this, "Invalid response format", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(newPost.this, "Error parsing server response", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(newPost.this, "Error in upload image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class SendPostTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String postTextValue = params[0];
            try {
                config cfg = new config();
                String rawCfg = cfg.loadCfgAsJson(newPost.this, "config.cfg");
                JSONObject jsonCfg = new JSONObject(rawCfg);
                JSONObject fastLogin = jsonCfg.optJSONObject("FAST_LOGIN");
                JSONObject server = jsonCfg.optJSONObject("SERVER");
                String url = (server != null) ? server.optString("url", "http://linkaProject.pythonanywhere.com") : "http://linkaProject.pythonanywhere.com";
                String username = (fastLogin != null) ? fastLogin.optString("username", "") : "";
                String token = (fastLogin != null) ? fastLogin.optString("token_session", "") : "";

                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("username", username);
                jsonResponse.put("token_session", token);
                jsonResponse.put("text_post", postTextValue);
                jsonResponse.put("datetime", TimeUtils.getDateTime());

                if (url.endsWith("/")) {
                    url = url.substring(0, url.length() - 1);
                }
                return request.requestHTTP(url + "/new", "post", jsonResponse, newPost.this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            btnSend.setEnabled(true);
            if (result != null && !result.isEmpty()) {
                Toast.makeText(newPost.this, "post was send!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(newPost.this, "Error in post sending.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}