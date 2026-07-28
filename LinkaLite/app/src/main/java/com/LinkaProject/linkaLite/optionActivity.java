package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.content.Intent;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
public class optionActivity extends Activity{
    private Button btnConfigurations;
    private Button btnInbox;
    private Button btnFriends;
    private Button btnChangeServer;
    private ImageButton btnHome;
    private ImageButton btnChat;
    private ImageButton btnProfile;
    private ImageButton btnOptions;
    @Override
    public void onCreate(Bundle savedInstanceState){        
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.activity_options);
        btnConfigurations = (Button) findViewById(R.id.btnConfigurations);
        btnInbox = (Button) findViewById(R.id.btnInbox);
        btnFriends = (Button) findViewById(R.id.btnFriends);
        btnChangeServer = (Button) findViewById(R.id.btnChangeServer);
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
    }
}
