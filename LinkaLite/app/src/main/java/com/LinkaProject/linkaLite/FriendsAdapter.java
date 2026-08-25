package com.LinkaProject.linkaLite;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class FriendsAdapter extends BaseAdapter {

    public static final int TYPE_FRIEND = 0;
    public static final int TYPE_GROUP = 1;
    public static final int TYPE_HEADER = 2;

    private Context context;
    private List<FriendItem> items;
    private LayoutInflater inflater;

    public FriendsAdapter(Context context, List<FriendItem> items) {
        this.context = context;
        this.items = items;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return items != null ? items.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return items != null ? items.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (items != null && position < items.size()) {
            return items.get(position).getType();
        }
        return TYPE_FRIEND;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    private static class ViewHolder {
        TextView txtName;
        TextView txtSubtitle;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (items == null || position >= items.size()) {
            return new View(context);
        }

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_channel, parent, false);
            holder = new ViewHolder();
            holder.txtName = (TextView) convertView.findViewById(R.id.txtChannelName);
            holder.txtSubtitle = (TextView) convertView.findViewById(R.id.txtChannelDescription);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        FriendItem item = items.get(position);

        if (item != null) {
            if (item.getType() == TYPE_GROUP) {
                holder.txtName.setText("[Group] " + (item.getGroupName() != null ? item.getGroupName() : "Sem Nome"));
                holder.txtSubtitle.setText("Permission: " + (item.getPermissions() != null ? item.getPermissions() : "membro"));
            } else if (item.getType() == TYPE_HEADER) {
                holder.txtName.setText(item.getGroupName());
                holder.txtSubtitle.setText("");
            } else {
                holder.txtName.setText(item.getUsername() != null ? item.getUsername() : "Usuário");
                holder.txtSubtitle.setText("DM");
            }
        }

        return convertView;
    }
}