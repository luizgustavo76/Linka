package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TimelineThemes extends Activity {
    private String url = "";
    private LinearLayout containerMain;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.themes_timeline);

        containerMain = (LinearLayout) findViewById(R.id.containerMain);

        LayoutInflater inflater = getLayoutInflater();

        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(TimelineThemes.this, "config.cfg"));
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            url = server.optString("url", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            String response = request.requestHTTP(url + "/view-index-themes", "get", new JSONObject(), TimelineThemes.this);
            JSONArray jsonIndex = new JSONArray(response);

            for (int i = 0; i < jsonIndex.length(); i++) {
                JSONObject jsonTheme = jsonIndex.getJSONObject(i);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                rowParams.setMargins(0, 0, 0, 8);
                row.setLayoutParams(rowParams);

                View cardView = inflater.inflate(R.layout.themes_card, row, false);

                TextView txtThemeName = (TextView) cardView.findViewById(R.id.txtThemeName);
                TextView txtThemeDesc = (TextView) cardView.findViewById(R.id.txtThemeDesc);

                txtThemeName.setText(jsonTheme.optString("name_theme", ""));
                txtThemeDesc.setText(jsonTheme.optString("description", ""));

                row.addView(cardView);
                containerMain.addView(row);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}