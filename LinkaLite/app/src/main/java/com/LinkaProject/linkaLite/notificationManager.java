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

            if (notifications == null || notifications.isEmpty()) {
                return;
            }

            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager navManager = (NotificationManager) context.getSystemService(ns);

            for (NotificationMessage msg : notifications) {
                int id = msg.getId();
                String fromUser = msg.getFromUser();
                String content = msg.getContent();

                int icon = R.drawable.icon; // ou android.R.drawable.stat_notify_chat
                CharSequence tickerText = fromUser + ": " + content;
                long when = System.currentTimeMillis();

                Notification notification = new Notification(icon, tickerText, when);

                // Correção do Título x Conteúdo
                CharSequence contentTitle = fromUser; 
                CharSequence contentText = content;

                Intent notificationIntent = new Intent(context, HomeActivity.class);
                notificationIntent.putExtra("NOTIFICATION_ID", id); // Passa o ID para a HomeActivity marcar como lida ao abrir
                
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
                notification.flags |= Notification.FLAG_AUTO_CANCEL; // Limpa a notificação da barra quando clicada

                int notifyId = (id > 0) ? id : (int) (System.currentTimeMillis() % 10000);
                navManager.notify(notifyId, notification);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}