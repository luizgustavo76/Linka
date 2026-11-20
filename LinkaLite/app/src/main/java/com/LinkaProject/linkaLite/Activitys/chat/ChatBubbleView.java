package com.LinkaProject.linkaLite;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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

        String safeUsername = (username != null && username.trim().length() > 0) ? username.trim() : "?";
        txtUsername.setText(safeUsername);
        txtMessage.setText(text);

        // Garante que a ImageView está visível e define o avatar algorítmico inicial
        imgProfile.setVisibility(View.VISIBLE);
        Bitmap defaultAvatar = AvatarGenerator.generateAvatar(safeUsername, 128);
        if (defaultAvatar != null) {
            imgProfile.setImageBitmap(defaultAvatar);
        }

        // Carrega a foto de perfil real se houver username válido
        if (!safeUsername.equals("?")) {
            ImageLoader imageLoader = new ImageLoader();
            imageLoader.viewProfilePicture(context, safeUsername, imgProfile);
        }

        if (isMe) {
            bubbleContainer.setGravity(Gravity.RIGHT);
            txtMessage.setBackgroundResource(R.layout.bubble_mine);
        } else {
            bubbleContainer.setGravity(Gravity.LEFT);
            txtMessage.setBackgroundResource(R.layout.bubble_other);
        }
    }
}