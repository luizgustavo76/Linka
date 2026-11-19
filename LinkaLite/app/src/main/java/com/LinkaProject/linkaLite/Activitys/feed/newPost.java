package com.LinkaProject.linkaLite;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class newPost extends BaseActivity {
    private static final String TAG = "Linka_Upload";
    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int PICK_VIDEO_REQUEST = 1002;
    private static final String CFG_EXTERNAL_FILE = "config-external.cfg";

    private TextView newPostText;
    private EditText editPostContent;
    private Button btnSend;
    private Button btnImage;
    private Button btnVideo;
    private Button btnLogin;
    private LinearLayout layoutGuest;
    private boolean isGuest;
    private Button btnAddOption;
    private CheckBox btnLinka;
    private CheckBox btnReddit;
    private CheckBox btnMastodon;
    private CheckBox btnBluesky;
    private String postId = "";
    private RelativeLayout containerMediaPreview;
    private ImageButton btnRemoveMedia;
    private GifImageView imgMediaPreview;

    private RelativeLayout containerRepostPreview;
    private TextView txtRepostOriginalUser;
    private TextView txtRepostOriginalText;
    private ImageButton btnRemoveRepost;
    private ImageView avatarRepost;
    private ImageView imgMedia;
    private boolean isEdited;
    private String uploadedMediaUrl = "";
    private String uploadedMediaType = "";
    private int repostPostId = -1;
    private String repostUser = "";
    private String repostText = "";
    private String repostDatetime = "";
    private String repostMediaUrl = "";
    private int repostMediaWidth = 0;
    private int repostMediaHeight = 0;

    private LinearLayout containerPoll;
    private Button btnPoll;

    private void addPollOption() {
        if (containerPoll == null) return;

        int currentOptions = containerPoll.getChildCount() - 1;
        if (currentOptions >= 8) {
            Toast.makeText(newPost.this, "Max 8 options", Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        final View optionView = inflater.inflate(R.layout.item_poll, containerPoll, false);

        EditText editOption = (EditText) optionView.findViewById(R.id.editOption);
        TextView txtClose = (TextView) optionView.findViewById(R.id.txtClose);

        editOption.setHint("Option " + (currentOptions + 1));

        txtClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (containerPoll.getChildCount() - 1 > 2) {
                    containerPoll.removeView(optionView);
                } else {
                    Toast.makeText(newPost.this, "Minimum 2 options", Toast.LENGTH_SHORT).show();
                }
            }
        });

        int insertIndex = Math.max(0, containerPoll.getChildCount() - 1);
        containerPoll.addView(optionView, insertIndex);
    }

    private JSONObject buildPollPayload(String postTitle) {
        if (containerPoll == null || containerPoll.getVisibility() != View.VISIBLE) {
            return null;
        }

        JSONArray optionsArray = new JSONArray();
        int count = containerPoll.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = containerPoll.getChildAt(i);
            EditText editOption = (EditText) child.findViewById(R.id.editOption);
            if (editOption != null) {
                String optText = editOption.getText().toString().trim();
                if (optText.length() > 0) {
                    optionsArray.put(optText);
                }
            }
        }

        if (optionsArray.length() < 2) {
            return null;
        }

        JSONObject pollObj = new JSONObject();
        try {
            pollObj.put("title", postTitle.trim().length() > 0 ? postTitle.trim() : "Poll");
            pollObj.put("options", optionsArray);
            pollObj.put("duration_hours", 24);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return pollObj;
    }

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
        btnVideo = (Button) findViewById(R.id.btnVideo);
        containerPoll = (LinearLayout) findViewById(R.id.containerPoll);
        btnLinka = (CheckBox) findViewById(R.id.btnLinka);
        btnReddit = (CheckBox) findViewById(R.id.btnReddit);
        btnMastodon = (CheckBox) findViewById(R.id.btnMastodon);
        btnBluesky = (CheckBox) findViewById(R.id.btnBluesky);
        imgMedia = (ImageView) findViewById(R.id.imgMedia);
        avatarRepost = (ImageView) findViewById(R.id.avatarRepost);
        containerMediaPreview = (RelativeLayout) findViewById(R.id.containerMediaPreview);
        btnRemoveMedia = (ImageButton) findViewById(R.id.btnRemoveMedia);
        imgMediaPreview = (GifImageView) findViewById(R.id.imgMediaPreview);
        btnPoll = (Button) findViewById(R.id.btnPoll);
        containerRepostPreview = (RelativeLayout) findViewById(R.id.containerRepostPreview);
        txtRepostOriginalUser = (TextView) findViewById(R.id.txtRepostOriginalUser);
        txtRepostOriginalText = (TextView) findViewById(R.id.txtRepostOriginalText);
        btnRemoveRepost = (ImageButton) findViewById(R.id.btnRemoveRepost);
        btnAddOption = (Button) findViewById(R.id.btnAddOption);

        if (btnLinka != null) {
            btnLinka.setChecked(true);
        }

        if (btnPoll != null) {
            btnPoll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (containerPoll.getVisibility() == View.VISIBLE) {
                        containerPoll.setVisibility(View.GONE);
                        btnPoll.setText("Create poll");
                    } else {
                        containerPoll.setVisibility(View.VISIBLE);
                        btnPoll.setText("Remove poll");
                        if (containerPoll.getChildCount() <= 1) {
                            addPollOption();
                            addPollOption();
                        }
                    }
                }
            });
        }

        if (btnAddOption != null) {
            btnAddOption.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addPollOption();
                }
            });
        }

        isGuest = getIntent().getBooleanExtra("isGuest", false);
        repostPostId = getIntent().getIntExtra("repostPostId", -1);
        repostUser = getIntent().getStringExtra("repostUser");
        repostText = getIntent().getStringExtra("repostText");
        repostDatetime = getIntent().getStringExtra("repostDatetime");
        isEdited = getIntent().getBooleanExtra("isEdited", false);
        int idInt = getIntent().getIntExtra("postId", -1);
        postId = (idInt != -1) ? String.valueOf(idInt) : getIntent().getStringExtra("postId");
        String textPost = getIntent().getStringExtra("textPost");
        String repostAvatarUrl = getIntent().getStringExtra("repostAvatarUrl");
        repostMediaUrl = getIntent().getStringExtra("repostMediaUrl");
        String repostMediaHeightStr = getIntent().getStringExtra("repostMediaHeight");
        String repostMediaWidthStr = getIntent().getStringExtra("repostMediaWidth");

        repostMediaHeight = getIntent().getIntExtra("repostMediaHeight", 0);
        repostMediaWidth = getIntent().getIntExtra("repostMediaWidth", 0);
        try {
            if (repostMediaHeight == 0 && repostMediaHeightStr != null) {
                repostMediaHeight = Integer.parseInt(repostMediaHeightStr);
            }
            if (repostMediaWidth == 0 && repostMediaWidthStr != null) {
                repostMediaWidth = Integer.parseInt(repostMediaWidthStr);
            }
        } catch (Exception ignored) {}
        if (isEdited && textPost != null && !textPost.equals("")) {
            editPostContent.setText(textPost);
        }
        if (repostPostId != -1 && containerRepostPreview != null) {
            containerRepostPreview.setVisibility(View.VISIBLE);
            if (txtRepostOriginalUser != null) txtRepostOriginalUser.setText(repostUser);
            if (txtRepostOriginalText != null) txtRepostOriginalText.setText(repostText);
            if (repostMediaUrl != null && repostMediaUrl.length() > 0 && imgMedia != null) {
                ImageLoader imageLoader = new ImageLoader();
                if (repostMediaWidth > 0 && repostMediaHeight > 0) {
                    ViewGroup.LayoutParams params = imgMedia.getLayoutParams();
                    params.width = repostMediaWidth;
                    params.height = repostMediaHeight;
                    imgMedia.setLayoutParams(params);
                }
                imageLoader.LoadImageUrl(repostMediaUrl, imgMedia);
            }
            if (repostAvatarUrl != null && repostAvatarUrl.length() > 0 && avatarRepost != null) {
                ImageLoader imageLoader = new ImageLoader();
                imageLoader.LoadImageUrl(repostAvatarUrl, avatarRepost);
            }
        }

        if (btnRemoveRepost != null) {
            btnRemoveRepost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    repostPostId = -1;
                    repostUser = "";
                    repostText = "";
                    repostDatetime = "";
                    repostMediaUrl = "";
                    repostMediaWidth = 0;
                    repostMediaHeight = 0;
                    if (containerRepostPreview != null) containerRepostPreview.setVisibility(View.GONE);
                }
            });
        }

        if (btnRemoveMedia != null) {
            btnRemoveMedia.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    uploadedMediaUrl = "";
                    uploadedMediaType = "";
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
            if (btnVideo != null) btnVideo.setVisibility(View.GONE);

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
            if (layoutGuest != null) layoutGuest.setVisibility(View.GONE);
            if (newPostText != null) newPostText.setVisibility(View.VISIBLE);
            if (editPostContent != null) editPostContent.setVisibility(View.VISIBLE);
            if (btnSend != null) btnSend.setVisibility(View.VISIBLE);
            if (btnImage != null) btnImage.setVisibility(View.VISIBLE);
            if (btnVideo != null) btnVideo.setVisibility(View.VISIBLE);

            setupPlatformListener(btnMastodon, "MASTODON");
            setupPlatformListener(btnReddit, "REDDIT");
            setupPlatformListener(btnBluesky, "BLUESKY");

            if (btnSend != null) {
                btnSend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String postContent = editPostContent.getText().toString();
                        boolean pollVisible = (containerPoll != null && containerPoll.getVisibility() == View.VISIBLE);
                        JSONObject pollPayload = buildPollPayload(postContent);

                        if (pollVisible && pollPayload == null) {
                            Toast.makeText(newPost.this, "Fill at least 2 poll options!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (postContent.trim().length() == 0 && uploadedMediaUrl.length() == 0 && repostPostId == -1 && pollPayload == null) {
                            Toast.makeText(newPost.this, "write something!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        btnSend.setEnabled(false);
                        new SendPostTask(pollPayload).execute(postContent);
                    }
                });
            }

            if (btnVideo != null) {
                btnVideo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openVideoPicker();
                    }
                });
            }

            if (btnImage != null) {
                btnImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (btnVideo != null) {
                            openImagePicker();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(newPost.this);
                            builder.setTitle("Attach Media");
                            builder.setItems(new CharSequence[]{"Image / GIF", "Video (MP4)"}, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == 0) {
                                        openImagePicker();
                                    } else {
                                        openVideoPicker();
                                    }
                                }
                            });
                            builder.show();
                        }
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

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(Intent.createChooser(intent, "Select Video"), PICK_VIDEO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedUri = data.getData();
            if (requestCode == PICK_IMAGE_REQUEST) {
                if (btnImage != null) btnImage.setEnabled(false);
                if (btnVideo != null) btnVideo.setEnabled(false);
                new UploadMediaTask(false).execute(selectedUri);
            } else if (requestCode == PICK_VIDEO_REQUEST) {
                if (btnImage != null) btnImage.setEnabled(false);
                if (btnVideo != null) btnVideo.setEnabled(false);
                Toast.makeText(newPost.this, "Uploading video...", Toast.LENGTH_SHORT).show();
                new UploadMediaTask(true).execute(selectedUri);
            }
        }
    }

    private class UploadMediaTask extends AsyncTask<Uri, Void, String> {
        private boolean isVideo;

        public UploadMediaTask(boolean isVideo) {
            this.isVideo = isVideo;
        }

        @Override
        protected String doInBackground(Uri... uris) {
            Uri selectedUri = uris[0];
            try {
                config cfg = new config();
                String rawCfg = cfg.loadCfgAsJson(newPost.this, "config.cfg");
                JSONObject jsonCfg = new JSONObject(rawCfg);
                JSONObject server = jsonCfg.optJSONObject("SERVER");
                String url = (server != null) ? server.optString("url", "http://linkaProject.pythonanywhere.com") : "http://linkaProject.pythonanywhere.com";

                if (url.endsWith("/")) {
                    url = url.substring(0, url.length() - 1);
                }

                String route = isVideo ? "/upload-video" : "/upload-image";
                return UploadTask.uploadProfilePicture(newPost.this, selectedUri, url + route);
            } catch (Exception e) {
                Log.e(TAG, "Exception during upload", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (btnImage != null) btnImage.setEnabled(true);
            if (btnVideo != null) btnVideo.setEnabled(true);
            if (result != null && result.length() > 0) {
                try {
                    JSONObject jsonResponse = new JSONObject(result);
                    if (isVideo) {
                        uploadedMediaUrl = jsonResponse.optString("video_url", jsonResponse.optString("media_url", ""));
                        uploadedMediaType = "video";
                    } else {
                        uploadedMediaUrl = jsonResponse.optString("image_url", jsonResponse.optString("media_url", ""));
                        uploadedMediaType = "image";
                    }

                    if (uploadedMediaUrl.length() > 0) {
                        if (containerMediaPreview != null) containerMediaPreview.setVisibility(View.VISIBLE);
                        if (isVideo) {
                            Toast.makeText(newPost.this, "Video uploaded!", Toast.LENGTH_SHORT).show();
                            String thumbUrl = jsonResponse.optString("thumbnail_url", "");
                            if (thumbUrl.length() > 0) {
                                ImageLoader imageLoader = new ImageLoader();
                                imageLoader.LoadImageUrl(thumbUrl, imgMediaPreview);
                            } else {
                                imgMediaPreview.setImageResource(android.R.drawable.ic_media_play);
                            }
                        } else {
                            Toast.makeText(newPost.this, "Image uploaded!", Toast.LENGTH_SHORT).show();
                            ImageLoader imageLoader = new ImageLoader();
                            imageLoader.LoadImageUrl(uploadedMediaUrl, imgMediaPreview);
                        }
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
        private JSONObject pollPayload;

        public SendPostTask(JSONObject pollPayload) {
            this.pollPayload = pollPayload;
        }

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
                if (uploadedMediaUrl.length() > 0 && "image".equals(uploadedMediaType)) {
                    finalContent += "\n[IMAGE]" + uploadedMediaUrl;
                }

                String mainResult = "";
                if (btnLinka != null && btnLinka.isChecked()) {
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("username", username);
                    jsonResponse.put("token_session", token);
                    jsonResponse.put("text_post", finalContent);
                    jsonResponse.put("datetime", TimeUtils.getDateTime());
                    jsonResponse.put("external_credentials", jsonExt);

                    if (uploadedMediaUrl.length() > 0) {
                        jsonResponse.put("media", uploadedMediaUrl);
                        jsonResponse.put("media_type", uploadedMediaType);
                    }

                    if (repostPostId != -1) {
                        JSONObject repostObj = new JSONObject();
                        repostObj.put("original_id", repostPostId);
                        repostObj.put("username", repostUser != null ? repostUser : "");
                        repostObj.put("text_post", repostText != null ? repostText : "");
                        repostObj.put("datetime", repostDatetime != null ? repostDatetime : "");

                        if (repostMediaUrl != null && repostMediaUrl.length() > 0) {
                            JSONObject mediaAtt = new JSONObject();
                            mediaAtt.put("media_url", repostMediaUrl);
                            mediaAtt.put("width", repostMediaWidth);
                            mediaAtt.put("height", repostMediaHeight);
                            repostObj.put("media_attachments", mediaAtt);
                        }
                        jsonResponse.put("repost", repostObj);
                    }

                    if (pollPayload != null) {
                        jsonResponse.put("poll", pollPayload);
                    }
                    if (isEdited){
                        jsonResponse.put("post_id", postId);
                        mainResult = request.requestHTTP(baseUrl + "/edit-post", "post", jsonResponse, newPost.this);                        
                    }else{
                        mainResult = request.requestHTTP(baseUrl + "/new", "post", jsonResponse, newPost.this);
                    }
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

                request.requestHTTP(endpoint, "post", payload, newPost.this);
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