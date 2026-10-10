package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import android.content.Intent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private String nameTheme = "";
    private boolean isManual = false;
    private List<FederationItem> itemList = new ArrayList<FederationItem>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_federation);

        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        listView = (ListView) findViewById(R.id.listFederations);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        edtUrl = (EditText) findViewById(R.id.edtUrl);

        nameTheme = getIntent().getStringExtra("nameTheme");
        isManual = getIntent().getBooleanExtra("isManual", false);
        if (nameTheme == null || nameTheme.isEmpty()) {
            nameTheme = getIntent().getStringExtra("theme");
        }

        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(EditTimeline.this, "config.cfg"));
            if (jsonCfg.has("SERVER")) {
                JSONObject server = jsonCfg.getJSONObject("SERVER");
                url = server.optString("url", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (btnAdd != null) {
            btnAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (edtUrl != null) {
                        String inputUrl = edtUrl.getText().toString().trim();
                        if (!inputUrl.isEmpty()) {
                            config cfg = new config();
                            cfg.toggleUrl(EditTimeline.this, inputUrl);
                            edtUrl.setText("");
                            fetchData();
                        }
                    }
                }
            });
        }

        if (btnHome != null) {
            btnHome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        if (listView != null) {
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Object obj = parent.getItemAtPosition(position);
                    if (obj instanceof FederationItem) {
                        FederationItem item = (FederationItem) obj;
                        config cfg = new config();
                        cfg.toggleUrl(EditTimeline.this, item.getUrl());
                        fetchData();
                    }
                }
            });
        }

        fetchData();
    }

    private void fetchData() {
        String baseUrl = (url != null && url.endsWith("/")) ? url.substring(0, url.length() - 1) : url;
        String targetEndpoint;
        if (nameTheme != null && !nameTheme.trim().isEmpty()) {
            targetEndpoint = baseUrl + "/view-index/" + nameTheme.trim().toLowerCase(Locale.ROOT);
        } else {
            targetEndpoint = baseUrl + "/view-index";
        }
        new FetchFederationsTask().execute(targetEndpoint);
    }

    private class FetchFederationsTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            if (isManual) {
                // Modo manual nao precisa fazer requisicao HTTP na API de temas
                return null;
            }
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
            itemList.clear();
            
            config cfg = new config();
            List<String> savedUrls = cfg.getSavedUrls(EditTimeline.this);
            if (savedUrls == null) {
                savedUrls = new ArrayList<String>();
            }

            if (isManual) {
                // Se for gerenciamento manual, popula a itemList com as URLs do arquivo config.cfg
                for (String savedUrl : savedUrls) {
                    itemList.add(new FederationItem(
                        "", 
                        "Adicionado manualmente", 
                        savedUrl, 
                        savedUrl
                    ));
                }
            } else {
                // Fluxo normal por tema: adiciona o servidor local + respostas HTTP
                itemList.add(new FederationItem(
                    "", 
                    "Local server", 
                    "Linka-local", 
                    "http://linkaProject.pythonanywhere.com"
                ));

                if (response != null && !response.trim().isEmpty()) {
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            itemList.add(new FederationItem(
                                obj.optString("cover_image", ""),
                                obj.optString("description", ""),
                                obj.optString("name", ""),
                                obj.optString("url", "")
                            ));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (listView != null && !isFinishing()) {
                FederationsAdapter adapter = new FederationsAdapter(EditTimeline.this, itemList, savedUrls);
                listView.setAdapter(adapter);
            }
        }
    }
}