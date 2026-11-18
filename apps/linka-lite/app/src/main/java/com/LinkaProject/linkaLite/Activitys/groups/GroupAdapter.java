package com.LinkaProject.linkaLite;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;
public class GroupAdapter extends BaseAdapter {
    private Context context;
    private List<Group> groupList;
    private LayoutInflater inflater;
    public GroupAdapter(Context context, List<Group> groupList) {
        this.context = context;
        this.groupList = groupList;
        this.inflater = LayoutInflater.from(context);
    }
    @Override
    public int getCount() { return groupList != null ? groupList.size() : 0; }
    @Override
    public Object getItem(int position) { return groupList.get(position); }
    @Override
    public long getItemId(int position) { return groupList.get(position).getId(); }
    private static class ViewHolder {
        TextView txtGroupName;
        TextView txtGroupPerm;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_channel, parent, false);
            holder = new ViewHolder();
            holder.txtGroupName = (TextView) convertView.findViewById(R.id.txtChannelName);
            holder.txtGroupPerm = (TextView) convertView.findViewById(R.id.txtChannelDescription);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Group group = groupList.get(position);
        holder.txtGroupName.setText(group.getName());
        holder.txtGroupPerm.setText("Permissão: " + group.getPermissions());
        return convertView;
    }
}