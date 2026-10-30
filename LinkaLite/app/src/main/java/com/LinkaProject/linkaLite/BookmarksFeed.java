package com.LinkaProject.linkaLite;
import com.LinkaProject.linkaLite.R;    
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BookmarksFeed extends Activity {
    private String username = "";
    private String url = "";
    private ListView listView;
    private int screenWidth = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bookmarks_feed);

        // Obter largura da tela para renderização de mídias
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;

        listView = (ListView) findViewById(R.id.listViewBookmarks); // Certifique-se de ter essa ID no seu R.layout.bookmark_feed

        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(BookmarksFeed.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.optJSONObject("FAST_LOGIN");
            JSONObject server = jsonCfg.optJSONObject("SERVER");

            if (server != null) url = server.optString("url", "");
            if (fastLogin != null) username = fastLogin.optString("username", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            JSONObject jsonBookmark = new JSONObject();
            jsonBookmark.put("username", username);
            jsonBookmark.put("receiver", username);

            String responseStr = request.requestHTTP(url + "/view-bookmarks", "post", jsonBookmark, BookmarksFeed.this);
            JSONObject jsonResult = new JSONObject(responseStr);

            ArrayList<JSONObject> postsList = new ArrayList<JSONObject>();
            JSONArray postsArray = jsonResult.optJSONArray("posts");
            if (postsArray == null) {
                postsArray = jsonResult.optJSONArray("bookmarks");
            }

            if (postsArray != null) {
                for (int i = 0; i < postsArray.length(); i++) {
                    postsList.add(postsArray.getJSONObject(i));
                }
            }

            PostAdapter adapter = new PostAdapter(BookmarksFeed.this, postsList);
            if (listView != null) {
                listView.setAdapter(adapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void highlightMentions(Context context, TextView textView, String text) {
        SpannableString spannableString = new SpannableString(text);
        Pattern pattern = Pattern.compile("(@[A-Za-z0-9_.-]+)|(#[A-Za-z0-9_.-]+)");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            final String match = matcher.group();
            int start = matcher.start();
            int end = matcher.end();

            spannableString.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    Toast.makeText(widget.getContext(), match, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(Color.BLUE);
                    ds.setUnderlineText(false);
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        textView.setText(spannableString);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    static class ViewHolder {
        ImageView avatarPost;
        RelativeLayout containerMedia;
        GifImageView imgPost;
        TextView txtMediaCount;
        Button btnPrevImage;
        Button btnNextImage;
        TextView tvUsername;
        TextView tvText;
        TextView tvDate;
        ImageButton btnComments;
        TextView txtCommentCount;
        ImageButton btnBookmarks;
        LinearLayout containerBookmarkOptions;
        Button btnPublicBookmarks;
        Button btnPrivateBookmarks;
        ImageButton btnCloseBookmarks;
    }

    private class PostAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<JSONObject> list;
        private ImageLoader imageLoader;

        public PostAdapter(Context context, ArrayList<JSONObject> list) {
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
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.item_post, parent, false);

                holder = new ViewHolder();
                holder.avatarPost = (ImageView) convertView.findViewById(R.id.postAvatar);
                holder.containerMedia = (RelativeLayout) convertView.findViewById(R.id.containerMedia);
                holder.imgPost = (GifImageView) convertView.findViewById(R.id.imgPost);
                holder.txtMediaCount = (TextView) convertView.findViewById(R.id.txtMediaCount);
                holder.btnPrevImage = (Button) convertView.findViewById(R.id.btnPrevImage);
                holder.btnNextImage = (Button) convertView.findViewById(R.id.btnNextImage);
                holder.tvUsername = (TextView) convertView.findViewById(R.id.postUsername);
                holder.tvText = (TextView) convertView.findViewById(R.id.postText);
                holder.tvDate = (TextView) convertView.findViewById(R.id.postDate);
                holder.btnComments = (ImageButton) convertView.findViewById(R.id.btnComments);
                holder.txtCommentCount = (TextView) convertView.findViewById(R.id.txtCommentCount);
                holder.btnBookmarks = (ImageButton) convertView.findViewById(R.id.btnBookmarks);
                holder.containerBookmarkOptions = (LinearLayout) convertView.findViewById(R.id.containerBookmarkOptions);
                holder.btnPublicBookmarks = (Button) convertView.findViewById(R.id.btnPublicBookmarks);
                holder.btnPrivateBookmarks = (Button) convertView.findViewById(R.id.btnPrivateBookmarks);
                holder.btnCloseBookmarks = (ImageButton) convertView.findViewById(R.id.btnCloseBookmarks);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final Bookmark bookmarkManager = new Bookmark(context);

            try {
                final JSONObject post = list.get(position);
                final String postUsername = post.optString("username", post.optString("user", "entity404"));
                String textPost = post.optString("text_post", post.optString("text", ""));
                final String origin_server = post.optString("origin_server", "");
                String datetime = post.optString("datetime", post.optString("date", ""));
                String commentsCount = post.optString("comments_count", post.optString("comment_count", "0"));
                final String postUniqueId = post.optString("unique_id", "");
                final String relative_datetime = post.optString("relative_datetime", "");
                final int id = post.optInt("id", -1);
                final String originalUrl = post.optString("original_url", "");
                final boolean isExternal = !origin_server.isEmpty() && !origin_server.equals("linka");

                holder.btnBookmarks.setSelected(false);
                if (holder.containerBookmarkOptions != null) {
                    holder.containerBookmarkOptions.setVisibility(View.GONE);
                }

                holder.avatarPost.setTag(null);
                holder.avatarPost.setImageDrawable(null);
                holder.imgPost.setTag(R.id.imgPost, null);
                holder.imgPost.setImageDrawable(null);
                holder.imgPost.setMovie(null);

                holder.containerMedia.setVisibility(View.GONE);
                holder.txtMediaCount.setVisibility(View.GONE);
                holder.btnPrevImage.setVisibility(View.GONE);
                holder.btnNextImage.setVisibility(View.GONE);

                holder.btnBookmarks.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.setSelected(!v.isSelected());
                        if (holder.containerBookmarkOptions != null) {
                            if (holder.containerBookmarkOptions.getVisibility() != View.VISIBLE) {
                                Animation slideIn = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
                                holder.containerBookmarkOptions.setVisibility(View.VISIBLE);
                                holder.containerBookmarkOptions.startAnimation(slideIn);
                            } else {
                                Animation slideOut = AnimationUtils.loadAnimation(context, R.anim.slide_out_right);
                                slideOut.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        holder.containerBookmarkOptions.setVisibility(View.GONE);
                                    }
                                    @Override public void onAnimationStart(Animation animation) {}
                                    @Override public void onAnimationRepeat(Animation animation) {}
                                });
                                holder.containerBookmarkOptions.startAnimation(slideOut);
                            }
                        }
                    }
                });

                if (holder.btnCloseBookmarks != null) {
                    holder.btnCloseBookmarks.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (holder.containerBookmarkOptions != null && holder.containerBookmarkOptions.getVisibility() == View.VISIBLE) {
                                Animation slideOut = AnimationUtils.loadAnimation(context, R.anim.slide_out_right);
                                slideOut.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        holder.containerBookmarkOptions.setVisibility(View.GONE);
                                    }
                                    @Override public void onAnimationStart(Animation animation) {}
                                    @Override public void onAnimationRepeat(Animation animation) {}
                                });
                                holder.containerBookmarkOptions.startAnimation(slideOut);
                            }
                        }
                    });
                }

                if (holder.btnPublicBookmarks != null) {
                    holder.btnPublicBookmarks.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            holder.btnBookmarks.setSelected(true);
                            if (holder.containerBookmarkOptions != null) {
                                holder.containerBookmarkOptions.setVisibility(View.GONE);
                            }
                            bookmarkManager.toggle(id, true, isExternal, postUniqueId, origin_server);
                        }
                    });
                }

                if (holder.btnPrivateBookmarks != null) {
                    holder.btnPrivateBookmarks.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            holder.btnBookmarks.setSelected(true);
                            if (holder.containerBookmarkOptions != null) {
                                holder.containerBookmarkOptions.setVisibility(View.GONE);
                            }
                            bookmarkManager.toggle(id, false, isExternal, postUniqueId, origin_server);
                        }
                    });
                }

                holder.tvUsername.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(BookmarksFeed.this, ViewProfile.class);
                        intent.putExtra("usernameProfile", postUsername);
                        startActivity(intent);
                    }
                });

                holder.btnComments.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(BookmarksFeed.this, comments_activity.class);
                        intent.putExtra("isExternal", isExternal);
                        intent.putExtra("externalUrl", origin_server);
                        intent.putExtra("uniqueId", postUniqueId);
                        intent.putExtra("originalUrl", originalUrl);
                        intent.putExtra("post_id", String.valueOf(id));
                        startActivity(intent);
                    }
                });

                holder.txtCommentCount.setText(commentsCount);
                holder.tvUsername.setText(postUsername.startsWith("@") ? postUsername : "@" + postUsername);
                holder.tvDate.setText(relative_datetime.length() > 0 ? relative_datetime : datetime);

                String avatarUrl = post.optString("avatar_url", "");
                if (avatarUrl.startsWith("http")) {
                    String avatarOtimizado = "http://wsrv.nl/?url=" + avatarUrl + "&w=48&h=48&fit=cover&output=jpg";
                    holder.avatarPost.setTag(avatarOtimizado);
                    imageLoader.LoadImageUrl(avatarOtimizado, holder.avatarPost);
                } else {
                    holder.avatarPost.setTag(avatarUrl);
                    imageLoader.LoadImageUrl(avatarUrl, holder.avatarPost);
                }

                final ArrayList<JSONObject> mediaItems = new ArrayList<JSONObject>();
                JSONArray mediaList = post.optJSONArray("media_list");
                if (mediaList == null) mediaList = post.optJSONArray("media_attachments");

                if (mediaList != null && mediaList.length() > 0) {
                    for (int i = 0; i < mediaList.length(); i++) {
                        mediaItems.add(mediaList.getJSONObject(i));
                    }
                } else {
                    JSONObject singleMedia = post.optJSONObject("media");
                    if (singleMedia != null && singleMedia.optString("media_url", "").length() > 0) {
                        mediaItems.add(singleMedia);
                    }
                }

                if (mediaItems.size() > 0) {
                    holder.containerMedia.setVisibility(View.VISIBLE);
                    renderMediaItem(holder.imgPost, mediaItems, 0, holder.txtMediaCount, holder.btnPrevImage, holder.btnNextImage);

                    if (mediaItems.size() > 1) {
                        holder.txtMediaCount.setVisibility(View.VISIBLE);
                        updateCarouselControls(0, mediaItems.size(), holder.txtMediaCount, holder.btnPrevImage, holder.btnNextImage);

                        holder.btnPrevImage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Integer currIdx = (Integer) holder.imgPost.getTag(R.id.imgPost);
                                if (currIdx == null) currIdx = 0;
                                if (currIdx > 0) {
                                    currIdx--;
                                    renderMediaItem(holder.imgPost, mediaItems, currIdx, holder.txtMediaCount, holder.btnPrevImage, holder.btnNextImage);
                                }
                            }
                        });

                        holder.btnNextImage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Integer currIdx = (Integer) holder.imgPost.getTag(R.id.imgPost);
                                if (currIdx == null) currIdx = 0;
                                if (currIdx < mediaItems.size() - 1) {
                                    currIdx++;
                                    renderMediaItem(holder.imgPost, mediaItems, currIdx, holder.txtMediaCount, holder.btnPrevImage, holder.btnNextImage);
                                }
                            }
                        });
                    }
                }

                String cleanText = textPost;
                int imgTagIdx = cleanText.indexOf("[IMAGE]");
                if (imgTagIdx != -1) {
                    cleanText = cleanText.substring(0, imgTagIdx).trim();
                } else {
                    cleanText = cleanText.trim();
                }

                if (cleanText.length() == 0) {
                    holder.tvText.setVisibility(View.GONE);
                } else {
                    holder.tvText.setVisibility(View.VISIBLE);
                    highlightMentions(context, holder.tvText, cleanText);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return convertView;
        }

        private void renderMediaItem(GifImageView imgPost, ArrayList<JSONObject> mediaItems, int index,
                                     TextView txtMediaCount, Button btnPrev, Button btnNext) {
            JSONObject mediaObj = mediaItems.get(index);
            String rawUrl = mediaObj.optString("media_url", "");
            String mediaType = mediaObj.optString("media_type", "").toLowerCase();
            String width = mediaObj.optString("width", "");
            String height = mediaObj.optString("heigth", "");

            imgPost.setTag(R.id.imgPost, index);

            ViewGroup.LayoutParams params = imgPost.getLayoutParams();
            if (screenWidth > 0) {
                params.width = screenWidth;
            }

            try {
                if (width.length() > 0 && height.length() > 0 && !width.equals("null") && !height.equals("null")) {
                    int origW = Integer.parseInt(width);
                    int origH = Integer.parseInt(height);
                    if (origW > 0 && screenWidth > 0) {
                        params.height = (screenWidth * origH) / origW;
                    } else {
                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                } else {
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
            } catch (Exception e) {
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            }

            imgPost.setLayoutParams(params);

            boolean isGifMedia = "gif".equals(mediaType) || rawUrl.toLowerCase().endsWith(".gif");

            final String finalProxyUrl;
            if (rawUrl.contains("/midia/") || rawUrl.startsWith("http://linkaProject.pythonanywhere.com") || rawUrl.startsWith("https://linkaProject.pythonanywhere.com")) {
                finalProxyUrl = rawUrl;
            } else if (isGifMedia) {
                finalProxyUrl = rawUrl;
            } else {
                if (rawUrl.startsWith("http")) {
                    finalProxyUrl = "http://wsrv.nl/?url=" + rawUrl + "&w=480&q=70&output=jpg";
                } else {
                    finalProxyUrl = rawUrl;
                }
            }

            imgPost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(BookmarksFeed.this, ViewPicture.class);
                    intent.putExtra("type", "Image");
                    intent.putExtra("url", rawUrl);
                    startActivity(intent);
                }
            });

            imgPost.setTag(finalProxyUrl);
            imageLoader.LoadImageUrl(finalProxyUrl, imgPost);

            updateCarouselControls(index, mediaItems.size(), txtMediaCount, btnPrev, btnNext);
        }

        private void updateCarouselControls(int index, int total, TextView txtCount, Button btnPrev, Button btnNext) {
            if (total > 1) {
                txtCount.setText((index + 1) + "/" + total);
                btnPrev.setVisibility(index > 0 ? View.VISIBLE : View.GONE);
                btnNext.setVisibility(index < total - 1 ? View.VISIBLE : View.GONE);
            } else {
                txtCount.setVisibility(View.GONE);
                btnPrev.setVisibility(View.GONE);
                btnNext.setVisibility(View.GONE);
            }
        }
    }
}