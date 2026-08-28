package com.LinkaProject.linkaLite;
import org.json.JSONObject;
import org.json.JSONException;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
public class ChangeBiography extends Activity{
    private EditText edtBiography;
    private Button btnSend;
    private String username = "";
    private String url = "";
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_biography);
        try{
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(ChangeBiography.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            url = server.optString("url", "");
            username = fastLogin.optString("username", "");
        }catch (JSONException e){
            e.printStackTrace();
        }
        edtBiography = (EditText) findViewById(R.id.edtBio);
        btnSend = (Button) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                try{
                    JSONObject jsonBio = new JSONObject();
                    jsonBio.put("username", username);
                    jsonBio.put("mode", "bio");
                    jsonBio.put("content", edtBiography.getText());
                    request.requestHTTP(url + "/edit", "post", jsonBio, ChangeBiography.this);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
}