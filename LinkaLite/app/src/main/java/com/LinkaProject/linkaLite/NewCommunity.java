package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import org.json.JSONObject;
import org.json.JSONException;
public class NewCommunity extends Activity {
    private String url = "";
    private String username = "";
    private EditText edtName;
    private EditText edtDescription;
    private Button btnCreateCommunity;
    private EditText edtBannerUrl;
    private EditText edtIconUrl;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        try{
            config cfg = new config();
            String rawCfg = cfg.loadCfgAsJson(NewCommunity.this, "config.cfg");
            JSONObject jsonCfg = new JSONObject(rawCfg);
            JSONObject server = jsonCfg.optJSONObject("SERVER");
            JSONObject fastLogin = jsonCfg.optJSONObject("FAST_LOGIN");
            url = (server != null) ? server.optString("url", "http://linkaProject.pythonanywhere.com") : "http://linkaProject.pythonanywhere.com";
            username = fastLogin.optString("username", "");
        }catch(JSONException e){
            e.printStackTrace();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_community);
        edtBannerUrl = (EditText) findViewById(R.id.edtBannerUrl);
        edtIconUrl = (EditText) findViewById(R.id.edtIconUrl);
        edtName = (EditText) findViewById(R.id.edtName);
        edtDescription = (EditText) findViewById(R.id.edtDescription);
        btnCreateCommunity = (Button) findViewById(R.id.btnCreateCommunity);

        btnCreateCommunity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = edtName.getText().toString().trim();
                String description = edtDescription.getText().toString().trim();
                String iconUrl = edtIconUrl.getText().toString().trim();
                String bannerUrl = edtBannerUrl.getText().toString().trim();
                if (name.length() == 0) {
                    Toast.makeText(NewCommunity.this, "Please enter a community name", Toast.LENGTH_SHORT).show();
                    return;
                }

                createCommunity(name, description, iconUrl, bannerUrl);
            }
        });
    }

    private void createCommunity(String name, String description, String iconUrl, String bannerUrl) {
        try{
            JSONObject jsonCommunity = new JSONObject();
            jsonCommunity.put("username", username);
            jsonCommunity.put("community_name", name);
            jsonCommunity.put("description", description);
            jsonCommunity.put("icon_image", iconUrl);
            jsonCommunity.put("banner_image", bannerUrl);
            request.requestHTTP(url + "/new-community", "post", jsonCommunity, NewCommunity.this);
        }catch(JSONException e){
            e.printStackTrace();
        }
        finish();
    }
}