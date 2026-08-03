import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {
    private String url = "";
    private String username = "";
    private int id = 0;
    private String fromUser = "";
    private String content = "";
    public static void createNotification() {
        try{
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(notificationManager.this, "config.cfg"));
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            url = server.getString("url").toString();
            username = fastLogin.getString("username").toString();
        }catch(JSONException e){
            e.printStackTrace();
        }
        JSONObject jsonNotifications;
        jsonNotifications.put("username", username);
        JSONObject response = new JSONObject(request.requestHTTP(url + "/notifications", "post", jsonNotifications, notificationManager.this));
        List<Message> notifications = MessageParser.parseJson(response);

        for (Message msg : notifications) {
            id = msg.getId();
            fromUser = msg.getFromUser();
            content = msg.getContent();
        }
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager = (NotificationManager) getSystemService(ns);
        int icon = android.R.drawable.stat_notify_chat;
        CharSequence tickerText = content;
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, tickerText, when);
        Context context = getApplicationContext();
        CharSequence contentTitle = "LinkaLite";
        CharSequence contentText = content;
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
}