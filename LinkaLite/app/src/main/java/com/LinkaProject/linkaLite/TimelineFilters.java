package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class TimelineFilters extends Activity {

    private ImageButton btnHome;
    private ImageButton btnChat;
    private ImageButton btnOptions;
    private ImageButton btnProfile;
    private TextView txtAdd;
    private TextView txtRemove;
    private ListView listFilters;

    private config cfg;
    private List<String> filterList;
    private ArrayAdapter<String> adapter;
    private int selectedPosition = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timeline_filters);

        cfg = new config();
        filterList = new ArrayList<String>();

        txtAdd = (TextView) findViewById(R.id.txtAdd);
        txtRemove = (TextView) findViewById(R.id.txtRemove);
        listFilters = (ListView) findViewById(R.id.listFilters);

        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);

        refreshFilterList();

        txtAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showAddFilterDialog();
            }
        });

        txtRemove.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (selectedPosition >= 0 && selectedPosition < filterList.size()) {
                    String tagToRemove = filterList.get(selectedPosition);
                    cfg.removeFilter(TimelineFilters.this, tagToRemove);
                    selectedPosition = -1;
                    refreshFilterList();
                    Toast.makeText(TimelineFilters.this, "Filter removed!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(TimelineFilters.this, "Select a filter to remove", Toast.LENGTH_SHORT).show();
                }
            }
        });

        listFilters.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedPosition = position;
                final String selectedTag = filterList.get(position);

                AlertDialog.Builder builder = new AlertDialog.Builder(TimelineFilters.this);
                builder.setTitle("Remove filter");
                builder.setMessage("Wanna remove? '" + selectedTag + "'?");
                builder.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        cfg.removeFilter(TimelineFilters.this, selectedTag);
                        selectedPosition = -1;
                        refreshFilterList();
                        Toast.makeText(TimelineFilters.this, "Filter removed    !", Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setNegativeButton("No", null);
                builder.show();
            }
        });
    }

    private void refreshFilterList() {
        filterList = cfg.getSavedFilters(this);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, filterList);
        listFilters.setAdapter(adapter);
    }

    private void showAddFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New filter");
        builder.setMessage("type here your tag filter");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String newTag = input.getText().toString().trim();
                if (!newTag.isEmpty()) {
                    cfg.addFilter(TimelineFilters.this, newTag);
                    refreshFilterList();
                    Toast.makeText(TimelineFilters.this, "Filter added!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(TimelineFilters.this, "the filter not be empty?", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}