package com.LinkaProject.linkaLite;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

import java.util.List;

public class ImageGalleryAdapter extends BaseAdapter {
    private Context context;
    private List<String> imageUrls;

    public ImageGalleryAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    @Override
    public int getCount() {
        return imageUrls != null ? imageUrls.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return imageUrls.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;

        if (convertView == null) {
            imageView = new ImageView(context);
            imageView.setLayoutParams(new Gallery.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, 
                ViewGroup.LayoutParams.FILL_PARENT
            ));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else {
            imageView = (ImageView) convertView;
        }

        String imageUrl = imageUrls.get(position);
        return imageView;
    }
}