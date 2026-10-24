package com.LinkaProject.linkaLite;

import android.app.ActivityGroup;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.graphics.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HomeActivity extends ActivityGroup {

    private void highlightMentions(final Context context, TextView textView, String text) {
        SpannableString spannableString = new SpannableString(text);
        Pattern pattern = Pattern.compile("@([a-zA-Z0-9_]+(@[a-zA-Z0-9_.-]+)?)");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            final String mention = matcher.group();
            int start = matcher.start();
            int end = matcher.end();

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    Intent intent = new Intent(context, ViewProfile.class);
                    intent.putExtra("usernameProfile", mention);
                    context.startActivity(intent);
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(Color.parseColor("#1E88E5"));
                    ds.setUnderlineText(false);
                }
            };

            spannableString.setSpan(
                clickableSpan, 
                start, 
                end, 
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        textView.setText(spannableString);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void personalizeTimeline(View view){
        Intent intent = new Intent(HomeActivity.this, MainEditTimeline.class);
        startActivity(intent);
    }

    private ImageButton newPost;
    private ImageButton btnInbox;
    private ImageButton btnProfile;
    private Button btnClose;
    private Button btnLogin;
    private HorizontalScrollView scrollViewPages;
    private TextView tabPrev, tabCurrent, tabNext;
    private int screenWidth;
    private int currentPage = 0;
    private String[] pageTitles = new String[]{"Timeline", "Local Feed", "Chat", "Options"};
    private String url = "";
    private ListView listViewTimeline;
    private ListView listViewLocal;
    private PostAdapter adapterTimeline;
    private PostAdapter adapterLocal;
    private ArrayList<JSONObject> postsTimeline = new ArrayList<JSONObject>();
    private ArrayList<JSONObject> postsLocal = new ArrayList<JSONObject>();
    private String actualTab = "";
    private View footerTimeline;
    private View footerLocal;
    private JSONArray tags = new JSONArray();
    private boolean isGuest = false;
    private ScheduledExecutorService scheduler;
    private ScheduledExecutorService schedulerNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Display display = getWindowManager().getDefaultDisplay();
        screenWidth = display.getWidth();

        scrollViewPages = (HorizontalScrollView) findViewById(R.id.scrollViewPages);
        tabPrev = (TextView) findViewById(R.id.tabPrev);
        tabCurrent = (TextView) findViewById(R.id.tabCurrent);
        tabNext = (TextView) findViewById(R.id.tabNext);

        setPageWidth(R.id.pageTimeline);
        setPageWidth(R.id.pageLocal);
        setPageWidth(R.id.pageChat);
        setPageWidth(R.id.pageOptions);

        tabPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage > 0) {
                    currentPage--;
                    scrollToPage(currentPage);
                }
            }
        });

        tabNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage < pageTitles.length - 1) {
                    currentPage++;
                    scrollToPage(currentPage);
                }
            }
        });

        scrollViewPages.setOnTouchListener(new View.OnTouchListener() {
            private float downX;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downX = event.getRawX();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    float upX = event.getRawX();
                    float deltaX = downX - upX;
                    int minSwipeDistance = screenWidth / 4;

                    if (Math.abs(deltaX) > minSwipeDistance) {
                        if (deltaX > 0 && currentPage < pageTitles.length - 1) {
                            currentPage++;
                        } else if (deltaX < 0 && currentPage > 0) {
                            currentPage--;
                        }
                    } else {
                        int scrollX = scrollViewPages.getScrollX();
                        currentPage = (scrollX + (screenWidth / 2)) / screenWidth;
                        if (currentPage < 0) currentPage = 0;
                        if (currentPage >= pageTitles.length) currentPage = pageTitles.length - 1;
                    }

                    scrollToPage(currentPage);
                    return true;
                }
                return false;
            }
        });

        updateTabHeader();

        newPost = (ImageButton) findViewById(R.id.newPost);
        btnInbox = (ImageButton) findViewById(R.id.btnInbox);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        btnClose = (Button) findViewById(R.id.btnClose);
        btnLogin = (Button) findViewById(R.id.btnLogin);

        LinearLayout layoutGuest = (LinearLayout) findViewById(R.id.layoutGuest);
        Intent intent = getIntent();
        isGuest = intent.getBooleanExtra("isGuest", false);
        if (isGuest && layoutGuest != null) {
            layoutGuest.setVisibility(View.VISIBLE);
        }

        if (newPost != null) {
            newPost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HomeActivity.this, newPost.class);
                    intent.putExtra("isGuest", isGuest);
                    startActivity(intent);
                }
            });
        }

        if (btnInbox != null) {
            btnInbox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentPage = 2;
                    scrollToPage(currentPage);
                }
            });
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(HomeActivity.this, profile.class));
                }
            });
        }

        if (btnLogin != null) {
            btnLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (layoutGuest != null) layoutGuest.setVisibility(View.GONE);
                }
            });
        }

        listViewTimeline = (ListView) findViewById(R.id.listViewTimeline);
        listViewLocal = (ListView) findViewById(R.id.listViewLocal);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        footerTimeline = inflater.inflate(R.layout.footer_federations, null);
        footerLocal = inflater.inflate(R.layout.footer_federations, null);

        if (listViewTimeline != null) {
            listViewTimeline.addFooterView(footerTimeline);
            adapterTimeline = new PostAdapter(this, postsTimeline, "timeline");
            listViewTimeline.setAdapter(adapterTimeline);
        }

        if (listViewLocal != null) {
            listViewLocal.addFooterView(footerLocal);
            adapterLocal = new PostAdapter(this, postsLocal, "local");
            listViewLocal.setAdapter(adapterLocal);
        }

        schedulerNotifications = Executors.newSingleThreadScheduledExecutor();
        scheduler = Executors.newSingleThreadScheduledExecutor();

        schedulerNotifications.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                notificationManager.createNotification(HomeActivity.this);
            }
        }, 0, 15, TimeUnit.SECONDS);

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    config cfg = new config();
                    JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(HomeActivity.this, "config.cfg"));
                    JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
                    JSONObject server = jsonCfg.getJSONObject("SERVER");
                    String token = fastLogin.getString("token_session");
                    String url = server.getString("url");
                    tokenManager.valideToken(token, url, HomeActivity.this);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 2, TimeUnit.MINUTES);
    }

    private void setPageWidth(int pageResId) {
        View page = findViewById(pageResId);
        if (page != null) {
            ViewGroup.LayoutParams params = page.getLayoutParams();
            params.width = screenWidth;
            page.setLayoutParams(params);
        }
    }

    private void scrollToPage(final int page) {
        scrollViewPages.post(new Runnable() {
            @Override
            public void run() {
                scrollViewPages.smoothScrollTo(page * screenWidth, 0);
            }
        });
        updateTabHeader();
        loadFeedForTab(pageTitles[page]);
    }

    private void updateTabHeader() {
        tabCurrent.setText(pageTitles[currentPage]);

        if (currentPage > 0) {
            tabPrev.setText("◀ " + pageTitles[currentPage - 1]);
            tabPrev.setVisibility(View.VISIBLE);
        } else {
            tabPrev.setVisibility(View.INVISIBLE);
        }

        if (currentPage < pageTitles.length - 1) {
            tabNext.setText(pageTitles[currentPage + 1] + " ▶");
            tabNext.setVisibility(View.VISIBLE);
        } else {
            tabNext.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFeedForTab(pageTitles[currentPage]);
    }

    private void loadActivityInFrame(String id, Intent intent, FrameLayout container) {
        if (container != null) {
            container.removeAllViews();
            Window window = getLocalActivityManager().startActivity(id, intent);
            View view = window.getDecorView();
            container.addView(view, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.FILL_PARENT));
        }
    }

    private void loadFeedForTab(String tabName) {
        String baseUrl = "http://linkaProject.pythonanywhere.com";
        JSONArray urlsArray = new JSONArray();

        try {
            config cfg = new config();
            String rawCfg = cfg.loadCfgAsJson(HomeActivity.this, "config.cfg");
            if (rawCfg != null && rawCfg.length() > 0) {
                JSONObject jsonCfg = new JSONObject(rawCfg);
                JSONObject server = jsonCfg.optJSONObject("SERVER");
                if (server != null) {
                    baseUrl = server.optString("url", baseUrl);
                    url = baseUrl;
                }
            }

            String rawTimeline = cfg.loadCfgAsJson(HomeActivity.this, "config-timeline.cfg");
            if (rawTimeline != null && rawTimeline.length() > 0) {
                JSONObject jsonTimeline = new JSONObject(rawTimeline);
                JSONObject fedSection = jsonTimeline.optJSONObject("FEDERATION-TIMELINE");
                if (fedSection != null) {
                    String urlsValue = fedSection.optString("urls", "");
                    String[] parts = urlsValue.split(",");
                    for (String part : parts) {
                        String cleanUrl = part.trim();
                        if (cleanUrl.length() > 0) {
                            urlsArray.put(cleanUrl);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if ("Timeline".equals(tabName)) {
            JSONObject body = new JSONObject();
            actualTab = "timeline";
            try {
                body.put("urls", urlsArray);
                body.put("filters", tags);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            new FetchFeedTask("timeline", baseUrl + "/view-external-posts", "POST", body).execute();

        } else if ("Local Feed".equals(tabName)) {
            actualTab = "local";
            new FetchFeedTask("local", baseUrl + "/feed", "GET", null).execute();

        } else if ("Chat".equals(tabName)) {
            FrameLayout pageChat = (FrameLayout) findViewById(R.id.pageChat);
            Intent intent = new Intent(HomeActivity.this, chatActivity.class);
            intent.putExtra("isGuest", isGuest);
            loadActivityInFrame("ChatActivityID", intent, pageChat);

        } else if ("Options".equals(tabName)) {
            FrameLayout pageOptions = (FrameLayout) findViewById(R.id.pageOptions);
            Intent intent = new Intent(HomeActivity.this, optionActivity.class);
            intent.putExtra("isGuest", isGuest);
            loadActivityInFrame("OptionActivityID", intent, pageOptions);
        }
    }

    private class FetchFeedTask extends AsyncTask<Void, Void, String> {
        private String tabType;
        private String url;
        private String method;
        private JSONObject jsonBody;

        public FetchFeedTask(String tabType, String url, String method, JSONObject jsonBody) {
            this.tabType = tabType;
            this.url = url;
            this.method = method;
            this.jsonBody = jsonBody;
        }

        @Override
        protected String doInBackground(Void... params) {
            return requestHTTP(url, method, jsonBody);
        }

        @Override
        protected void onPostExecute(String result) {
            ArrayList<JSONObject> targetList = "timeline".equals(tabType) ? postsTimeline : postsLocal;
            PostAdapter targetAdapter = "timeline".equals(tabType) ? adapterTimeline : adapterLocal;

            if (targetList == null || targetAdapter == null) {
                return;
            }

            targetList.clear();

            if (result == null || result.trim().length() == 0) {
                targetAdapter.notifyDataSetChanged();
                Toast.makeText(HomeActivity.this, "Error loading feed", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                String trimmed = result.trim();
                if (trimmed.startsWith("[")) {
                    JSONArray jsonArray = new JSONArray(trimmed);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        targetList.add(jsonArray.getJSONObject(i));
                    }
                } else if (trimmed.startsWith("{")) {
                    JSONObject errObj = new JSONObject(trimmed);
                    String msg = errObj.optString("error", errObj.optString("status", "Error loading feed"));
                    Toast.makeText(HomeActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(HomeActivity.this, "Error parsing posts", Toast.LENGTH_SHORT).show();
            }

            targetAdapter.notifyDataSetChanged();
        }
    }

    private class PostAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<JSONObject> list;
        private String tabType;
        private ImageLoader imageLoader;

        public PostAdapter(Context context, ArrayList<JSONObject> list, String tabType) {
            this.context = context;
            this.list = list;
            this.tabType = tabType;
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
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.item_post, null);
            }

            ImageView avatarPost = (ImageView) convertView.findViewById(R.id.postAvatar);
            RelativeLayout containerMedia = (RelativeLayout) convertView.findViewById(R.id.containerMedia);
            final GifImageView imgPost = (GifImageView) convertView.findViewById(R.id.imgPost);
            final TextView txtMediaCount = (TextView) convertView.findViewById(R.id.txtMediaCount);
            final Button btnPrevImage = (Button) convertView.findViewById(R.id.btnPrevImage);
            final Button btnNextImage = (Button) convertView.findViewById(R.id.btnNextImage);

            TextView tvUsername = (TextView) convertView.findViewById(R.id.postUsername);
            TextView tvText = (TextView) convertView.findViewById(R.id.postText);
            TextView tvDate = (TextView) convertView.findViewById(R.id.postDate);
            ImageButton btnComments = (ImageButton) convertView.findViewById(R.id.btnComments);
            TextView txtCommentCount = (TextView) convertView.findViewById(R.id.txtCommentCount);

            avatarPost.setTag(null);
            avatarPost.setImageBitmap(null);

            imgPost.setTag(null);
            imgPost.setImageBitmap(null);
            imgPost.setMovie(null); // Reseta o estado do GifImageView nos cards reciclados

            containerMedia.setVisibility(View.GONE);
            txtMediaCount.setVisibility(View.GONE);
            btnPrevImage.setVisibility(View.GONE);
            btnNextImage.setVisibility(View.GONE);

            tvUsername.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HomeActivity.this, ViewProfile.class);
                    JSONObject post = list.get(position);
                    String usernameProfile = post.optString("username", post.optString("user", "entity404"));
                    intent.putExtra("usernameProfile", usernameProfile);
                    startActivity(intent);
                }
            });

            try {
                final JSONObject post = list.get(position);
                String username = post.optString("username", post.optString("user", "entity404"));
                String textPost = post.optString("text_post", post.optString("text", ""));
                final String origin_server = post.optString("origin_server", "");
                String datetime = post.optString("datetime", post.optString("date", ""));
                String commentsCount = post.optString("comments_count", post.optString("comment_count", "0"));
                final String postUniqueId = post.optString("unique_id", "");
                final String relative_datetime = post.optString("relative_datetime", "");

                final String id = post.optString("id", "");
                final String originalUrl = post.optString("original_url", "");

                btnComments.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(HomeActivity.this, comments_activity.class);
                        if ("timeline".equals(actualTab)) {
                            intent.putExtra("isExternal", true);
                            intent.putExtra("externalUrl", origin_server);
                            intent.putExtra("uniqueId", postUniqueId);
                            intent.putExtra("originalUrl", originalUrl);
                        } else {
                            intent.putExtra("isExternal", false);
                        }
                        intent.putExtra("isGuest", isGuest);
                        intent.putExtra("post_id", id);
                        startActivity(intent);
                    }
                });

                txtCommentCount.setText(commentsCount);
                tvUsername.setText(username.startsWith("@") ? username : "@" + username);
                tvDate.setText(relative_datetime + "(" + datetime + ")");

                if ("timeline".equals(actualTab)) {
                    String avatarUrl = post.optString("avatar_url", "");
                    if (avatarUrl.startsWith("http")) {
                        String avatarOtimizado = "http://wsrv.nl/?url=" + avatarUrl + "&w=48&h=48&fit=cover&output=jpg";
                        imageLoader.LoadImageUrl(avatarOtimizado, avatarPost);
                    } else {
                        imageLoader.LoadImageUrl(avatarUrl, avatarPost);
                    }
                } else {
                    imageLoader.viewProfilePicture(context, username, avatarPost);
                }

                // --- MONTAGEM DA LISTA DE MÍDIAS (IMAGENS E GIFS) ---
                final ArrayList<JSONObject> mediaItems = new ArrayList<JSONObject>();
                JSONArray mediaList = post.optJSONArray("media_list");
                if (mediaList == null) {
                    mediaList = post.optJSONArray("media_attachments");
                }

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
                    containerMedia.setVisibility(View.VISIBLE);
                    int initialIndex = 0;
                    renderMediaItem(imgPost, mediaItems, initialIndex, txtMediaCount, btnPrevImage, btnNextImage);

                    if (mediaItems.size() > 1) {
                        txtMediaCount.setVisibility(View.VISIBLE);
                        updateCarouselControls(initialIndex, mediaItems.size(), txtMediaCount, btnPrevImage, btnNextImage);

                        btnPrevImage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Integer currIdx = (Integer) imgPost.getTag(R.id.imgPost);
                                if (currIdx == null) currIdx = 0;
                                if (currIdx > 0) {
                                    currIdx--;
                                    renderMediaItem(imgPost, mediaItems, currIdx, txtMediaCount, btnPrevImage, btnNextImage);
                                }
                            }
                        });

                        btnNextImage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Integer currIdx = (Integer) imgPost.getTag(R.id.imgPost);
                                if (currIdx == null) currIdx = 0;
                                if (currIdx < mediaItems.size() - 1) {
                                    currIdx++;
                                    renderMediaItem(imgPost, mediaItems, currIdx, txtMediaCount, btnPrevImage, btnNextImage);
                                }
                            }
                        });
                    }
                }

                String cleanText = textPost.replaceAll("\\[IMAGE\\].*", "").trim();
                if (cleanText.length() == 0) {
                    tvText.setVisibility(View.GONE);
                } else {
                    tvText.setVisibility(View.VISIBLE);
                    tvText.setText(cleanText);
                    highlightMentions(context, tvText, cleanText);
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
            params.width = screenWidth;

            try {
                if (width.length() > 0 && height.length() > 0 && !width.equals("null") && !height.equals("null")) {
                    int origW = Integer.parseInt(width);
                    int origH = Integer.parseInt(height);
                    if (origW > 0) {
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

            // Detecção se a mídia é um GIF (por tipo no JSON ou extensão do arquivo)
            boolean isGifMedia = "gif".equals(mediaType) || rawUrl.toLowerCase().endsWith(".gif");

            final String finalProxyUrl;
            if (isGifMedia) {
                // GIFs sempre preservam a URL ou passam pelo proxy HTTP do backend sem conversão para JPG
                if ("local".equals(actualTab)) {
                    finalProxyUrl = "http://linkaProject.pythonanywhere.com/lite-render?url=" + rawUrl;
                } else {
                    finalProxyUrl = rawUrl;
                }
            } else {
                // Imagens estáticas (JPEG/PNG)
                if ("local".equals(actualTab)) {
                    finalProxyUrl = "http://linkaProject.pythonanywhere.com/lite-render?url=" + rawUrl;
                } else if ("timeline".equals(actualTab)) {
                    finalProxyUrl = "http://wsrv.nl/?url=" + rawUrl + "&w=640&q=80&output=jpg";
                } else {
                    finalProxyUrl = rawUrl;
                }
            }

            imgPost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HomeActivity.this, ViewPicture.class);
                    intent.putExtra("type", "Image");
                    intent.putExtra("url", finalProxyUrl);
                    startActivity(intent);
                }
            });

            // Dispara o ImageLoader (o ImageLoader detecta a assinatura mágica/Movie e decide entre Bitmap ou GIF)
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

    public String requestHTTP(String urlParam, String method, JSONObject json_body) {
        return request.requestHTTP(urlParam, method, json_body, HomeActivity.this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        if (schedulerNotifications != null && !schedulerNotifications.isShutdown()) {
            schedulerNotifications.shutdownNow();
        }
    }
}