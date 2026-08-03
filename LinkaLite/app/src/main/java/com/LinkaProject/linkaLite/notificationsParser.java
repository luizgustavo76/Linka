package com.LinkaProject.linkaLite;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class notificationsParser {

    public static List<NotificationMessage> parseJson(String jsonRaw) {
        List<NotificationMessage> messageList = new ArrayList<NotificationMessage>();

        try {
            JSONObject jsonObject = new JSONObject(jsonRaw);
            JSONArray array = jsonObject.getJSONArray("notifications");

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                int id = obj.getInt("id");
                String content = obj.getString("content");
                String datetime = obj.getString("datetime");
                String fromUser = obj.getString("fromUser");
                String receiver = obj.getString("receiver");
                int read = obj.getInt("read");
                String type = obj.getString("type");

                NotificationMessage message = new NotificationMessage(id, content, datetime, fromUser, receiver, read, type);
                messageList.add(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return messageList;
    }
}