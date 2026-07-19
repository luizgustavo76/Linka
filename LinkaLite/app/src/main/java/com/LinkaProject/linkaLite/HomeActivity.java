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

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
public class HomeActivity extends Activity {
    private Button btnHome;
    private Button btnProfile;
    private Button btnOptions;
    private Button btnChat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        btnHome = (Button) findViewById(R.id.btnHome);
        btnChat = (Button) findViewById(R.id.btnChat);
        btnProfile = (Button) findViewById(R.id.btnProfile);
        btnOptions = (Button) findViewById(R.id.btnOptions);
    }
}