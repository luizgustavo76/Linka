package com.LinkaProject.linkaLite;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class CommunityAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Community> communityList;
    private ImageLoader imageLoader;

    public CommunityAdapter(Context context, ArrayList<Community> communityList) {
        this.context = context;
        this.communityList = communityList;
        this.imageLoader = new ImageLoader();
    }

    @Override
    public int getCount() {
        return communityList != null ? communityList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return communityList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        ImageView imgBanner;
        ImageView imgCommunityAvatar;
        TextView txtName;
        TextView txtDescription;
        Button btnMember;
        Button btnView;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_communities, parent, false);

            holder = new ViewHolder();
            holder.imgBanner = (ImageView) convertView.findViewById(R.id.imgBanner);
            holder.imgCommunityAvatar = (ImageView) convertView.findViewById(R.id.imgCommunityAvatar);
            holder.txtName = (TextView) convertView.findViewById(R.id.txtName);
            holder.txtDescription = (TextView) convertView.findViewById(R.id.txtDescription);
            holder.btnMember = (Button) convertView.findViewById(R.id.btnMember);
            holder.btnView = (Button) convertView.findViewById(R.id.btnView);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Community item = communityList.get(position);

        if (holder.txtName != null) {
            holder.txtName.setText(item.getName());
        }

        if (holder.txtDescription != null) {
            holder.txtDescription.setText(item.getDescription());
        }

        if (holder.imgBanner != null) {
            holder.imgBanner.setImageDrawable(null);
            if (item.getBannerUrl() != null && item.getBannerUrl().length() > 0) {
                imageLoader.LoadImageUrl(item.getBannerUrl(), holder.imgBanner);
            }
        }

        if (holder.imgCommunityAvatar != null) {
            holder.imgCommunityAvatar.setImageDrawable(null);
            if (item.getIconUrl() != null && item.getIconUrl().length() > 0) {
                imageLoader.LoadImageUrl(item.getIconUrl(), holder.imgCommunityAvatar);
            }
        }

        if (holder.btnMember != null) {
            holder.btnMember.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "Joined " + item.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (holder.btnView != null) {
            holder.btnView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String id = item.getId(); // Corrigido de int para String
                    Intent intent = new Intent(context, CommunityFeedActivity.class); // Usa 'context' em vez de 'CommunityAdapter.this'
                    intent.putExtra("community_id", id);
                    Toast.makeText(context, "Opening " + item.getName(), Toast.LENGTH_SHORT).show();
                    context.startActivity(intent);
                }
            });
        }

        return convertView;
    }
}