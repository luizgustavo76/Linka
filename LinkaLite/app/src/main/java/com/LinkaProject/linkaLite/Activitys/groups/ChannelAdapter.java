package com.LinkaProject.linkaLite;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;
public class ChannelAdapter extends BaseAdapter {
    private Context context;
    private List<Channel> channelList;
    private LayoutInflater inflater;
    public ChannelAdapter(Context context, List<Channel> channelList) {
        this.context = context;
        this.channelList = channelList;
        this.inflater = LayoutInflater.from(context);
    }
    @Override
    public int getCount() {
        return channelList != null ? channelList.size() : 0;
    }
    @Override
    public Object getItem(int position) {
        return channelList.get(position);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    private static class ViewHolder {
        TextView txtName;
        TextView txtDescription;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_channel, parent, false);
            holder = new ViewHolder();
            holder.txtName = (TextView) convertView.findViewById(R.id.txtChannelName);
            holder.txtDescription = (TextView) convertView.findViewById(R.id.txtChannelDescription);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Channel channel = channelList.get(position);
        holder.txtName.setText("# " + channel.getName());
        holder.txtDescription.setText(channel.getDescription());
        return convertView;
    }
}