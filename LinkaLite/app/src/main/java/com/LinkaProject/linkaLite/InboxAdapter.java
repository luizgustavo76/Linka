package com.LinkaProject.linkaLite;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class InboxAdapter extends BaseAdapter {

    private Context context;
    private List<InboxItem> itemList;
    private LayoutInflater inflater;

    public InboxAdapter(Context context, List<InboxItem> itemList) {
        this.context = context;
        this.itemList = itemList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return itemList.size();
    }

    @Override
    public Object getItem(int position) {
        return itemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        ImageView imgAvatar;
        TextView txtUsername;
        TextView txtMessage;
        Button btnAccept;
        Button btnDenied;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_inbox, parent, false);
            
            holder = new ViewHolder();
            holder.imgAvatar = (ImageView) convertView.findViewById(R.id.imgAvatar);
            // ID corrigido para bater com item_inbox.xml (@+id/username)
            holder.txtUsername = (TextView) convertView.findViewById(R.id.username);
            // Mapeando a mensagem (@+id/message)
            holder.txtMessage = (TextView) convertView.findViewById(R.id.message);
            holder.btnAccept = (Button) convertView.findViewById(R.id.btnAccept);
            holder.btnDenied = (Button) convertView.findViewById(R.id.btnDenied);
            
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final InboxItem item = itemList.get(position);

        holder.txtUsername.setText(item.getUsername());
        holder.txtMessage.setText(item.getMessage());

        // Clique no Botão ACEITAR
        holder.btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Accept " + item.getUsername(), Toast.LENGTH_SHORT).show();
                itemList.remove(position);
                notifyDataSetChanged();
            }
        });

        // Clique no Botão RECUSAR
        holder.btnDenied.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Denied " + item.getUsername(), Toast.LENGTH_SHORT).show();                
                itemList.remove(position);
                notifyDataSetChanged();
            }
        });

        return convertView;
    }
}