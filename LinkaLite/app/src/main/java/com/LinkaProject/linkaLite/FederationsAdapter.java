package com.LinkaProject.linkaLite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FederationsAdapter extends BaseAdapter {
    private Context context;
    private List<FederationItem> items;
    private List<String> savedUrls;
    private LayoutInflater inflater;
    private ExecutorService executorService;

    public FederationsAdapter(Context context, List<FederationItem> items, List<String> savedUrls) {
        this.context = context;
        this.items = items;
        this.savedUrls = savedUrls;
        this.inflater = LayoutInflater.from(context);
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public FederationsAdapter(Context context, List<FederationItem> items) {
        this(context, items, null);
    }

    @Override
    public int getCount() {
        return items != null ? items.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        ImageView coverFederation;
        CheckBox btnFederation;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_federations, parent, false);
            holder = new ViewHolder();
            holder.coverFederation = (ImageView) convertView.findViewById(R.id.coverFederation);
            holder.btnFederation = (CheckBox) convertView.findViewById(R.id.btnFederation);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final FederationItem item = items.get(position);

        String displayName = item.getName();
        if (displayName == null || displayName.trim().isEmpty() || displayName.equalsIgnoreCase("null")) {
            displayName = item.getUrl();
        }
        if (displayName == null || displayName.trim().isEmpty() || displayName.equalsIgnoreCase("null")) {
            displayName = "Sem nome";
        }
        holder.btnFederation.setText(displayName);

        boolean isSaved = savedUrls != null && item.getUrl() != null && savedUrls.contains(item.getUrl().trim());
        item.setChecked(isSaved);

        holder.btnFederation.setOnCheckedChangeListener(null);
        holder.btnFederation.setChecked(item.isChecked());

        holder.btnFederation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                item.setChecked(isChecked);
                String link = item.getUrl();
                if (link != null && !link.trim().isEmpty()) {
                    config cfg = new config();
                    cfg.toggleUrl(context, link);
                    if (savedUrls != null) {
                        if (isChecked && !savedUrls.contains(link.trim())) {
                            savedUrls.add(link.trim());
                        } else if (!isChecked) {
                            savedUrls.remove(link.trim());
                        }
                    }
                }
            }
        });

        String imageSrc = item.getCoverImage();
        holder.coverFederation.setImageBitmap(null);
        holder.coverFederation.setTag(imageSrc);

        if (imageSrc != null && imageSrc.startsWith("data:image")) {
            try {
                String cleanBase64 = imageSrc.substring(imageSrc.indexOf(",") + 1);
                byte[] decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.coverFederation.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (imageSrc != null && (imageSrc.startsWith("http://") || imageSrc.startsWith("https://"))) {
            final ImageView targetImageView = holder.coverFederation;
            final String currentUrl = imageSrc;

            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    final Bitmap bitmap = downloadBitmap(currentUrl);
                    if (bitmap != null) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (currentUrl.equals(targetImageView.getTag())) {
                                    targetImageView.setImageBitmap(bitmap);
                                }
                            }
                        });
                    }
                }
            });
        }

        return convertView;
    }

    private Bitmap downloadBitmap(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}