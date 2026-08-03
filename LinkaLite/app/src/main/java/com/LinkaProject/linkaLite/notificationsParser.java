package com.LinkaProject.linkaLite;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class notificationsParser {

    public static List<NotificationMessage> parseJson(String jsonRaw) {
        List<NotificationMessage> messageList = new ArrayList<NotificationMessage>();

        if (jsonRaw == null || jsonRaw.trim().isEmpty()) {
            return messageList;
        }

        try {
            JSONArray array;
            String trimmedJson = jsonRaw.trim();

            // Detecta se o backend retornou diretamente uma Array [...] ou um Objeto {"notifications": [...]}
            if (trimmedJson.startsWith("[")) {
                array = new JSONArray(trimmedJson);
            } else {
                JSONObject jsonObject = new JSONObject(trimmedJson);
                array = jsonObject.getJSONArray("notifications");
            }

            for (int i = 0; i < array.length(); i++) {
                try {
                    JSONObject obj = array.getJSONObject(i);

                    // optInt/optString evitam que o código quebre caso venha nulo ou com outro nome de chave
                    int id = obj.optInt("id", i + 1);
                    String content = obj.optString("content", obj.optString("message", ""));
                    String datetime = obj.optString("datetime", obj.optString("date", ""));
                    String fromUser = obj.optString("fromUser", obj.optString("from_user", "LinkaLite"));
                    String receiver = obj.optString("receiver", "");
                    int read = obj.optInt("read", 0);
                    String type = obj.optString("type", "notification");

                    NotificationMessage message = new NotificationMessage(id, content, datetime, fromUser, receiver, read, type);
                    messageList.add(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return messageList;
    }
}