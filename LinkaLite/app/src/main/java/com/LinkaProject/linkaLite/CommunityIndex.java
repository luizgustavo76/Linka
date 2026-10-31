package com.LinkaProject.linkaLite;
import android.content.Intent;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.widget.Button;
public class CommunityIndex extends Activity {
    private ListView listViewCommunities;
    private CommunityAdapter adapter;
    private ArrayList<Community> communityList;
    private String url = "";
    private Button btnNew;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.communities_index);
        btnNew = (Button) findViewById(R.id.btnNew);
        btnNew.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(CommunityIndex.this, NewCommunity.class);
                startActivity(intent);
            }
        });
        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(CommunityIndex.this, "conifg.cfg"));
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            url = server.optString("url");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        listViewCommunities = (ListView) findViewById(R.id.listViewCommunities);
        if (listViewCommunities != null) {
            listViewCommunities.setScrollingCacheEnabled(false);
            listViewCommunities.setAnimationCacheEnabled(false);
        }

        communityList = new ArrayList<Community>();

        try {
            String response = request.requestHTTP(url + "/view-communities", "get", new JSONObject(), CommunityIndex.this);
            if (response != null && !response.trim().equals("")) {
                JSONArray jsonResult = new JSONArray(response);
                for (int i = 0; i < jsonResult.length(); i++) {
                    JSONObject obj = jsonResult.getJSONObject(i);
                    String name = obj.optString("name", "wtf a community without a name");
                    String description = obj.optString("description", "");
                    String bannerUrl = obj.optString("banner_image", "");
                    String iconUrl = obj.optString("icon_image", "");
                    Community community = new Community(name, description, bannerUrl, iconUrl);
                    communityList.add(community);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        adapter = new CommunityAdapter(this, communityList);
        if (listViewCommunities != null) {
            listViewCommunities.setAdapter(adapter);
        }
    }
}