package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

public class CommunityFeedActivity extends Activity {

    private ListView listViewFeed;
    private ArrayList<JSONObject> postsList = new ArrayList<JSONObject>();
    private FeedAdapter adapter;
    private String communityId = "";
    private String serverUrl = "http://patient-hall-2915.luizsgustavo76.workers.dev/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_feed);

        listViewFeed = (ListView) findViewById(R.id.listViewFeed);
        adapter = new FeedAdapter(this, postsList);
        if (listViewFeed != null) {
            listViewFeed.setAdapter(adapter);
        }

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("community_id")) {
            communityId = intent.getStringExtra("community_id");
        }

        try {
            config cfg = new config();
            String rawCfg = cfg.loadCfgAsJson(this, "config.cfg");
            if (rawCfg != null && rawCfg.length() > 0) {
                JSONObject jsonCfg = new JSONObject(rawCfg);
                JSONObject server = jsonCfg.optJSONObject("SERVER");
                if (server != null) {
                    String cfgUrl = server.optString("url", "").trim();
                    if (cfgUrl.length() > 0) {
                        serverUrl = cfgUrl;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        new FetchCommunityPostsTask().execute();
    }

    private class FetchCommunityPostsTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                String baseUrl = serverUrl.trim();
                while (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                String targetUrl = baseUrl + "/view-posts";

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("community_id", communityId);

                return request.requestHTTP(targetUrl, "POST", jsonParam, 0, CommunityFeedActivity.this);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (isFinishing()) return;

            if (result != null && result.trim().length() > 0) {
                try {
                    String trimmed = result.trim();
                    if (trimmed.startsWith("[")) {
                        JSONArray jsonArray = new JSONArray(trimmed);
                        postsList.clear();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            postsList.add(jsonArray.getJSONObject(i));
                        }
                    } else if (trimmed.startsWith("{")) {
                        JSONObject errObj = new JSONObject(trimmed);
                        String msg = errObj.optString("error", errObj.optString("status", "Error loading community posts"));
                        Toast.makeText(CommunityFeedActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(CommunityFeedActivity.this, "Failed to load community feed", Toast.LENGTH_SHORT).show();
            }
            adapter.notifyDataSetChanged();
        }
    }

    private class FeedAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<JSONObject> list;
        private ImageLoader imageLoader;

        public FeedAdapter(Context context, ArrayList<JSONObject> list) {
            this.context = context;
            this.list = list;
            this.imageLoader = new ImageLoader();
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.item_post, parent, false);

                holder = new ViewHolder();
                holder.avatarPost = (ImageView) convertView.findViewById(R.id.postAvatar);
                holder.tvUsername = (TextView) convertView.findViewById(R.id.postUsername);
                holder.tvText = (TextView) convertView.findViewById(R.id.postText);
                holder.tvDate = (TextView) convertView.findViewById(R.id.postDate);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            try {
                JSONObject post = list.get(position);
                String username = post.optString("username", post.optString("user", "entity404"));
                String textPost = post.optString("text_post", post.optString("text", ""));
                String datetime = post.optString("datetime", post.optString("date", ""));

                holder.tvUsername.setText(username.startsWith("@") ? username : "@" + username);
                holder.tvText.setText(textPost);
                holder.tvDate.setText(datetime);

                imageLoader.viewProfilePicture(context, username, holder.avatarPost);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return convertView;
        }
    }

    static class ViewHolder {
        ImageView avatarPost;
        TextView tvUsername;
        TextView tvText;
        TextView tvDate;
    }
}