package com.LinkaProject.linkaLite;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class CommentAdapter extends ArrayAdapter<Comment> {
    private Context context;
    private List<Comment> comments;
    private boolean isExternal;
    private String externalUrl;

    public CommentAdapter(Context context, List<Comment> comments, boolean isExternal, String externalUrl) {
        super(context, R.layout.item_comment, comments);
        this.context = context;
        this.comments = comments;
        this.isExternal = isExternal;
        this.externalUrl = externalUrl;
    }

    static class ViewHolder {
        ImageView imgAvatar;
        TextView tvUsername;
        TextView tvText;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.item_comment, parent, false);

            holder = new ViewHolder();
            holder.imgAvatar = (ImageView) convertView.findViewById(R.id.imgCommentAvatar);
            holder.tvUsername = (TextView) convertView.findViewById(R.id.commentUsername);
            holder.tvText = (TextView) convertView.findViewById(R.id.textComment);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Comment comment = comments.get(position);

        // CORREÇÃO: Evita a duplicação do '@' na exibição do username
        String user = comment.getUsername();
        if (user != null && user.startsWith("@")) {
            holder.tvUsername.setText(user);
        } else {
            holder.tvUsername.setText("@" + (user != null ? user : "Anonimo"));
        }

        holder.tvText.setText(comment.getTextComment());

        ImageLoader imageLoader = new ImageLoader();

        // Roteamento da imagem de perfil
        if (!isExternal) {
            imageLoader.viewProfilePicture(context, comment.getUsername(), holder.imgAvatar);
        } else {
            String avatarUrl = externalUrl + "/profile-picture?username=" + comment.getUsername();
            imageLoader.LoadImageUrl(avatarUrl, holder.imgAvatar);
        }

        return convertView;
    }
}