package com.LinkaProject.linkaLite;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class CommunityIndex extends BaseActivity {
    private ListView listViewCommunities;
    private CommunityAdapter adapter;
    private final ArrayList<Community> allCommunities = new ArrayList<Community>();
    private final ArrayList<Community> filteredCommunities = new ArrayList<Community>();
    private String url = "https://patient-hall-2915.luizsgustavo76.workers.dev";
    private Button btnNew;
    private EditText editSearch;
    private ImageButton btnBack, btnRefreshCommunities, btnProfile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.communities_index);

        btnNew = (Button) findViewById(R.id.btnNew);
        editSearch = (EditText) findViewById(R.id.editSearch);
        btnBack = (ImageButton) findViewById(R.id.btnBack);
        btnRefreshCommunities = (ImageButton) findViewById(R.id.btnRefreshCommunities);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        listViewCommunities = (ListView) findViewById(R.id.listViewCommunities);

        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(CommunityIndex.this, profile.class));
                }
            });
        }

        if (btnRefreshCommunities != null) {
            btnRefreshCommunities.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new FetchCommunitiesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            });
        }

        if (btnNew != null) {
            btnNew.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(CommunityIndex.this, NewCommunity.class));
                }
            });
        }

        try {
            config cfg = new config();
            String raw = cfg.loadCfgAsJson(CommunityIndex.this, "config.cfg");
            if (raw != null && raw.length() > 0) {
                JSONObject jsonCfg = new JSONObject(raw);
                JSONObject server = jsonCfg.optJSONObject("SERVER");
                if (server != null) {
                    String cfgUrl = server.optString("url", "").trim();
                    if (cfgUrl.length() > 0) {
                        url = cfgUrl;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (listViewCommunities != null) {
            listViewCommunities.setScrollingCacheEnabled(true);
            listViewCommunities.setAnimationCacheEnabled(true);
            adapter = new CommunityAdapter(this, filteredCommunities);
            listViewCommunities.setAdapter(adapter);
        }

        if (editSearch != null) {
            editSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterCommunities(s.toString());
                }
            });
        }

        new FetchCommunitiesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new FetchCommunitiesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void filterCommunities(String query) {
        filteredCommunities.clear();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        if (q.length() == 0) {
            filteredCommunities.addAll(allCommunities);
        } else {
            for (Community c : allCommunities) {
                String name = c.getName() != null ? c.getName().toLowerCase(Locale.getDefault()) : "";
                String desc = c.getDescription() != null ? c.getDescription().toLowerCase(Locale.getDefault()) : "";
                if (name.contains(q) || desc.contains(q)) {
                    filteredCommunities.add(c);
                }
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private class FetchCommunitiesTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            String baseUrl = url.trim();
            while (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            return request.requestHTTP(baseUrl + "/view-communities", "GET", new JSONObject(), CommunityIndex.this);
        }

        @Override
        protected void onPostExecute(String response) {
            if (isFinishing()) return;
            if (response != null && response.trim().length() > 0) {
                try {
                    JSONArray jsonResult = new JSONArray(response.trim());
                    allCommunities.clear();
                    for (int i = 0; i < jsonResult.length(); i++) {
                        JSONObject obj = jsonResult.getJSONObject(i);
                        String name = obj.optString("name", "Community");
                        String description = obj.optString("description", "");
                        String bannerUrl = obj.optString("banner_image", "");
                        String iconUrl = obj.optString("icon_image", "");
                        String id = obj.optString("id", "");

                        allCommunities.add(new Community(name, description, bannerUrl, iconUrl, id));
                    }
                    String currentSearch = editSearch != null ? editSearch.getText().toString() : "";
                    filterCommunities(currentSearch);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(CommunityIndex.this, "Error parsing communities", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(CommunityIndex.this, "Failed to load communities", Toast.LENGTH_SHORT).show();
            }
        }
    }
}