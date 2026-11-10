package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ThemesViewer extends BaseActivity implements View.OnClickListener {

    private TextView btnLight;
    private TextView btnDark;
    private TextView btnLightBlue;
    private TextView btnDarkBlue;
    private TextView btnLightRed;
    private TextView btnDarkRed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // 1. Aplica o tema antes de inflar a view
        ThemeManager.applyTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.themes_activity);

        // 2. Inicializa os componentes do layout (matching IDs do XML)
        btnLight = (TextView) findViewById(R.id.btnLight);
        btnDark = (TextView) findViewById(R.id.btnDark);
        btnLightBlue = (TextView) findViewById(R.id.btnLightBlue);
        btnDarkBlue = (TextView) findViewById(R.id.btnDarkBlue);
        btnLightRed = (TextView) findViewById(R.id.btnLightRed);
        btnDarkRed = (TextView) findViewById(R.id.btnDarkRed);

        // 3. Registra os listeners de clique
        if (btnLight != null) btnLight.setOnClickListener(this);
        if (btnDark != null) btnDark.setOnClickListener(this);
        if (btnLightBlue != null) btnLightBlue.setOnClickListener(this);
        if (btnDarkBlue != null) btnDarkBlue.setOnClickListener(this);
        if (btnLightRed != null) btnLightRed.setOnClickListener(this);
        if (btnDarkRed != null) btnDarkRed.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String selectedTheme = "light";

        int id = v.getId();
        if (id == R.id.btnLight) {
            selectedTheme = "light";
        } else if (id == R.id.btnDark) {
            selectedTheme = "dark";
        } else if (id == R.id.btnLightBlue) {
            selectedTheme = "light_blue";
        } else if (id == R.id.btnDarkBlue) {
            selectedTheme = "dark_blue";
        } else if (id == R.id.btnLightRed) {
            selectedTheme = "light_red";
        } else if (id == R.id.btnDarkRed) {
            selectedTheme = "dark_red";
        }

        // Salva a escolha no SharedPreferences
        ThemeManager.setTheme(this, selectedTheme);
        Toast.makeText(this, "Tema alterado com sucesso!", Toast.LENGTH_SHORT).show();

        // Recarrega a Activity para aplicar as cores do novo tema sem fechar o app
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }
}