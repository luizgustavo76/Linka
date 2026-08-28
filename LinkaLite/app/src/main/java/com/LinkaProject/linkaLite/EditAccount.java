package com.LinkaProject.linkaLite;
import org.json.JSONObject;
import org.json.JSONException;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

public class EditAccount extends Activity {

    private Button btnChangePicture;
    private Button btnChangeBiography;
    private ImageButton btnHome;
    private ImageButton btnChat;
    private ImageButton btnOptions;
    private ImageButton btnProfile;
    private String url = "";
    private static final int PICK_IMAGE_REQUEST = 1001;
    private String username = "";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_account_activity);
        try{
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(EditAccount.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            url = server.optString("url", "");
            username = fastLogin.optString("username", "");
        }catch (JSONException e){
            e.printStackTrace();
        }
        btnChangeBiography = (Button) findViewById(R.id.btnChangeBiography);
        btnChangePicture = (Button) findViewById(R.id.btnChangePicture);
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);

        btnChangePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EditAccount.this, HomeActivity.class);
                startActivity(intent);
            }
        });

        btnOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EditAccount.this, optionActivity.class);
                startActivity(intent);
            }
        });

        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EditAccount.this, profile.class);
                startActivity(intent);
            }
        });

        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EditAccount.this, chatActivity.class);
                startActivity(intent);
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select the profile picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            new UploadProfilePicTask().execute(selectedImageUri);
        }
    }

    private class UploadProfilePicTask extends AsyncTask<Uri, Void, String> {
        @Override
        protected String doInBackground(Uri... uris) {
            Uri selectedImageUri = uris[0];
            String fullUrl = url + "/upload-profile"; 
            return UploadTask.uploadProfilePicture(EditAccount.this, selectedImageUri, fullUrl);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Toast.makeText(EditAccount.this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(EditAccount.this, "Error in update pfp", Toast.LENGTH_SHORT).show();
            }
        }
    }
}