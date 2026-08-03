package com.LinkaProject.linkaLite;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.List;

public class notificationManager {

    private static final int DEFAULT_NOTIFICATION_ID = 1;

    public static void createNotification(Context context) {
        String url = "";
        String username = "";
        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(context, "config.cfg"));
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            url = server.getString("url");
            username = fastLogin.getString("username");
            JSONObject jsonNotifications = new JSONObject();
            jsonNotifications.put("username", username);
            String responseRaw = request.requestHTTP(url + "/notifications", "post", jsonNotifications, context);
            List<NotificationMessage> notifications = notificationsParser.parseJson(responseRaw);
            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager navManager = (NotificationManager) context.getSystemService(ns);
            for (NotificationMessage msg : notifications) {
                int id = msg.getId();
                String fromUser = msg.getFromUser();
                String content = msg.getContent();
                int icon = android.R.drawable.stat_notify_chat;
                CharSequence tickerText = content;
                long when = System.currentTimeMillis();
                Notification notification = new Notification(icon, tickerText, when);
                CharSequence contentTitle = "LinkaLite";
                CharSequence contentText = content;
                Intent notificationIntent = new Intent(context, HomeActivity.class);
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
                try {
                    Method setLatestEventInfo = Notification.class.getMethod(
                        "setLatestEventInfo", Context.class, CharSequence.class, CharSequence.class, PendingIntent.class
                    );
                    setLatestEventInfo.invoke(notification, context, contentTitle, contentText, contentIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                notification.defaults |= Notification.DEFAULT_SOUND;
                notification.defaults |= Notification.DEFAULT_VIBRATE;

                int notifyId = (id > 0) ? id : DEFAULT_NOTIFICATION_ID;
                navManager.notify(notifyId, notification);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}