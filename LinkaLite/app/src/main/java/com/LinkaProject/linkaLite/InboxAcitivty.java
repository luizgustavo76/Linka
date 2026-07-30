package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.List;

public class InboxActivity extends Activity {

    private ListView lvInbox;
    private InboxAdapter adapter;
    private List<InboxItem> itemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        lvInbox = (ListView) findViewById(R.id.lvInbox);
        itemList = new ArrayList<InboxItem>();

        // Dados de teste (no futuro você preenche isso com o JSON que vem da sua API Python)
        itemList.add(new InboxItem("101", "@dev_galaxy_y", "http://seu-server.com/img1.jpg"));
        itemList.add(new InboxItem("102", "@user_s2_ultra", "http://seu-server.com/img2.jpg"));
        itemList.add(new InboxItem("103", "@retro_coder", "http://seu-server.com/img3.jpg"));

        // Instancia e define o Adapter na ListView
        adapter = new InboxAdapter(this, itemList);
        lvInbox.setAdapter(adapter);
    }
}