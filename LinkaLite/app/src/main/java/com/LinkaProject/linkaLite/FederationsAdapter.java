package com.LinkaProject.linkaLite;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class FederationsAdapter extends BaseAdapter {
    private Context context;
    private List<FederationItem> items;
    private LayoutInflater inflater;
    private ExecutorService executorService;
    public FederationsAdapter(Context context, List<FederationItem> items) {
        this.context = context;
        this.items = items;
        this.inflater = LayoutInflater.from(context);
        this.executorService = Executors.newFixedThreadPool(4); // Para baixar imagens em background
    }
    @Override
    public int getCount() {
        return items.size();
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
        Button btnFederation;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_federations, parent, false);
            holder = new ViewHolder();
            holder.coverFederation = (ImageView) convertView.findViewById(R.id.coverFederation);
            holder.btnFederation = (Button) convertView.findViewById(R.id.btnFederation);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final FederationItem item = items.get(position);
        holder.btnFederation.setText(item.getName());
        holder.btnFederation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String link = item.getUrl();
                if (link.startsWith("http://") || link.startsWith("https://")) {
                    Intent intent = new Intent(v.getContext(), FederationsFeed.class);
                    intent.putExtra("url", link);
                    v.getContext().startActivity(intent);
                } else {
                    Toast.makeText(context, "URL: " + link, Toast.LENGTH_SHORT).show();
                }
            }
        });
        // Tratamento da Imagem (Base64 vs HTTP)
        String imageSrc = item.getCoverImage();
        holder.coverFederation.setImageBitmap(null); // Limpa imagem antiga
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
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    final Bitmap bitmap = downloadBitmap(imageSrc);
                    if (bitmap != null) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                targetImageView.setImageBitmap(bitmap);
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