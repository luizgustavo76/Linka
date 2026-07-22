package com.LinkaProject.linkaLite;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.LinkaProject.linkaLite.R;
import java.util.List;

public class CommentAdapter extends ArrayAdapter<Comment> {
    private Context context;
    private List<Comment> comments;

    public CommentAdapter(Context context, List<Comment> comments) {
        super(context, R.layout.item_comment, comments);
        this.context = context;
        this.comments = comments;
    }

    // Guardião de referências para reaproveitar views no Cupcake
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

        // Preenche as Views
        holder.tvUsername.setText("@" + comment.getUsername());
        holder.tvText.setText(comment.getTextComment());
        return convertView;
    }
}