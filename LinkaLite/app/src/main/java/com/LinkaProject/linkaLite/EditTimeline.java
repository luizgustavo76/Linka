package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class EditTimeline extends Activity {
    private Button btnNewer;
    private EditText edtUrl;
    private Button btnAdd;
    private ImageButton btnHome;
    private ImageButton btnChat;
    private ImageButton btnOptions;
    private ImageButton btnProfile;
    private ListView listView;
    private String url = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_federation);

        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        listView = (ListView) findViewById(R.id.listFederations);
        edtUrl = (EditText) findViewById(R.id.edtUrl);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(EditTimeline.this, "config.cfg"));
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            url = server.getString("url");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Executa a busca na rede em Background
        new FetchFederationsTask().execute(url + "/view-index");
    }

    // AsyncTask para evitar NetworkOnMainThreadException
    private class FetchFederationsTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String targetUrl = params[0];
            try {
                return request.requestHTTP(targetUrl, "get", new JSONObject(), EditTimeline.this);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            if (response == null || response.trim().isEmpty()) {
                Toast.makeText(EditTimeline.this, "Error in loading data", Toast.LENGTH_SHORT).show();
                return;
            }

            List<FederationItem> itemList = new ArrayList<>();
            try {
                JSONArray jsonArray = new JSONArray(response);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    itemList.add(new FederationItem(
                        obj.optString("cover_image"),
                        obj.optString("description"),
                        obj.optString("name"),
                        obj.optString("url")
                    ));
                }

                if (listView != null && !isFinishing()) {
                    FederationsAdapter adapter = new FederationsAdapter(EditTimeline.this, itemList);
                    listView.setAdapter(adapter);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}