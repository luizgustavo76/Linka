package com.LinkaProject.linkaLite;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.reflect.Method;
import java.util.List;

public class notificationManager {

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

            if (notifications == null || notifications.size() == 0) {
                return;
            }

            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager navManager = (NotificationManager) context.getSystemService(ns);

            for (NotificationMessage msg : notifications) {
                int id = msg.getId();
                String fromUser = msg.getFromUser();
                String content = msg.getContent();

                int icon = R.drawable.icon;
                CharSequence tickerText = fromUser + ": " + content;
                long when = System.currentTimeMillis();

                Notification notification = new Notification(icon, tickerText, when);

                CharSequence contentTitle = fromUser; 
                CharSequence contentText = content;

                Intent notificationIntent = new Intent(context, HomeActivity.class);
                notificationIntent.putExtra("NOTIFICATION_ID", id);
                
                PendingIntent contentIntent = PendingIntent.getActivity(
                    context, 
                    id,
                    notificationIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT
                );

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
                notification.flags |= Notification.FLAG_AUTO_CANCEL;

                // ANDROID 5.0+ (Lollipop / API 21): Define prioridade maxima/alta para virar "Heads-Up" (pop-up flutuante na tela)
                if (Integer.parseInt(Build.VERSION.SDK) >= Build.VERSION_CODES.JELLY_BEAN) {
                    notification.priority = Notification.PRIORITY_HIGH; // Ou Notification.PRIORITY_MAX
                }

                int notifyId = (id > 0) ? id : (int) (System.currentTimeMillis() % 10000);
                navManager.notify(notifyId, notification);

                try {
                    JSONObject jsonRead = new JSONObject();
                    jsonRead.put("id", id);
                    request.requestHTTP(url + "/set-read-notification", "post", jsonRead, context);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}