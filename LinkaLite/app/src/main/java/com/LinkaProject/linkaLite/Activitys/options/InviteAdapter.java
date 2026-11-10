package com.LinkaProject.linkaLite;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;

public class InviteAdapter extends BaseAdapter {

    private Context context;
    private List<String> inviteList;
    private LayoutInflater inflater;

    public InviteAdapter(Context context, List<String> inviteList) {
        this.context = context;
        this.inviteList = inviteList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return inviteList != null ? inviteList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return inviteList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        TextView txtInviteCode;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            
            holder = new ViewHolder();
            holder.txtInviteCode = (TextView) convertView.findViewById(android.R.id.text1);
            
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String inviteCode = inviteList.get(position);
        holder.txtInviteCode.setText(inviteCode);

        return convertView;
    }
}