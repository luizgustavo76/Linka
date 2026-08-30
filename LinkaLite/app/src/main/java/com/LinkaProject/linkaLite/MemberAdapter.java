package com.linka.lite.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;
import com.linka.lite.R;
import com.linka.lite.model.Member;

public class MemberAdapter extends BaseAdapter {

    private final Context context;
    private final List<Member> memberList;
    private final LayoutInflater inflater;

    public MemberAdapter(Context context, List<Member> memberList) {
        this.context = context;
        this.memberList = memberList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return memberList != null ? memberList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return memberList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        ImageView imgAvatar;
        TextView txtUsername;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.card_member, parent, false);
            holder = new ViewHolder();
            holder.imgAvatar = (ImageView) convertView.findViewById(R.id.ImgAvatar);
            holder.txtUsername = (TextView) convertView.findViewById(R.id.txtUsername);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Member member = memberList.get(position);

        holder.txtUsername.setText("@" + member.getUsername());

        if (member.getAvatarResourceId() != 0) {
            holder.imgAvatar.setImageResource(member.getAvatarResourceId());
        } else {
            holder.imgAvatar.setImageResource(R.drawable.default_avatar);
        }

        return convertView;
    }
}