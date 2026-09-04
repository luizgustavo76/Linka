package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import org.json.JSONException;
import org.json.JSONObject;

public class ChangeGroupName extends Activity {
    private EditText edtName;
    private Button btnSend;
    private String username = "";
    private String url = "";
    private int groupId = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_group_name);

        Intent intent = getIntent();
        groupId = intent.getIntExtra("groupId", -1);

        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(ChangeGroupName.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            url = server.optString("url", "");
            username = fastLogin.optString("username", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        edtName = (EditText) findViewById(R.id.edtName);
        btnSend = (Button) findViewById(R.id.btnSend);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {                    
                    JSONObject jsonRequest = new JSONObject();
                    jsonRequest.put("username", username);
                    jsonRequest.put("group_id", groupId);
                    jsonRequest.put("new_name", edtName.getText().toString().trim());
                    
                    request.requestHTTP(url + "/rename-group", "post", jsonRequest, ChangeGroupName.this);
                    
                    Intent intentGroup = new Intent(ChangeGroupName.this, ChannelsGroupActivity.class);
                    intentGroup.putExtra("groupId", groupId);
                    startActivity(intentGroup);
                    finish(); 
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}