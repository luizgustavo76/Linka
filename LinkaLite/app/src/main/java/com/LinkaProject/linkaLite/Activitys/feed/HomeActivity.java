package com.LinkaProject.linkaLite;

import android.app.ActivityGroup;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.MediaController;
import android.media.MediaPlayer;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.graphics.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.graphics.drawable.BitmapDrawable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HomeActivity extends ActivityGroup {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_]+(@[a-zA-Z0-9_.-]+)?)");

    private static VideoView currentPlayingVideo = null;
    private static ImageButton currentPlayButton = null;
    private static View currentThumbnail = null;

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
    private String[] pageTitles = new String[]{"Timeline", "Chat"};
    private String url = "";
    private ListView listViewTimeline;
    private PostAdapter adapterTimeline;
    private ArrayList<JSONObject> postsTimeline = new ArrayList<JSONObject>();
    private String actualTab = "";
    private View footerTimeline;
    private JSONArray tags = new JSONArray();
    private boolean isGuest = false;
    private ScheduledExecutorService scheduler, schedulerNotifications, schedulerFeedPolling;
    private DatabaseHelper dbHelper;
    private DrawerLayout drawerLayout;
    private ImageView logo;
    private ImageButton fabNewPost;
    private int feedPageTimeline = 1;
    private boolean isLoading = false;
    private String actualFeed = "Timeline";
    private boolean hasMoreTimeline = true;
    private Spinner spinnerFeeds;
    private View topHeaderContainer;
    private String username = "";
    private boolean isHeaderVisible = true;
    private boolean isHeaderAnimating = false;
    private int lastFirstVisibleItem = 0;
    private int lastTop = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        try {
            config cfg = new config();
            JSONObject jsonCfg = new JSONObject(cfg.loadCfgAsJson(HomeActivity.this, "config.cfg"));
            JSONObject fastLogin = jsonCfg.getJSONObject("FAST_LOGIN");
            username = fastLogin.optString("username", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        List<String> redesList = new ArrayList<>(Arrays.asList("Timeline", "Linka >", "Communities >", "Friends >", "+ New Feed"));
        Cursor cursor = db.rawQuery("SELECT * from feeds", null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                redesList.add(name);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        String[] redes = redesList.toArray(new String[0]);

        spinnerFeeds = (Spinner) findViewById(R.id.spinner_feeds);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                R.layout.spinner_item_top,
                redes
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFeeds.setAdapter(adapter);

        spinnerFeeds.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectItem = parent.getItemAtPosition(position).toString();
                String newFeed = selectItem.replace(">", "").trim();
                
                if (!newFeed.equals(actualFeed)) {
                    actualFeed = newFeed;
                    postsTimeline.clear();
                    if (adapterTimeline != null) adapterTimeline.notifyDataSetChanged();
                    refreshFeed();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        topHeaderContainer = findViewById(R.id.topHeaderContainer);

        fabNewPost = (ImageButton) findViewById(R.id.fabNewPost);
        fabNewPost.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent(HomeActivity.this, newPost.class);
                startActivity(intent);
            }
        });

        Display display = getWindowManager().getDefaultDisplay();
        screenWidth = display.getWidth();
        logo = (ImageView) findViewById(R.id.logo);
        if (logo != null) {
            BitmapDrawable logoDrawable = (BitmapDrawable) logo.getDrawable();
            logoDrawable.setAntiAlias(true);
            logoDrawable.setFilterBitmap(true);
            logo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    refreshFeed();
                    UIAnimationHelper.rotateWithWeight((ImageView) v);
                }
            });
        }
        UIAnimationHelper.rotateWithWeight(logo);
        scrollViewPages = (HorizontalScrollView) findViewById(R.id.scrollViewPages);
        tabPrev = (TextView) findViewById(R.id.tabPrev);
        tabCurrent = (TextView) findViewById(R.id.tabCurrent);
        tabNext = (TextView) findViewById(R.id.tabNext);

        setPageWidth(R.id.pageTimeline);
        setPageWidth(R.id.pageChat);

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

        if (scrollViewPages != null) {
            scrollViewPages.setOnTouchListener(new View.OnTouchListener() {
                private float startX;
                private float startY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getRawX();
                            startY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            float endX = event.getRawX();
                            float endY = event.getRawY();
                            float deltaX = startX - endX;
                            float deltaY = startY - endY;

                            if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 40) {
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
        }

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
                    currentPage = 1;
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

        AbsListView.OnScrollListener feedScrollListener = new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (!isLoading && (view == listViewTimeline && hasMoreTimeline) 
                    && totalItemCount > 0 && (firstVisibleItem + visibleItemCount >= totalItemCount - 2)) {
                    feedPageTimeline++;
                    loadFeedForTab("Timeline", false, feedPageTimeline);
                }

                if (firstVisibleItem == 0) {
                    View firstChild = view.getChildAt(0);
                    int currentTop = (firstChild == null) ? 0 : firstChild.getTop();
                    if (currentTop >= 0) {
                        showHeaderBar();
                        lastFirstVisibleItem = firstVisibleItem;
                        lastTop = currentTop;
                        return;
                    }
                }

                View firstChild = view.getChildAt(0);
                int currentTop = (firstChild == null) ? 0 : firstChild.getTop();

                if (firstVisibleItem > lastFirstVisibleItem) {
                    hideHeaderBar();
                } else if (firstVisibleItem < lastFirstVisibleItem) {
                    showHeaderBar();
                } else {
                    int diff = currentTop - lastTop;
                    if (diff < -25) {
                        hideHeaderBar();
                    } else if (diff > 25) {
                        showHeaderBar();
                    }
                }

                lastFirstVisibleItem = firstVisibleItem;
                lastTop = currentTop;
            }
        };

        if (listViewTimeline != null) {
            listViewTimeline.setScrollingCacheEnabled(true);
            listViewTimeline.setAnimationCacheEnabled(true);
            listViewTimeline.setOnScrollListener(feedScrollListener);
        }

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        footerTimeline = inflater.inflate(R.layout.footer_federations, null);

        if (listViewTimeline != null) {
            listViewTimeline.addFooterView(footerTimeline);
            adapterTimeline = new PostAdapter(this, postsTimeline, "timeline");
            listViewTimeline.setAdapter(adapterTimeline);
        }

        schedulerNotifications = Executors.newSingleThreadScheduledExecutor();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        schedulerFeedPolling = Executors.newSingleThreadScheduledExecutor();

        schedulerNotifications.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                notificationManager.createNotification(HomeActivity.this);
            }
        }, 0, 60, TimeUnit.SECONDS);

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
        }, 0, 3, TimeUnit.MINUTES);

        schedulerFeedPolling.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        checkNewPosts();
                    }
                });
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    private void hideHeaderBar() {
        if (!isHeaderVisible || isHeaderAnimating || topHeaderContainer == null) return;

        TranslateAnimation anim = new TranslateAnimation(0, 0, 0, -topHeaderContainer.getHeight());
        anim.setDuration(180);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isHeaderAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                topHeaderContainer.setVisibility(View.GONE);
                isHeaderVisible = false;
                isHeaderAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        topHeaderContainer.startAnimation(anim);
    }

    private void showHeaderBar() {
        if (isHeaderVisible || isHeaderAnimating || topHeaderContainer == null) return;

        topHeaderContainer.setVisibility(View.VISIBLE);

        TranslateAnimation anim = new TranslateAnimation(0, 0, -topHeaderContainer.getHeight(), 0);
        anim.setDuration(180);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isHeaderAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isHeaderVisible = true;
                isHeaderAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        topHeaderContainer.startAnimation(anim);
    }

    private void setupDrawerMenu() {
        LinearLayout drawerHeader = (LinearLayout) findViewById(R.id.drawerHeader);
        ImageView imgProfile = (ImageView) findViewById(R.id.imgProfile);
        TextView txtDisplayName = (TextView) findViewById(R.id.txtDisplayName);
        TextView txtUsername = (TextView) findViewById(R.id.txtUsername);

        View btnDrawerFeed = findViewById(R.id.btnDrawerFeed);
        View btnCommunities = findViewById(R.id.btnCommunities);
        View btnDrawerChat = findViewById(R.id.btnDrawerChat);
        View btnDrawerInbox = findViewById(R.id.btnDrawerInbox);
        View btnDrawerProfile = findViewById(R.id.btnDrawerProfile);
        View btnDrawerFavorites = findViewById(R.id.btnDrawerFavorites);
        View btnDrawerOptions = findViewById(R.id.btnDrawerOptions);
        View btnDrawerLogout = findViewById(R.id.btnDrawerLogout);
        String currentUsername = "user";

        if (btnDrawerOptions != null) {
            btnDrawerOptions.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    startActivity(new Intent(HomeActivity.this, optionActivity.class));
                }
            });
        }

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
                    scrollToPage(0);
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
                    scrollToPage(1);
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
        if (page < 0 || page >= pageTitles.length) return;
        currentPage = page;

        if (scrollViewPages != null) {
            scrollViewPages.post(new Runnable() {
                @Override
                public void run() {
                    scrollViewPages.smoothScrollTo(page * screenWidth, 0);
                }
            });
        }
        updateTabHeader();
        loadFeedForTab(pageTitles[page], false, 1);
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
        loadFeedForTab(pageTitles[currentPage], false, 1);
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

    private void checkNewPosts() {
        String tabName = pageTitles[currentPage];
        if ("Timeline".equals(tabName)) {
            loadFeedForTab("Timeline", true, 1);
        }
    }

    public void refreshFeed() {
        if ("Timeline".equals(pageTitles[currentPage])) {
            postsTimeline.clear();
            feedPageTimeline = 1;
            hasMoreTimeline = true;
            if (adapterTimeline != null) adapterTimeline.notifyDataSetChanged();
        }
        loadFeedForTab(pageTitles[currentPage], true, 1);
    }

    private void loadFeedForTab(String tabName, boolean forceReload, int targetPage) {
        if (isLoading) return;

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
            actualTab = "timeline";
            
            if (!forceReload && targetPage == 1 && postsTimeline.size() > 0) {
                if (adapterTimeline != null) adapterTimeline.notifyDataSetChanged();
                return;
            }

            isLoading = true;

            if ("Timeline".equalsIgnoreCase(actualFeed.trim())) {
                JSONObject body = new JSONObject();
                try {
                    body.put("urls", urlsArray);
                    body.put("filters", tags);
                    body.put("page", targetPage);
                    body.put("limit", 20);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                new FetchFeedTask("timeline", baseUrl + "/view-external-posts?page=" + targetPage + "&limit=20", "POST", body, targetPage).execute();
            }
            else if ("Linka".equalsIgnoreCase(actualFeed.trim())) {
                new FetchFeedTask("timeline", baseUrl + "/feed?page=" + targetPage + "&limit=20", "GET", null, targetPage).execute();
            } 
            else if ("Friends".equalsIgnoreCase(actualFeed.trim())){
                try{
                    JSONObject jsonFriends = new JSONObject();
                    jsonFriends.put("username", username);
                    new FetchFeedTask("timeline", baseUrl + "/friends-feed?page=" + targetPage + "&limit=20", "POST", jsonFriends, targetPage).execute();
                }catch(JSONException e){
                    e.printStackTrace();
                }
            }
            else {
                JSONObject body = new JSONObject();
                try {
                    body.put("urls", urlsArray);
                    body.put("filters", tags);
                    body.put("page", targetPage);
                    body.put("limit", 20);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                new FetchFeedTask("timeline", baseUrl + "/view-external-posts?page=" + targetPage + "&limit=20", "POST", body, targetPage).execute();
            }

        } else if ("Chat".equals(tabName)) {
            FrameLayout pageChat = (FrameLayout) findViewById(R.id.pageChat);
            Intent intent = new Intent(HomeActivity.this, chatActivity.class);
            intent.putExtra("isGuest", isGuest);
            loadActivityInFrame("ChatActivityID", intent, pageChat);
        }
    }

    private class FetchFeedTask extends AsyncTask<Void, Void, String> {
        private String tabType;
        private String url;
        private String method;
        private JSONObject jsonBody;
        private int pageRequested;

        public FetchFeedTask(String tabType, String url, String method, JSONObject jsonBody, int pageRequested) {
            this.tabType = tabType;
            this.url = url;
            this.method = method;
            this.jsonBody = jsonBody;
            this.pageRequested = pageRequested;
        }

        @Override
        protected String doInBackground(Void... params) {
            return requestHTTP(url, method, jsonBody);
        }

        @Override
        protected void onPostExecute(String result) {
            isLoading = false;
            ArrayList<JSONObject> targetList = postsTimeline;
            PostAdapter targetAdapter = adapterTimeline;

            if (targetList == null || targetAdapter == null) return;

            if (result == null || result.trim().length() == 0) {
                hasMoreTimeline = false;
                return;
            }

            try {
                String trimmed = result.trim();
                if (trimmed.startsWith("[")) {
                    JSONArray jsonArray = new JSONArray(trimmed);

                    if (jsonArray.length() < 20) {
                        hasMoreTimeline = false;
                    }

                    if (pageRequested == 1) {
                        targetList.clear();
                    }

                    for (int i = 0; i < jsonArray.length(); i++) {
                        targetList.add(jsonArray.getJSONObject(i));
                    }
                    targetAdapter.notifyDataSetChanged();

                } else if (trimmed.startsWith("{")) {
                    JSONObject errObj = new JSONObject(trimmed);
                    String msg = errObj.optString("error", errObj.optString("status", "Error loading feed"));
                    Toast.makeText(HomeActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class ViewHolder {
        ImageView avatarPost;
        RelativeLayout containerMedia;
        GifImageView imgPost;
        VideoView videoPost;
        ImageButton btnPlayVideo;
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
        ImageButton btnReply;
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
                holder.videoPost = (VideoView) convertView.findViewById(R.id.videoPost);
                holder.btnPlayVideo = (ImageButton) convertView.findViewById(R.id.btnPlayVideo);
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
                holder.btnReply = (ImageButton) convertView.findViewById(R.id.btnReply);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final Bookmark bookmarkManager = new Bookmark(context);

            try {
                final JSONObject post = list.get(position);
                final String platform = post.optString("platform", "desconhecida");
                final String postUsername = post.optString("username", post.optString("user", "entity404"));
                String textPost = post.optString("text_post", post.optString("text", ""));
                final String origin_server = post.optString("origin_server", "");
                String datetime = post.optString("datetime", post.optString("date", ""));
                String commentsCount = post.optString("comments_count", post.optString("comment_count", "0"));
                final String postUniqueId = post.optString("unique_id", "");
                final String relative_datetime = post.optString("relative_datetime", "");
                final int id = post.optInt("id", -1);
                final String originalUrl = post.optString("original_url", "");
                
                final boolean isExternal = !origin_server.isEmpty() && !origin_server.equalsIgnoreCase("linka");

                holder.btnReply.setOnClickListener(new View.OnClickListener(){
                    public void onClick(View v){
                        Intent intent = new Intent(HomeActivity.this, newPost.class);
                        intent.putExtra("repostPostId", id);
                        intent.putExtra("repostUser", postUsername);
                        intent.putExtra("repostText", textPost);
                        
                        JSONArray jsonArray = post.optJSONArray("media_list");
                        if (jsonArray != null && jsonArray.length() > 0) {
                            JSONObject media = jsonArray.optJSONObject(0);
                            if (media != null) {
                                intent.putExtra("repostMediaUrl", media.optString("media_url", ""));
                                intent.putExtra("repostMediaWidth", media.optString("width", "0"));
                                intent.putExtra("repostMediaHeight", media.optString("heigth", "0")); 
                            }
                        }

                        startActivity(intent);
                    }
                });
                
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
                if (holder.btnPrevImage != null) holder.btnPrevImage.setVisibility(View.GONE);
                if (holder.btnNextImage != null) holder.btnNextImage.setVisibility(View.GONE);
                if (holder.videoPost != null) {
                    holder.videoPost.stopPlayback();
                    holder.videoPost.setVisibility(View.GONE);
                }
                if (holder.btnPlayVideo != null) holder.btnPlayVideo.setVisibility(View.GONE);

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
                        intent.putExtra("usernameProfile", postUsername);
                        startActivity(intent);
                    }
                });

                holder.btnComments.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(HomeActivity.this, comments_activity.class);
                        
                        if ("Linka".equalsIgnoreCase(actualFeed.trim())) {
                            intent.putExtra("isExternal", false);
                        } else {
                            intent.putExtra("isExternal", isExternal);
                            if (isExternal) {
                                intent.putExtra("externalUrl", origin_server);
                                intent.putExtra("uniqueId", postUniqueId);
                                intent.putExtra("originalUrl", originalUrl);
                            }
                        }
                        
                        intent.putExtra("isGuest", isGuest);
                        intent.putExtra("post_id", String.valueOf(id));
                        startActivity(intent);
                    }
                });

                holder.txtCommentCount.setText(commentsCount);
                holder.tvUsername.setText(postUsername.startsWith("@") ? postUsername : "@" + postUsername);
                holder.tvDate.setText(relative_datetime.length() > 0 ? relative_datetime : datetime);

                if ("timeline".equals(actualTab)) {
                    String avatarUrl = post.optString("avatar_url", "");
                    if (avatarUrl != null && avatarUrl.trim().length() > 0) {
                        holder.avatarPost.setTag(avatarUrl);
                        imageLoader.LoadImageUrl(avatarUrl, holder.avatarPost);
                    }
                } else {
                    holder.avatarPost.setTag("avatar_" + postUsername);
                    imageLoader.viewProfilePicture(context, postUsername, holder.avatarPost);
                }

                final ArrayList<JSONObject> mediaItems = new ArrayList<JSONObject>();
                JSONArray mediaList = post.optJSONArray("media_list");
                if (mediaList == null) mediaList = post.optJSONArray("media_attachments");

                if (mediaList != null && mediaList.length() > 0) {
                    for (int i = 0; i < mediaList.length(); i++) {
                        JSONObject item = mediaList.getJSONObject(i);
                        String itemUrl = item.optString("media_url", item.optString("url", ""));
                        if (itemUrl.length() > 0 && !itemUrl.equals("null")) {
                            mediaItems.add(item);
                        }
                    }
                } else {
                    JSONObject singleMedia = post.optJSONObject("media");
                    if (singleMedia != null && singleMedia.optString("media_url", "").length() > 0) {
                        mediaItems.add(singleMedia);
                    } else {
                        String stringMedia = post.optString("media", "");
                        if (stringMedia.length() > 0 && !stringMedia.equals("null") && stringMedia.startsWith("http")) {
                            JSONObject fallbackObj = new JSONObject();
                            try {
                                fallbackObj.put("media_url", stringMedia);
                                fallbackObj.put("media_type", post.optString("media_type", "image"));
                                fallbackObj.put("width", post.optString("width", ""));
                                fallbackObj.put("heigth", post.optString("heigth", ""));
                                mediaItems.add(fallbackObj);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                if (mediaItems.size() > 0) {
                    holder.containerMedia.setVisibility(View.VISIBLE);
                    renderMediaItem(holder, mediaItems, 0);

                    if (mediaItems.size() > 1) {
                        holder.txtMediaCount.setVisibility(View.VISIBLE);

                        if (holder.btnPrevImage != null) {
                            holder.btnPrevImage.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Integer currIdx = (Integer) holder.imgPost.getTag(R.id.imgPost);
                                    if (currIdx == null) currIdx = 0;
                                    if (currIdx > 0) {
                                        currIdx--;
                                        renderMediaItem(holder, mediaItems, currIdx);
                                    }
                                }
                            });
                        }

                        if (holder.btnNextImage != null) {
                            holder.btnNextImage.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Integer currIdx = (Integer) holder.imgPost.getTag(R.id.imgPost);
                                    if (currIdx == null) currIdx = 0;
                                    if (currIdx < mediaItems.size() - 1) {
                                        currIdx++;
                                        renderMediaItem(holder, mediaItems, currIdx);
                                    }
                                }
                            });
                        }
                    }
                } else {
                    holder.containerMedia.setVisibility(View.GONE);
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
                Log.e("LINKA_DEBUG", "[POST VIEW ERR] Erro ao renderizar card: " + e.getMessage());
                e.printStackTrace();
            }

            return convertView;
        }

        private void renderMediaItem(final ViewHolder holder, final ArrayList<JSONObject> mediaItems, final int index) {
            JSONObject mediaObj = mediaItems.get(index);
            final String rawUrl = mediaObj.optString("media_url", mediaObj.optString("url", ""));
            String mediaType = mediaObj.optString("media_type", "").toLowerCase();
            String thumbUrl = mediaObj.optString("thumbnail_url", "");
            
            String width = mediaObj.optString("width", "");
            String height = mediaObj.optString("height", mediaObj.optString("heigth", ""));

            holder.imgPost.setTag(R.id.imgPost, index);

            ViewGroup.LayoutParams params = holder.imgPost.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT; 

            try {
                if (width.length() > 0 && height.length() > 0 && !width.equals("null") && !height.equals("null")) {
                    int origW = Integer.parseInt(width);
                    int origH = Integer.parseInt(height);
                    if (origW > 0) {
                        int calculatedHeight = (screenWidth * origH) / origW;
                        params.height = Math.min(calculatedHeight, screenWidth); 
                    } else {
                        params.height = screenWidth; 
                    }
                } else {
                    params.height = screenWidth;
                }
            } catch (Exception e) {
                params.height = screenWidth;
            }
            holder.imgPost.setLayoutParams(params);
            holder.imgPost.setScaleType(ImageView.ScaleType.CENTER_CROP);

            boolean isVideo = "video".equalsIgnoreCase(mediaType) || rawUrl.endsWith(".mp4") || rawUrl.contains("video.twimg.com");

            if (isVideo) {
                if (holder.btnPlayVideo != null) {
                    holder.btnPlayVideo.setVisibility(View.VISIBLE);
                    holder.btnPlayVideo.bringToFront();
                }
                if (holder.videoPost != null) holder.videoPost.setVisibility(View.GONE);
                holder.imgPost.setVisibility(View.VISIBLE);

                String previewUrl = (thumbUrl.length() > 0 && !thumbUrl.endsWith(".mp4")) ? thumbUrl : "";
                if (previewUrl.length() > 0) {
                    holder.imgPost.setTag(previewUrl);
                    imageLoader.LoadImageUrl(previewUrl, holder.imgPost);
                } else if (!rawUrl.endsWith(".mp4")) {
                    holder.imgPost.setTag(rawUrl);
                    imageLoader.LoadImageUrl(rawUrl, holder.imgPost);
                } else {
                    holder.imgPost.setImageResource(android.R.drawable.ic_media_play);
                }

                if (holder.btnPlayVideo != null) {
                    holder.btnPlayVideo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (currentPlayingVideo != null) {
                                try {
                                    currentPlayingVideo.stopPlayback();
                                } catch (Exception e) {}
                                currentPlayingVideo.setVisibility(View.GONE);
                                if (currentPlayButton != null) currentPlayButton.setVisibility(View.VISIBLE);
                                if (currentThumbnail != null) currentThumbnail.setVisibility(View.VISIBLE);
                            }

                            holder.imgPost.setVisibility(View.GONE);
                            holder.btnPlayVideo.setVisibility(View.GONE);
                            holder.videoPost.setVisibility(View.VISIBLE);

                            ViewGroup.LayoutParams vidParams = holder.videoPost.getLayoutParams();
                            vidParams.width = ViewGroup.LayoutParams.FILL_PARENT;
                            vidParams.height = (screenWidth * 9) / 16;
                            holder.videoPost.setLayoutParams(vidParams);

                            MediaController mediaController = new MediaController(HomeActivity.this);
                            mediaController.setAnchorView(holder.videoPost);
                            holder.videoPost.setMediaController(mediaController);

                            holder.videoPost.setVideoPath(rawUrl);
                            holder.videoPost.requestFocus();

                            holder.videoPost.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    mp.setLooping(false);
                                    mp.start();
                                }
                            });

                            holder.videoPost.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                                @Override
                                public boolean onInfo(MediaPlayer mp, int what, int extra) {
                                    if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                                        return true;
                                    } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                                        if (!mp.isPlaying()) {
                                            mp.start();
                                        }
                                        return true;
                                    }
                                    return false;
                                }
                            }); // <-- FALTAVA FECHAR O setOnInfoListener AQUI

                            currentPlayingVideo = holder.videoPost;
                            currentPlayButton = holder.btnPlayVideo;
                            currentThumbnail = holder.imgPost;
                        }
                    }); // <-- FECHA O setOnClickListener
                }
            } else {
                if (holder.btnPlayVideo != null) holder.btnPlayVideo.setVisibility(View.GONE);
                if (holder.videoPost != null) holder.videoPost.setVisibility(View.GONE);
                holder.imgPost.setVisibility(View.VISIBLE);

                holder.imgPost.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(HomeActivity.this, ViewPicture.class);
                        intent.putExtra("type", "Image");
                        intent.putExtra("url", rawUrl);
                        startActivity(intent);
                    }
                });

                holder.imgPost.setTag(rawUrl);
                imageLoader.LoadImageUrl(rawUrl, holder.imgPost);
            }

            updateCarouselControls(index, mediaItems.size(), holder.txtMediaCount, holder.btnPrevImage, holder.btnNextImage);
        }

        private void updateCarouselControls(int index, int total, TextView txtCount, Button btnPrev, Button btnNext) {
            if (total > 1) {
                if (txtCount != null) {
                    txtCount.setText((index + 1) + "/" + total);
                    txtCount.setVisibility(View.VISIBLE);
                }
                if (btnPrev != null) btnPrev.setVisibility(index > 0 ? View.VISIBLE : View.GONE);
                if (btnNext != null) btnNext.setVisibility(index < total - 1 ? View.VISIBLE : View.GONE);
            } else {
                if (txtCount != null) txtCount.setVisibility(View.GONE);
                if (btnPrev != null) btnPrev.setVisibility(View.GONE);
                if (btnNext != null) btnNext.setVisibility(View.GONE);
            }
        }
    }

    public String requestHTTP(String urlParam, String method, JSONObject json_body) {
        return request.requestHTTP(urlParam, method, json_body, HomeActivity.this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentPlayingVideo != null) {
            try {
                currentPlayingVideo.stopPlayback();
            } catch (Exception e) {}
            currentPlayingVideo = null;
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        if (schedulerNotifications != null && !schedulerNotifications.isShutdown()) {
            schedulerNotifications.shutdownNow();
        }
        if (schedulerFeedPolling != null && !schedulerFeedPolling.isShutdown()) {
            schedulerFeedPolling.shutdownNow();
        }
    }
}