package com.LinkaProject.linkaLite;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
public class ChatBubbleView extends LinearLayout {
    private TextView txtMessage;
    private LinearLayout bubbleContainer;
    public ChatBubbleView(Context context, String text, boolean isMe) {
        super(context);
        init(context, text, isMe);
    }
    private void init(Context context, String text, boolean isMe) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.chat_bubble_item, this, true);
        bubbleContainer = (LinearLayout) findViewById(R.id.bubbleContainer);
        txtMessage = (TextView) findViewById(R.id.txtMessage);
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