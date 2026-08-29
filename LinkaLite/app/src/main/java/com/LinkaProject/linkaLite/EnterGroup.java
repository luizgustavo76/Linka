package com.LinkaProject.linkaLite;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
public class EnterGroup extends Activity{
    private Button btnSend;
    private EditText edtCode;
    private String url = "";
    private String username = "";
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        try{
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(EnterGroup.this, "config.cfg"));
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            username = fastLogin.optString("username", "");
            url = server.optString("url", "");
        }catch(JSONException e){
            e.printStackTrace();
        }
        setContentView(R.layout.activity_enter_group);
        edtCode = (EditText) findViewById(R.id.edtCode);
        btnSend = (Button) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                try{
                    JSONObject jsonRequest = new JSONObject();
                    jsonRequest.put("username", username);
                    jsonRequest.put("code", edtCode.getText());
                    request.requestHTTP(url + "/join-group", "post", jsonRequest, EnterGroup.this);
                }catch(JSONException e){
                    e.printStackTrace();
                }
            }
        });
    }
}