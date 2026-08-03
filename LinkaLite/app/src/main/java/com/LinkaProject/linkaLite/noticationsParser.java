import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

public class notificationsParser {

    public static List<Message> parseJson(String jsonRaw) {
        List<Message> messageList = new ArrayList<Message>();

        try {
            // 1. Instancia o Array JSON
            JSONArray jsonArray = new JSONArray(jsonRaw);

            // 2. Percorre cada item do Array
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                // 3. Extrai as chaves do objeto
                int id = obj.getInt("id");
                String content = obj.getString("content");
                String datetime = obj.getString("datetime");
                String fromUser = obj.getString("from_user");
                String receiver = obj.getString("receiver");
                int read = obj.getInt("read");
                String type = obj.getString("type");

                MessageNotifications message = new Message(id, content, datetime, fromUser, receiver, read, type);
                messageList.add(message);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return messageList;
    }
}