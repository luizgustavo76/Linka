package com.LinkaProject.linkaLite;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONException;
public class InboxAdapter extends BaseAdapter {
    private Context context;
    private List<InboxItem> itemList;
    private LayoutInflater inflater;
    private String url = "";
    private String username = "";
    public InboxAdapter(Context context, List<InboxItem> itemList) {
        this.context = context;
        this.itemList = itemList;
        this.inflater = LayoutInflater.from(context);
    }
    @Override
    public int getCount() {
        return itemList.size();
    }
    @Override
    public Object getItem(int position) {
        return itemList.get(position);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    static class ViewHolder {
        ImageView imgAvatar;
        TextView txtUsername;
        TextView txtMessage;
        Button btnAccept;
        Button btnDenied;
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        try{
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(context, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.getJSONObject("SERVER");
            url = server.getString("url").toString();
            username = fastLogin.getString("username").toString();
        }catch(JSONException e){
            e.printStackTrace();
        }
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_inbox, parent, false);
            holder = new ViewHolder();
            holder.imgAvatar = (ImageView) convertView.findViewById(R.id.imgAvatar);
            holder.txtUsername = (TextView) convertView.findViewById(R.id.username);
            holder.txtMessage = (TextView) convertView.findViewById(R.id.message);
            holder.btnAccept = (Button) convertView.findViewById(R.id.btnAccept);
            holder.btnDenied = (Button) convertView.findViewById(R.id.btnDenied);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final InboxItem item = itemList.get(position);
        holder.txtUsername.setText(item.getUsername());
        holder.txtMessage.setText(item.getMessage());
        holder.btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    JSONObject jsonAccept = new JSONObject();
                    jsonAccept.put("receiver", username);
                    jsonAccept.put("remittee", item.getUsername());
                    request.requestHTTP(url + "/accept", "post", jsonAccept, context);
                }catch(Exception e){
                    e.printStackTrace();
                }
                Toast.makeText(context, "Accept " + item.getUsername(), Toast.LENGTH_SHORT).show();
                itemList.remove(position);
                notifyDataSetChanged();
            }
        });
        holder.btnDenied.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    JSONObject jsonDenied = new JSONObject();
                    jsonDenied.put("receiver", username);
                    jsonDenied.put("remittee", item.getUsername());
                    request.requestHTTP(url + "/denied", "post", jsonDenied, context);
                }catch(Exception e){
                    e.printStackTrace();
                }
                Toast.makeText(context, "Denied " + item.getUsername(), Toast.LENGTH_SHORT).show();                
                itemList.remove(position);
                notifyDataSetChanged();
            }
        });
        return convertView;
    }
}