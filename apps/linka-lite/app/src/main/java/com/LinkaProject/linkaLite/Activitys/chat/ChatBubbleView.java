package com.LinkaProject.linkaLite;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
public class ChatBubbleView extends LinearLayout {
    private TextView txtMessage;
    private ImageView imgProfile;
    private TextView txtUsername;
    private LinearLayout bubbleContainer;
    public ChatBubbleView(Context context, String text, boolean isMe, String username) {
        super(context);
        init(context, text, isMe, username);
    }
    private void init(Context context, String text, boolean isMe, String username) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.chat_bubble_item, this, true);
        bubbleContainer = (LinearLayout) findViewById(R.id.bubbleContainer);
        txtMessage = (TextView) findViewById(R.id.txtMessage);
        imgProfile = (ImageView) findViewById(R.id.imgProfile);
        txtUsername = (TextView) findViewById(R.id.txtUsername);
        ImageLoader imageLoader = new ImageLoader();
        imageLoader.viewProfilePicture(context, username, imgProfile);
        txtUsername.setText(username);
        txtMessage.setText(text);
        if (isMe) {
            bubbleContainer.setGravity(Gravity.RIGHT);
            txtMessage.setBackgroundResource(R.layout.bubble_mine);
        } else {
            bubbleContainer.setGravity(Gravity.LEFT);
            txtMessage.setBackgroundResource(R.layout.bubble_other);
        }
    }
}