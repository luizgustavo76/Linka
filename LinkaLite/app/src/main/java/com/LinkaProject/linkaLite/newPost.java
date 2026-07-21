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
import android.widget.EditText;
import android.widget.Button;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
public class newPost extends Activity{
    private TextView newPostText;
    private EditText textPost;
    private Button btnSend;
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.new_post_activity);
        TextView newPostText = (TextView) findViewById(R.id.newPostText);
        EditText textPost = (EditText) findViewById(R.id.textPost);
        Button btnSend = (Button) findViewById(R.id.btnSend);
    }
}