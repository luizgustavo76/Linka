package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ImageButton;
public class ViewPicture extends Activity {

    private ImageView imageView;
    private Button btnDownload;
    private Handler handler;
    private ImageButton btnHome;
    private ImageButton btnChat;
    private ImageButton btnOptions;
    private ImageButton btnProfile;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_picture);
        btnHome = (ImageButton) findViewById(R.id.btnHome);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        btnOptions = (ImageButton) findViewById(R.id.btnOptions);
        handler = new Handler();
        imageView = (ImageView) findViewById(R.id.imageView);
        btnDownload = (Button) findViewById(R.id.btnDownload);
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewPicture.this, HomeActivity.class);
                startActivity(intent);
            }
        });
        btnOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewPicture.this, optionActivity.class);
                startActivity(intent);
            }
        });
        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewPicture.this, profile.class);
                startActivity(intent);
            }
        });
        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewPicture.this, chatActivity.class);
                startActivity(intent);
            }
        });
        Intent intent = getIntent();
        String type = intent.getStringExtra("type");
        ImageLoader ImageClass = new ImageLoader();

        if ("profile picture".equals(type)) {
            String username = intent.getStringExtra("username");
            ImageClass.viewProfilePicture(ViewPicture.this, username, imageView);
        } else {
            String url = intent.getStringExtra("url");
            ImageClass.LoadImageUrl(url, imageView);
        }

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImageInSd();
            }
        });
    }

    private void saveImageInSd() {
        if (imageView.getDrawable() == null) {
            Toast.makeText(ViewPicture.this, "Wait the image load", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                    Bitmap bitmap = drawable.getBitmap();

                    if (bitmap != null) {
                        String titulo = "linka_" + System.currentTimeMillis();
                        
                        final String savedUrl = MediaStore.Images.Media.insertImage(
                                getContentResolver(),
                                bitmap,
                                titulo,
                                "Image saved in gallery"
                        );

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (savedUrl != null) {
                                    Toast.makeText(ViewPicture.this, "Image saved in gallery!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(ViewPicture.this, "Error in saving photo in SDCard.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ViewPicture.this, "Failed in save.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }
}