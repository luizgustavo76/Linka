package com.LinkaProject.linkaLite;
import com.LinkaProject.linkaLite.R;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
public class HomeActivity extends Activity {
    private ImageButton btnHome;
    private ImageButton btnProfile;
    private ImageButton btnOptions;
    private ImageButton btnChat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
    }
}