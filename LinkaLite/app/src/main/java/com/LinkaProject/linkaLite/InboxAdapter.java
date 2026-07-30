package com.example.linka;

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

    // O ViewHolder reutiliza as referências das views na RAM
    static class ViewHolder {
        ImageView imgAvatar;
        TextView txtUsername;
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
            holder.txtUsername = (TextView) convertView.findViewById(R.id.postUsername);
            holder.btnAccept = (Button) convertView.findViewById(R.id.btnAccept);
            holder.btnDenied = (Button) convertView.findViewById(R.id.btnDenied);
            
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final InboxItem item = itemList.get(position);

        // Preenche o nome do usuário
        holder.txtUsername.setText(item.getUsername());

        // TODO: Carregar a imagem da URL aqui (usando o seu proxy/rota de imagens)
        // Por enquanto usa uma imagem padrão do projeto:
        // holder.imgAvatar.setImageResource(R.drawable.ic_avatar_placeholder);

        // Clique no Botão ACEITAR
        holder.btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Aceitou " + item.getUsername(), Toast.LENGTH_SHORT).show();
                
                // 1. Envia a requisição de aceite para o seu Backend Python aqui
                
                // 2. Remove o item da lista e atualiza a tela
                itemList.remove(position);
                notifyDataSetChanged();
            }
        });

        // Clique no Botão RECUSAR
        holder.btnDenied.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Recusou " + item.getUsername(), Toast.LENGTH_SHORT).show();
                
                // 1. Envia a requisição de recusa para o seu Backend Python aqui
                
                // 2. Remove o item da lista e atualiza a tela
                itemList.remove(position);
                notifyDataSetChanged();
            }
        });

        return convertView;
    }
}