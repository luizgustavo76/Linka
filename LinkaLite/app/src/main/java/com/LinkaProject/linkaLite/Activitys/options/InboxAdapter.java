package com.LinkaProject.linkaLite;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import org.json.JSONObject;

public class InboxAdapter extends BaseAdapter {
    private Context context;
    private List<InboxItem> itemList;
    private LayoutInflater inflater;
    private ImageLoader imageLoader;
    private String url = "";
    private String username = "";

    public InboxAdapter(Context context, List<InboxItem> itemList, String url, String username) {
        this.context = context;
        this.itemList = itemList;
        this.inflater = LayoutInflater.from(context);
        this.imageLoader = new ImageLoader();
        this.url = url;
        this.username = username;
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
        TextView txtBadgeType;
        TextView txtMessage;
        LinearLayout layoutActions;
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
            holder.txtUsername = (TextView) convertView.findViewById(R.id.username);
            holder.txtBadgeType = (TextView) convertView.findViewById(R.id.txtBadgeType);
            holder.txtMessage = (TextView) convertView.findViewById(R.id.message);
            holder.layoutActions = (LinearLayout) convertView.findViewById(R.id.layoutActions);
            holder.btnAccept = (Button) convertView.findViewById(R.id.btnAccept);
            holder.btnDenied = (Button) convertView.findViewById(R.id.btnDenied);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final InboxItem item = itemList.get(position);
        final String targetUser = item.getUsername();

        holder.txtUsername.setText("@" + targetUser);
        imageLoader.viewProfilePicture(context, targetUser, holder.imgAvatar);

        View.OnClickListener openProfileListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(context, profile.class);
                    intent.putExtra("username", targetUser);
                    context.startActivity(intent);
                } catch (Exception ignored) {}
            }
        };
        holder.imgAvatar.setOnClickListener(openProfileListener);
        holder.txtUsername.setOnClickListener(openProfileListener);

        String msg = item.getMessage();
        boolean isNullMsg = (msg == null || msg.trim().length() == 0 || "null".equalsIgnoreCase(msg.trim()));

        if (item.getType() == InboxItem.TYPE_REQUEST) {
            holder.txtBadgeType.setText("REQUEST");
            holder.txtMessage.setText(isNullMsg ? "sent a friend request" : msg);
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.btnAccept.setVisibility(View.VISIBLE);
            holder.btnAccept.setText("Aceitar");
            holder.btnDenied.setVisibility(View.VISIBLE);
            holder.btnDenied.setText("Recusar");

            holder.btnAccept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        JSONObject jsonAccept = new JSONObject();
                        jsonAccept.put("receiver", username);
                        jsonAccept.put("remittee", targetUser);
                        request.requestHTTP(url + "/accept", "post", jsonAccept, context);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(context, "Accepted @" + targetUser, Toast.LENGTH_SHORT).show();
                    if (position < itemList.size()) {
                        itemList.remove(position);
                        notifyDataSetChanged();
                    }
                }
            });

            holder.btnDenied.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        JSONObject jsonDenied = new JSONObject();
                        jsonDenied.put("receiver", username);
                        jsonDenied.put("remittee", targetUser);
                        request.requestHTTP(url + "/denied", "post", jsonDenied, context);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(context, "Declined @" + targetUser, Toast.LENGTH_SHORT).show();
                    if (position < itemList.size()) {
                        itemList.remove(position);
                        notifyDataSetChanged();
                    }
                }
            });
        } else {
            // Notificação da rota /notifications (from_user, type, content, id)
            String badge = item.getNotifType();
            if (badge == null || badge.trim().length() == 0 || "null".equalsIgnoreCase(badge.trim())) {
                badge = "NOTIFY";
            } else {
                badge = badge.toUpperCase();
            }

            holder.txtBadgeType.setText(badge);
            holder.txtMessage.setText(isNullMsg ? "interacted with you." : msg);

            // Exibe um botão discreto para marcar a notificação como lida (/set-read-notification)
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.btnAccept.setVisibility(View.GONE);
            holder.btnDenied.setVisibility(View.VISIBLE);
            holder.btnDenied.setText("mark as read");

            holder.btnDenied.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        if (item.getId() > 0) {
                            JSONObject jsonRead = new JSONObject();
                            jsonRead.put("id", item.getId());
                            request.requestHTTP(url + "/set-read-notification", "post", jsonRead, context);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (position < itemList.size()) {
                        itemList.remove(position);
                        notifyDataSetChanged();
                    }
                }
            });
        }

        return convertView;
    }
}