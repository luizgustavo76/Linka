package com.LinkaProject.linkaLite;

import android.app.ActivityGroup;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_]+(@[a-zA-Z0-9_.-]+)?)");

    private void highlightMentions(final Context context, TextView textView, String text) {
        SpannableString spannableString = new SpannableString(text);
        Matcher matcher = MENTION_PATTERN.matcher(text);

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
                    ds.setColor(Color.parseColor("#0066CC"));
                    ds.setUnderlineText(false);
                }
            };

            spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        textView.setText(spannableString);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void personalizeTimeline(View view) {
        Intent intent = new Intent(HomeActivity.this, MainEditTimeline.class);
        startActivity(intent);
    }

    private ImageButton btnHamburger, newPost, btnInbox, btnProfile;
    private Button btnClose, btnLogin;
    private HorizontalScrollView scrollViewPages;
    private TextView tabPrev, tabCurrent, tabNext;
    private int screenWidth;
    private int currentPage = 0;
    private String[] pageTitles = new String[]{"Timeline", "Local Feed", "Chat", "Options"};
    private String url = "";
    private ListView listViewTimeline, listViewLocal;
    private PostAdapter adapterTimeline, adapterLocal;
    private ArrayList<JSONObject> postsTimeline = new ArrayList<JSONObject>();
    private ArrayList<JSONObject> postsLocal = new ArrayList<JSONObject>();
    private String actualTab = "";
    private View footerTimeline, footerLocal;
    private JSONArray tags = new JSONArray();
    private boolean isGuest = false;
    private ScheduledExecutorService scheduler, schedulerNotifications;
    private DatabaseHelper dbHelper;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        dbHelper = new DatabaseHelper(this);

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

        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        setupDrawerMenu();
        
        btnHamburger = (ImageButton) findViewById(R.id.btnHamburger);
        if (btnHamburger != null) {
            btnHamburger.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

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
                    startActivity(new Intent(HomeActivity.this, LoginActivity.class));
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

        if (listViewTimeline != null) {
            listViewTimeline.setScrollingCacheEnabled(false);
            listViewTimeline.setAnimationCacheEnabled(false);
        }

        if (listViewLocal != null) {
            listViewLocal.setScrollingCacheEnabled(false);
            listViewLocal.setAnimationCacheEnabled(false);
        }

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

    private void setupDrawerMenu() {
        LinearLayout drawerHeader = (LinearLayout) findViewById(R.id.drawerHeader);
        ImageView imgProfile = (ImageView) findViewById(R.id.imgProfile);
        TextView txtDisplayName = (TextView) findViewById(R.id.txtDisplayName);
        TextView txtUsername = (TextView) findViewById(R.id.txtUsername);

        Button btnDrawerFeed = (Button) findViewById(R.id.btnDrawerFeed);
        Button btnDrawerChat = (Button) findViewById(R.id.btnDrawerChat);
        Button btnDrawerInbox = (Button) findViewById(R.id.btnDrawerInbox);
        Button btnDrawerProfile = (Button) findViewById(R.id.btnDrawerProfile);
        Button btnDrawerFavorites = (Button) findViewById(R.id.btnDrawerFavorites);
        Button btnDrawerLogout = (Button) findViewById(R.id.btnDrawerLogout);
        Button btnCommunities = (Button) findViewById(R.id.btnCommunities);
        String currentUsername = "user";

        try {
            config cfg = new config();
            String rawCfg = cfg.loadCfgAsJson(HomeActivity.this, "config.cfg");
            if (rawCfg != null && rawCfg.length() > 0) {
                JSONObject jsonCfg = new JSONObject(rawCfg);
                JSONObject userObj = jsonCfg.optJSONObject("FAST_LOGIN");
                if (userObj != null) {
                    currentUsername = userObj.optString("username", "user");
                    if (txtUsername != null) txtUsername.setText("@" + currentUsername);
                    if (txtDisplayName != null) txtDisplayName.setText(currentUsername);
                    if (imgProfile != null) {
                        ImageLoader loader = new ImageLoader();
                        loader.viewProfilePicture(HomeActivity.this, currentUsername, imgProfile);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        final String finalUsername = currentUsername;
        if (drawerHeader != null) {
            drawerHeader.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout != null) drawerLayout.closeDrawers();
                    Intent intent = new Intent(HomeActivity.this, ViewProfile.class);
                    intent.putExtra("usernameProfile", finalUsername);
                    startActivity(intent);
                }
            });
        }

        if (btnDrawerFeed != null) {
            btnDrawerFeed.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout != null) drawerLayout.closeDrawers();
                    scrollToPage(1);
                }
            });
        }
        if (btnCommunities != null){
            btnCommunities.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    Intent intent = new Intent(HomeActivity.this, CommunityIndex.class);
                    startActivity(intent);
                }
            });
        }
        if (btnDrawerChat != null) {
            btnDrawerChat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout != null) drawerLayout.closeDrawers();
                    scrollToPage(2);
                }
            });
        }

        if (btnDrawerInbox != null) {
            btnDrawerInbox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout != null) drawerLayout.closeDrawers();
                    startActivity(new Intent(HomeActivity.this, InboxActivity.class));
                }
            });
        }

        if (btnDrawerProfile != null) {
            btnDrawerProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout != null) drawerLayout.closeDrawers();
                    startActivity(new Intent(HomeActivity.this, profile.class));
                }
            });
        }

        if (btnDrawerFavorites != null) {
            btnDrawerFavorites.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout != null) drawerLayout.closeDrawers();
                    startActivity(new Intent(HomeActivity.this, BookmarksFeed.class));
                }
            });
        }

        if (btnDrawerLogout != null) {
            btnDrawerLogout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout != null) drawerLayout.closeDrawers();
                    Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        }
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

    private void clearDatabaseCache() {
        if (postsLocal != null) {
            postsLocal.clear();
        }
        if (adapterLocal != null) {
            adapterLocal.notifyDataSetChanged();
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
            clearDatabaseCache();
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

            if (targetList == null || targetAdapter == null) return;

            if (result == null || result.trim().length() == 0) {
                targetAdapter.notifyDataSetChanged();
                return;
            }

            try {
                String trimmed = result.trim();
                if (trimmed.startsWith("[")) {
                    JSONArray jsonArray = new JSONArray(trimmed);
                    targetList.clear();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        targetList.add(jsonArray.getJSONObject(i));
                    }
                } else if (trimmed.startsWith("{")) {
                    JSONObject errObj = new JSONObject(trimmed);
                    String msg = errObj.optString("error", errObj.optString("status", "Error loading feed"));
                    Toast.makeText(HomeActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            targetAdapter.notifyDataSetChanged();
        }
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
                final String username = post.optString("username", post.optString("user", "entity404"));
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
                        Intent intent = new Intent(HomeActivity.this, ViewProfile.class);
                        intent.putExtra("usernameProfile", username);
                        startActivity(intent);
                    }
                });

                holder.btnComments.setOnClickListener(new View.OnClickListener() {
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
                        intent.putExtra("post_id", String.valueOf(id));
                        startActivity(intent);
                    }
                });

                holder.txtCommentCount.setText(commentsCount);
                holder.tvUsername.setText(username.startsWith("@") ? username : "@" + username);
                holder.tvDate.setText(relative_datetime.length() > 0 ? relative_datetime : datetime);

                if ("timeline".equals(actualTab)) {
                    String avatarUrl = post.optString("avatar_url", "");
                    if (avatarUrl.startsWith("http")) {
                        String avatarOtimizado = "http://wsrv.nl/?url=" + avatarUrl + "&w=48&h=48&fit=cover&output=jpg";
                        holder.avatarPost.setTag(avatarOtimizado);
                        imageLoader.LoadImageUrl(avatarOtimizado, holder.avatarPost);
                    } else {
                        holder.avatarPost.setTag(avatarUrl);
                        imageLoader.LoadImageUrl(avatarUrl, holder.avatarPost);
                    }
                } else {
                    holder.avatarPost.setTag("avatar_" + username);
                    imageLoader.viewProfilePicture(context, username, holder.avatarPost);
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
                    Intent intent = new Intent(HomeActivity.this, ViewPicture.class);
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