package com.LinkaProject.linkaLite;

import android.content.Intent;
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

            int COLUMNS = 3;

            for (int i = 0; i < jsonIndex.length(); i += COLUMNS) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setWeightSum((float) COLUMNS);

                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                rowParams.setMargins(0, 0, 0, 8);
                row.setLayoutParams(rowParams);

                for (int col = 0; col < COLUMNS; col++) {
                    int itemIndex = i + col;

                    if (itemIndex < jsonIndex.length()) {
                        JSONObject jsonTheme = jsonIndex.getJSONObject(itemIndex);
                        View cardView = inflater.inflate(R.layout.themes_card, row, false);

                        TextView txtThemeName = (TextView) cardView.findViewById(R.id.txtThemeName);
                        TextView txtThemeDesc = (TextView) cardView.findViewById(R.id.txtThemeDesc);

                        final String currentTheme = jsonTheme.optString("name_theme", "");
                        txtThemeName.setText(currentTheme);
                        txtThemeDesc.setText(jsonTheme.optString("description", ""));

                        // Listener configurado diretamente no card individual
                        cardView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(TimelineThemes.this, EditTimeline.class);
                                intent.putExtra("nameTheme", currentTheme);
                                startActivity(intent);
                            }
                        });

                        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1.0f
                        );
                        cardParams.setMargins(4, 4, 4, 4);
                        cardView.setLayoutParams(cardParams);

                        row.addView(cardView);
                    } else {
                        View dummyView = new View(this);
                        LinearLayout.LayoutParams dummyParams = new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1.0f
                        );
                        dummyView.setLayoutParams(dummyParams);
                        row.addView(dummyView);
                    }
                }
                containerMain.addView(row);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}