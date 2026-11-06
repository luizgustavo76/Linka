package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class newPost extends Activity {
    private static final String TAG = "Linka_Upload";
    private static final int PICK_IMAGE_REQUEST = 1001;

    private TextView newPostText;
    private EditText editPostContent;
    private Button btnSend;
    private Button btnImage;
    private Button btnLogin;
    private LinearLayout layoutGuest;
    private boolean isGuest;

    private CheckBox btnLinka;
    private CheckBox btnReddit;
    private CheckBox btnMastodon;
    private CheckBox btnBluesky;

    private RelativeLayout containerMediaPreview;
    private ImageButton btnRemoveMedia;
    private GifImageView imgMediaPreview;

    private LinearLayout containerRepostPreview;
    private TextView txtRepostOriginalUser;
    private TextView txtRepostOriginalText;
    private ImageButton btnRemoveRepost;

    private String uploadedImageUrl = "";
    private int repostPostId = -1;

    private static final String CFG_EXTERNAL_FILE = "config-external.cfg";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.new_post_activity);

        layoutGuest = (LinearLayout) findViewById(R.id.layoutGuest);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        newPostText = (TextView) findViewById(R.id.newPostText);
        editPostContent = (EditText) findViewById(R.id.editPostContent);
        btnSend = (Button) findViewById(R.id.btnSend);
        btnImage = (Button) findViewById(R.id.btnImage);

        btnLinka = (CheckBox) findViewById(R.id.btnLinka);
        btnReddit = (CheckBox) findViewById(R.id.btnReddit);
        btnMastodon = (CheckBox) findViewById(R.id.btnMastodon);
        btnBluesky = (CheckBox) findViewById(R.id.btnBluesky);

        containerMediaPreview = (RelativeLayout) findViewById(R.id.containerMediaPreview);
        btnRemoveMedia = (ImageButton) findViewById(R.id.btnRemoveMedia);
        imgMediaPreview = (GifImageView) findViewById(R.id.imgMediaPreview);

        containerRepostPreview = (LinearLayout) findViewById(R.id.containerRepostPreview);
        txtRepostOriginalUser = (TextView) findViewById(R.id.txtRepostOriginalUser);
        txtRepostOriginalText = (TextView) findViewById(R.id.txtRepostOriginalText);
        btnRemoveRepost = (ImageButton) findViewById(R.id.btnRemoveRepost);

        if (btnLinka != null) {
            btnLinka.setChecked(true);
        }

        isGuest = getIntent().getBooleanExtra("isGuest", false);
        repostPostId = getIntent().getIntExtra("repostPostId", -1);
        String repostUser = getIntent().getStringExtra("repostUser");
        String repostText = getIntent().getStringExtra("repostText");

        if (repostPostId != -1 && containerRepostPreview != null) {
            containerRepostPreview.setVisibility(View.VISIBLE);
            if (txtRepostOriginalUser != null) txtRepostOriginalUser.setText(repostUser);
            if (txtRepostOriginalText != null) txtRepostOriginalText.setText(repostText);
        }

        if (btnRemoveRepost != null) {
            btnRemoveRepost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    repostPostId = -1;
                    if (containerRepostPreview != null) containerRepostPreview.setVisibility(View.GONE);
                }
            });
        }

        if (btnRemoveMedia != null) {
            btnRemoveMedia.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    uploadedImageUrl = "";
                    if (containerMediaPreview != null) containerMediaPreview.setVisibility(View.GONE);
                }
            });
        }

        if (isGuest) {
            if (layoutGuest != null) layoutGuest.setVisibility(View.VISIBLE);
            if (newPostText != null) newPostText.setVisibility(View.GONE);
            if (editPostContent != null) editPostContent.setVisibility(View.GONE);
            if (btnSend != null) btnSend.setVisibility(View.GONE);
            if (btnImage != null) btnImage.setVisibility(View.GONE);

            if (btnLogin != null) {
                btnLogin.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(newPost.this, LoginActivity.class);
                        startActivity(intent);
                    }
                });
            }
        } else {
            if (layoutGuest != null) {
                layoutGuest.setVisibility(View.GONE);
            }
            if (newPostText != null) newPostText.setVisibility(View.VISIBLE);
            if (editPostContent != null) editPostContent.setVisibility(View.VISIBLE);
            if (btnSend != null) btnSend.setVisibility(View.VISIBLE);
            if (btnImage != null) btnImage.setVisibility(View.VISIBLE);

            setupPlatformListener(btnMastodon, "MASTODON");
            setupPlatformListener(btnReddit, "REDDIT");
            setupPlatformListener(btnBluesky, "BLUESKY");

            if (btnSend != null) {
                btnSend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String postContent = editPostContent.getText().toString();
                        if (postContent.trim().length() == 0 && uploadedImageUrl.length() == 0 && repostPostId == -1) {
                            Toast.makeText(newPost.this, "write something!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        btnSend.setEnabled(false);
                        new SendPostTask().execute(postContent);
                    }
                });
            }

            if (btnImage != null) {
                btnImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openGallery();
                    }
                });
            }
        }
    }

    private void setupPlatformListener(final CheckBox checkBox, final String platform) {
        if (checkBox == null) return;

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!hasValidCredentials(platform)) {
                        checkBox.setChecked(false);
                        Toast.makeText(newPost.this, "Login required for " + platform, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(newPost.this, ExternalLogin.class);
                        intent.putExtra("platform", platform);
                        startActivity(intent);
                    }
                }
            }
        });
    }

    private boolean hasValidCredentials(String platform) {
        config cfg = new config();
        String jsonRaw = cfg.loadCfgAsJson(newPost.this, CFG_EXTERNAL_FILE);

        if (jsonRaw != null && jsonRaw.length() > 0) {
            try {
                JSONObject jsonCfg = new JSONObject(jsonRaw);
                JSONObject section = jsonCfg.optJSONObject(platform);
                if (section != null) {
                    String token = section.optString("token", "");
                    return token.trim().length() > 0;
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error checking credentials: " + e.getMessage());
            }
        }
        return false;
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
            if (btnImage != null) btnImage.setEnabled(false);
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
                Log.e(TAG, "Exception during upload", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (btnImage != null) btnImage.setEnabled(true);
            if (result != null && result.length() > 0) {
                try {
                    JSONObject jsonResponse = new JSONObject(result);
                    uploadedImageUrl = jsonResponse.optString("image_url", "");

                    if (uploadedImageUrl.length() > 0) {
                        if (containerMediaPreview != null) containerMediaPreview.setVisibility(View.VISIBLE);
                        Toast.makeText(newPost.this, "Image uploaded!", Toast.LENGTH_SHORT).show();
                        ImageLoader imageLoader = new ImageLoader();
                        imageLoader.LoadImageUrl(uploadedImageUrl, imgMediaPreview);
                    } else {
                        String serverError = jsonResponse.optString("error", "Upload failed");
                        Toast.makeText(newPost.this, serverError, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(newPost.this, "Response parse error", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(newPost.this, "Upload error", Toast.LENGTH_LONG).show();
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
                String baseUrl = (server != null) ? server.optString("url", "http://linkaProject.pythonanywhere.com") : "http://linkaProject.pythonanywhere.com";
                String username = (fastLogin != null) ? fastLogin.optString("username", "") : "";
                String token = (fastLogin != null) ? fastLogin.optString("token_session", "") : "";

                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }

                String rawExternal = cfg.loadCfgAsJson(newPost.this, CFG_EXTERNAL_FILE);
                JSONObject jsonExt = (rawExternal != null && rawExternal.length() > 0) ? new JSONObject(rawExternal) : new JSONObject();

                String finalContent = postTextValue;
                if (uploadedImageUrl.length() > 0) {
                    finalContent += "\n[IMAGE]" + uploadedImageUrl;
                }

                String mainResult = "";
                if (btnLinka != null && btnLinka.isChecked()) {
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("username", username);
                    jsonResponse.put("token_session", token);
                    jsonResponse.put("text_post", finalContent);
                    jsonResponse.put("datetime", TimeUtils.getDateTime());
                    jsonResponse.put("external_credentials", jsonExt);
                    if (repostPostId != -1) {
                        jsonResponse.put("repost_id", repostPostId);
                    }

                    mainResult = request.requestHTTP(baseUrl + "/new", "post", jsonResponse, newPost.this);
                }

                dispatchExternalNetwork(btnMastodon, "MASTODON", baseUrl, finalContent, jsonExt, cfg);
                dispatchExternalNetwork(btnBluesky, "BLUESKY", baseUrl, finalContent, jsonExt, cfg);
                dispatchExternalNetwork(btnReddit, "REDDIT", baseUrl, finalContent, jsonExt, cfg);

                return mainResult.length() > 0 ? mainResult : "ok";
            } catch (Exception e) {
                Log.e(TAG, "Error sending post", e);
            }
            return null;
        }

        private void dispatchExternalNetwork(CheckBox checkBox, String networkKey, String defaultUrl, String postText, JSONObject jsonExt, config cfg) {
            if (checkBox == null || !checkBox.isChecked()) return;

            try {
                JSONObject netObj = jsonExt.optJSONObject(networkKey);
                String token = (netObj != null) ? netObj.optString("token", "") : "";
                String handle = (netObj != null) ? netObj.optString("username", "") : "";
                String customUrl = (netObj != null) ? netObj.optString("url", "") : "";

                String proxyUrl = (customUrl != null && customUrl.trim().length() > 0) ? customUrl.trim() : defaultUrl;
                if (proxyUrl.endsWith("/")) {
                    proxyUrl = proxyUrl.substring(0, proxyUrl.length() - 1);
                }

                String endpoint = proxyUrl + "/feed/" + networkKey.toLowerCase() + "/new";

                JSONObject payload = new JSONObject();
                payload.put("access_token", token);
                payload.put("acess_token", token);
                payload.put("handle", handle);
                payload.put("username", handle);
                payload.put("text_post", postText);

                String response = request.requestHTTP(endpoint, "post", payload, newPost.this);
            } catch (Exception e) {
                Log.e(TAG, "Failed to dispatch to " + networkKey, e);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (btnSend != null) btnSend.setEnabled(true);
            if (result != null && result.length() > 0) {
                Toast.makeText(newPost.this, "post was sent!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(newPost.this, "Error in post sending.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}