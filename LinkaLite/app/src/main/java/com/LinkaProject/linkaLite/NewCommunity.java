package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class NewCommunity extends Activity {

    private EditText edtName;
    private EditText edtDescription;
    private Button btnCreateCommunity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_community);

        edtName = (EditText) findViewById(R.id.edtName);
        edtDescription = (EditText) findViewById(R.id.edtDescription);
        btnCreateCommunity = (Button) findViewById(R.id.btnCreateCommunity);

        btnCreateCommunity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = edtName.getText().toString().trim();
                String description = edtDescription.getText().toString().trim();

                if (name.length() == 0) {
                    Toast.makeText(NewCommunity.this, "Please enter a community name", Toast.LENGTH_SHORT).show();
                    return;
                }

                createCommunity(name, description);
            }
        });
    }

    private void createCommunity(String name, String description) {
        finish();
    }
}