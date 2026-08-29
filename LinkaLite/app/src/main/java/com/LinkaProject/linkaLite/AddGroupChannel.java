package com.LinkaProject.linkaLite;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.ListView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
public class AddGroupChannel extends Activity{
    private Button btnSend;
    private EditText edtName;
    private String username = "";
    private String url = "";
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_channel);
        try{
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(AddGroupChannel.this, "config.cfg"));
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            url = server.optString("url", "");
            username = fastLogin.optString("username", "");
        }catch(JSONException e){
            e.printStackTrace();
        }
        btnSend = (Button) findViewById(R.id.btnSend);
        edtName = (EditText) findViewById(R.id.edtName);
        btnSend.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                try{
                    JSONObject jsonAdd = new JSONObject();
                    jsonAdd.put("username", username);
                    jsonAdd.put("channel_name", edtName.getText().toString());
                    request.requestHTTP(url + "/new-channel", "post", jsonAdd, AddGroupChannel.this);
                    Intent intent = new Intent(AddGroupChannel.this, ChannelsGroupActivity.class);
                    startActivity(intent);
                }catch(JSONException e){
                    e.printStackTrace();
                }
            }
        });
    }
}