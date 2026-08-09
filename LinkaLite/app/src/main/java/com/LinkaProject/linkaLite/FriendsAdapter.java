package com.LinkaProject.linkaLite;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class FriendsAdapter extends BaseAdapter {

    public static final int TYPE_FRIEND = 0;
    public static final int TYPE_HEADER = 1;
    private static final int TOTAL_TYPES = 2;

    private Context context;    
    private List<FriendItem> itemList;
    private LayoutInflater inflater;

    public FriendsAdapter(Context context, List<FriendItem> itemList) {
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

    @Override
    public int getViewTypeCount() {
        return TOTAL_TYPES; 
    }

    @Override
    public int getItemViewType(int position) {
        return itemList.get(position).getType();
    }

    static class FriendViewHolder {
        ImageView avatar;
        TextView username;
    }

    static class HeaderViewHolder {
        TextView txtHeader;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int type = getItemViewType(position);
        FriendItem item = itemList.get(position);

        if (type == TYPE_FRIEND) {
            FriendViewHolder holder;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_chat, parent, false);
                holder = new FriendViewHolder();
                
                holder.avatar = (ImageView) convertView.findViewById(R.id.avatar);
                holder.username = (TextView) convertView.findViewById(R.id.username);
                
                convertView.setTag(holder);
            } else {
                holder = (FriendViewHolder) convertView.getTag();
            }

            holder.username.setText(item.getUsername());

            // 1. Reseta o ícone para o padrão (evita fotos trocadas ao rolar a lista)
            holder.avatar.setImageResource(R.drawable.account);

            // 2. Lógica de carregamento de avatar
            if (item.getAvatarBitmap() != null) {
                holder.avatar.setImageBitmap(item.getAvatarBitmap());
            } else if (item.getAvatarResId() != 0) {
                holder.avatar.setImageResource(item.getAvatarResId());
            } else {
                String cleanName = item.getUsername().replace("@", "");
                ImageLoader imageLoad = new ImageLoader();
                imageLoad.viewProfilePicture(context, cleanName, holder.avatar);
            }

        // --- TIPO 1: CABEÇALHO ---
        } else if (type == TYPE_HEADER) {
            HeaderViewHolder holder;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_header, parent, false);
                holder = new HeaderViewHolder();
                holder.txtHeader = (TextView) convertView.findViewById(R.id.txtHeader);
                convertView.setTag(holder);
            } else {
                holder = (HeaderViewHolder) convertView.getTag();
            }

            holder.txtHeader.setText(item.getHeaderTitle());
        }

        return convertView;
    }
}