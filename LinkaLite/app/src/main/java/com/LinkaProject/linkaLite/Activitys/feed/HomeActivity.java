package com.LinkaProject.linkaLite;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.app.ActivityGroup;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
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
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
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
import android.graphics.Typeface;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.graphics.drawable.BitmapDrawable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HomeActivity extends ActivityGroup {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_]+(@[a-zA-Z0-9_.-]+)?)");

    private static final int MAX_AVATAR_CACHE_ENTRIES = 60;
    private static final Map<String, Bitmap> nativeAvatarCache = Collections.synchronizedMap(
            new LinkedHashMap<String, Bitmap>(MAX_AVATAR_CACHE_ENTRIES, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Bitmap> eldest) {
                    return size() > MAX_AVATAR_CACHE_ENTRIES;
                }
            }
    );

    private static Bitmap getCachedAvatar(String usernameKey) {
        if (usernameKey == null) return null;
        Bitmap bmp = nativeAvatarCache.get(usernameKey);
        if (bmp != null && !bmp.isRecycled()) {
            return bmp;
        }
        Bitmap generated = AvatarGenerator.generateAvatar(usernameKey, 128);
        if (generated != null) {
            nativeAvatarCache.put(usernameKey, generated);
        }
        return generated;
    }

    private static ProportionalVideoView currentPlayingVideo = null;
    private static ImageButton currentPlayButton = null;
    private static View currentThumbnail = null;

    private static void stopActiveVideo() {
        if (currentPlayingVideo != null) {
            try {
                currentPlayingVideo.stopPlayback();
            } catch (Exception e) {}
            currentPlayingVideo.setVisibility(View.GONE);
            currentPlayingVideo = null;
        }
        if (currentPlayButton != null) {
            currentPlayButton.setVisibility(View.VISIBLE);
            currentPlayButton = null;
        }
        if (currentThumbnail != null) {
            currentThumbnail.setVisibility(View.VISIBLE);
            currentThumbnail = null;
        }
    }

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

    private ImageButton btnHamburger, btnTopChat, navHome, navCommunities, btnNotifications, btnProfile, fabNewPost;
    private Button btnClose, btnLogin;
    private HorizontalScrollView scrollViewPages;
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
    private DrawerLayout drawerLayout;
    private ImageView logo;
    private int feedPageTimeline = 1;
    private boolean isLoading = false;
    private String actualFeed = "Linka";
    private int lastValidSpinnerPos = 0;
    private boolean hasMoreTimeline = true;
    private Spinner spinnerFeeds;
    private View topHeaderContainer;
    private String username = "";
    private String tokenSession = "";
    private boolean isHeaderVisible = true;
    private boolean isHeaderAnimating = false;
    private int lastFirstVisibleItem = 0;
    private int lastTop = 0;
    private int lastShownNotifCount = 0;
    private int lastShownDmCount = 0;

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
            tokenSession = fastLogin.optString("token_session", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        List<String> redesList = new ArrayList<String>(Arrays.asList("Linka >", "Timeline", "Friends >", "+ New Feed"));
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT name FROM feeds", null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameColIdx = cursor.getColumnIndexOrThrow("name");
                do {
                    redesList.add(cursor.getString(nameColIdx));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        String[] redes = redesList.toArray(new String[redesList.size()]);

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

                if ("+ New Feed".equalsIgnoreCase(newFeed)) {
                    spinnerFeeds.setSelection(lastValidSpinnerPos);
                    startActivity(new Intent(HomeActivity.this, MainEditTimeline.class));
                    return;
                }

                lastValidSpinnerPos = position;

                if (currentPage != 0) {
                    scrollToPage(0);
                }

                if (!newFeed.equalsIgnoreCase(actualFeed)) {
                    stopActiveVideo();
                    actualFeed = newFeed;
                    postsTimeline.clear();
                    feedPageTimeline = 1;
                    hasMoreTimeline = true;
                    if (adapterTimeline != null) adapterTimeline.notifyDataSetChanged();
                    refreshFeed();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        topHeaderContainer = findViewById(R.id.topHeaderContainer);

        Display display = getWindowManager().getDefaultDisplay();
        screenWidth = display.getWidth();
        logo = (ImageView) findViewById(R.id.logo);
        if (logo != null) {
            if (logo.getDrawable() instanceof BitmapDrawable) {
                BitmapDrawable logoDrawable = (BitmapDrawable) logo.getDrawable();
                logoDrawable.setAntiAlias(true);
                logoDrawable.setFilterBitmap(true);
            }
            logo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentPage != 0) {
                        scrollToPage(0);
                    } else if (listViewTimeline != null) {
                        listViewTimeline.setSelection(0);
                    }
                    refreshFeed();
                    UIAnimationHelper.rotateWithWeight((ImageView) v);
                }
            });
            UIAnimationHelper.rotateWithWeight(logo);
        }

        scrollViewPages = (HorizontalScrollView) findViewById(R.id.scrollViewPages);
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

        LinearLayout layoutGuest = (LinearLayout) findViewById(R.id.layoutGuest);
        Intent intent = getIntent();
        isGuest = intent.getBooleanExtra("isGuest", false);
        if (isGuest && layoutGuest != null) {
            layoutGuest.setVisibility(View.VISIBLE);
        }

        btnTopChat = (ImageButton) findViewById(R.id.btnTopChat);
        if (btnTopChat != null) {
            btnTopChat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentPage == 0) {
                        scrollToPage(1);
                    } else {
                        scrollToPage(0);
                    }
                }
            });
        }

        navHome = (ImageButton) findViewById(R.id.navHome);
        navCommunities = (ImageButton) findViewById(R.id.navCommunities);
        fabNewPost = (ImageButton) findViewById(R.id.fabNewPost);
        btnNotifications = (ImageButton) findViewById(R.id.btnNotifications);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        btnClose = (Button) findViewById(R.id.btnClose);
        btnLogin = (Button) findViewById(R.id.btnLogin);

        if (navHome != null) {
            navHome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentPage != 0) {
                        scrollToPage(0);
                    } else if (listViewTimeline != null) {
                        if (listViewTimeline.getFirstVisiblePosition() > 0) {
                            listViewTimeline.setSelection(0);
                        } else {
                            refreshFeed();
                        }
                    }
                }
            });
        }

        if (navCommunities != null) {
            navCommunities.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopActiveVideo();
                    startActivity(new Intent(HomeActivity.this, CommunityIndex.class));
                }
            });
        }

        if (fabNewPost != null) {
            fabNewPost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent postIntent = new Intent(HomeActivity.this, newPost.class);
                    postIntent.putExtra("isGuest", isGuest);
                    startActivity(postIntent);
                }
            });
        }

        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopActiveVideo();
                    LinearLayout bubble = (LinearLayout) findViewById(R.id.bubbleNotifContainer);
                    if (bubble != null) bubble.setVisibility(View.GONE);
                    startActivity(new Intent(HomeActivity.this, InboxActivity.class));
                }
            });
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopActiveVideo();
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
                int footerCount = listViewTimeline.getFooterViewsCount();
                int dataCount = totalItemCount - footerCount;

                if (!isLoading && hasMoreTimeline && dataCount > 0
                        && (firstVisibleItem + visibleItemCount >= totalItemCount - 1)) {
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

        updateNavSelection();
        startBackgroundSchedulers();
    }

    private static class BubbleBackgroundDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean pointUp;

        public BubbleBackgroundDrawable(boolean pointUp) {
            this.pointUp = pointUp;
            paint.setColor(Color.parseColor("#FF4B4B"));
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float w = b.width();
            float h = b.height();
            float arrowH = 10f;
            float arrowHalfW = 9f;
            float radius = 12f;
            float cx = w / 2f;

            Path path = new Path();
            if (pointUp) {
                RectF body = new RectF(0, arrowH, w, h);
                path.addRoundRect(body, radius, radius, Path.Direction.CW);
                path.moveTo(cx - arrowHalfW, arrowH + 1f);
                path.lineTo(cx, 0f);
                path.lineTo(cx + arrowHalfW, arrowH + 1f);
                path.close();
            } else {
                RectF body = new RectF(0, 0, w, h - arrowH);
                path.addRoundRect(body, radius, radius, Path.Direction.CW);
                path.moveTo(cx - arrowHalfW, h - arrowH - 1f);
                path.lineTo(cx, h);
                path.lineTo(cx + arrowHalfW, h - arrowH - 1f);
                path.close();
            }
            canvas.drawPath(path, paint);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter cf) { paint.setColorFilter(cf); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    private static class WhiteBadgeIconDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean isMessage;

        public WhiteBadgeIconDrawable(boolean isMessage) {
            this.isMessage = isMessage;
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float w = b.width();
            float h = b.height();

            if (isMessage) {
                float left = w * 0.08f;
                float right = w * 0.92f;
                float top = h * 0.20f;
                float bottom = h * 0.80f;

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(2f, w * 0.11f));
                canvas.drawRoundRect(new RectF(left, top, right, bottom), 2f, 2f, paint);

                Path flap = new Path();
                flap.moveTo(left, top + 1f);
                flap.lineTo(w * 0.50f, h * 0.54f);
                flap.lineTo(right, top + 1f);
                canvas.drawPath(flap, paint);
            } else {
                paint.setStyle(Paint.Style.FILL);
                float headRadius = w * 0.21f;
                canvas.drawCircle(w * 0.50f, h * 0.30f, headRadius, paint);

                RectF shoulders = new RectF(w * 0.16f, h * 0.56f, w * 0.84f, h * 0.94f);
                Path body = new Path();
                body.addRoundRect(shoulders, new float[]{w * 0.30f, h * 0.26f, w * 0.30f, h * 0.26f, 3f, 3f, 3f, 3f}, Path.Direction.CW);
                canvas.drawPath(body, paint);
            }
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter cf) { paint.setColorFilter(cf); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    public void showNotificationBubble(int count, final boolean isDirectMessage) {
        final LinearLayout bubble = (LinearLayout) findViewById(R.id.bubbleNotifContainer);
        TextView txtCount = (TextView) findViewById(R.id.txtBubbleCount);
        ImageView imgIcon = (ImageView) findViewById(R.id.imgBubbleIcon);

        if (bubble == null || txtCount == null || count <= 0) return;

        txtCount.setText(String.valueOf(count));

        if (imgIcon != null) {
            imgIcon.setImageDrawable(new WhiteBadgeIconDrawable(isDirectMessage));
        }

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) bubble.getLayoutParams();
        if (isDirectMessage) {
            lp.addRule(RelativeLayout.ABOVE, 0);
            lp.addRule(RelativeLayout.BELOW, R.id.topHeaderContainer);
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            lp.setMargins(0, 2, 10, 0);
            bubble.setBackgroundDrawable(new BubbleBackgroundDrawable(true));
            bubble.setPadding(16, 18, 16, 10);
        } else {
            int btnWidth = screenWidth / 5;
            int rightOffset = Math.max(12, btnWidth + (btnWidth / 2) - 34);
            lp.addRule(RelativeLayout.BELOW, 0);
            lp.addRule(RelativeLayout.ABOVE, R.id.bottomBarContainer);
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            lp.setMargins(0, 0, rightOffset, 4);
            bubble.setBackgroundDrawable(new BubbleBackgroundDrawable(false));
            bubble.setPadding(16, 10, 16, 18);
        }
        bubble.setLayoutParams(lp);
        bubble.setVisibility(View.VISIBLE);
        bubble.bringToFront();

        ScaleAnimation popIn = new ScaleAnimation(
                0.4f, 1.0f, 0.4f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, isDirectMessage ? 0.0f : 1.0f
        );
        popIn.setDuration(220);
        bubble.startAnimation(popIn);

        bubble.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bubble.setVisibility(View.GONE);
                if (isDirectMessage) {
                    scrollToPage(1);
                } else {
                    startActivity(new Intent(HomeActivity.this, InboxActivity.class));
                }
            }
        });

        bubble.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (bubble.getVisibility() == View.VISIBLE) {
                    AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                    fadeOut.setDuration(300);
                    fadeOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override public void onAnimationStart(Animation animation) {}
                        @Override public void onAnimationRepeat(Animation animation) {}
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            bubble.setVisibility(View.GONE);
                        }
                    });
                    bubble.startAnimation(fadeOut);
                }
            }
        }, 5500);
    }

    private void checkUnreadBadges() {
        if (isGuest || username == null || username.length() == 0) return;
        try {
            String baseUrl = (url != null && url.length() > 0) ? url : "http://linkaProject.pythonanywhere.com";
            if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

            JSONObject payload = new JSONObject();
            payload.put("username", username);
            payload.put("token_session", tokenSession);

            String resNotif = requestHTTP(baseUrl + "/notifications", "POST", payload);
            int unreadNotifs = 0;
            if (resNotif != null && resNotif.trim().startsWith("[")) {
                JSONArray arr = new JSONArray(resNotif.trim());
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.optJSONObject(i);
                    if (item != null && !item.optBoolean("read", false) && !item.optBoolean("is_read", false)) {
                        unreadNotifs++;
                    }
                }
            } else if (resNotif != null && resNotif.trim().startsWith("{")) {
                JSONObject obj = new JSONObject(resNotif.trim());
                unreadNotifs = obj.optInt("unread_count", obj.optInt("count", 0));
            }

            String resDm = requestHTTP(baseUrl + "/unread-messages", "POST", payload);
            int unreadDms = 0;
            if (resDm != null && resDm.trim().startsWith("{")) {
                JSONObject dmObj = new JSONObject(resDm.trim());
                unreadDms = dmObj.optInt("unread_count", dmObj.optInt("count", 0));
            } else if (resDm != null && resDm.trim().startsWith("[")) {
                JSONArray dmArr = new JSONArray(resDm.trim());
                unreadDms = dmArr.length();
            }

            final int finalNotifs = unreadNotifs;
            final int finalDms = unreadDms;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (finalDms > 0 && finalDms != lastShownDmCount) {
                        lastShownDmCount = finalDms;
                        showNotificationBubble(finalDms, true);
                    } else if (finalNotifs > 0 && finalNotifs != lastShownNotifCount) {
                        lastShownNotifCount = finalNotifs;
                        showNotificationBubble(finalNotifs, false);
                    }
                    if (finalDms == 0) lastShownDmCount = 0;
                    if (finalNotifs == 0) lastShownNotifCount = 0;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateNavSelection() {
        if (navHome != null) {
            navHome.setBackgroundColor(currentPage == 0 ? Color.parseColor("#2A2A2A") : Color.TRANSPARENT);
        }
        if (btnTopChat != null) {
            btnTopChat.setBackgroundColor(currentPage == 1 ? Color.parseColor("#2A2A2A") : Color.TRANSPARENT);
        }
    }

    private void startBackgroundSchedulers() {
        if (schedulerNotifications == null || schedulerNotifications.isShutdown()) {
            schedulerNotifications = Executors.newSingleThreadScheduledExecutor();
            schedulerNotifications.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    notificationManager.createNotification(HomeActivity.this);
                    checkUnreadBadges();
                }
            }, 2, 60, TimeUnit.SECONDS);
        }

        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
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
        }

        if (schedulerFeedPolling == null || schedulerFeedPolling.isShutdown()) {
            schedulerFeedPolling = Executors.newSingleThreadScheduledExecutor();
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
    }

    private void stopBackgroundSchedulers() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        if (schedulerNotifications != null && !schedulerNotifications.isShutdown()) {
            schedulerNotifications.shutdownNow();
            schedulerNotifications = null;
        }
        if (schedulerFeedPolling != null && !schedulerFeedPolling.isShutdown()) {
            schedulerFeedPolling.shutdownNow();
            schedulerFeedPolling = null;
        }
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

        View btnDrawerFavorites = findViewById(R.id.btnDrawerFavorites);
        View btnDrawerEditTimeline = findViewById(R.id.btnDrawerEditTimeline);
        View btnDrawerOptions = findViewById(R.id.btnDrawerOptions);
        View btnDrawerLogout = findViewById(R.id.btnDrawerLogout);
        String currentUsername = "user";

        if (btnDrawerOptions != null) {
            btnDrawerOptions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout != null) drawerLayout.closeDrawers();
                    startActivity(new Intent(HomeActivity.this, optionActivity.class));
                }
            });
        }

        if (btnDrawerEditTimeline != null) {
            btnDrawerEditTimeline.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout != null) drawerLayout.closeDrawers();
                    startActivity(new Intent(HomeActivity.this, MainEditTimeline.class));
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
                    stopActiveVideo();
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
        if (page != 0) {
            stopActiveVideo();
        }
        if (page == 1) {
            LinearLayout bubble = (LinearLayout) findViewById(R.id.bubbleNotifContainer);
            if (bubble != null) bubble.setVisibility(View.GONE);
        }
        currentPage = page;
        showHeaderBar();
        updateNavSelection();

        if (scrollViewPages != null) {
            scrollViewPages.post(new Runnable() {
                @Override
                public void run() {
                    scrollViewPages.smoothScrollTo(page * screenWidth, 0);
                }
            });
        }
        loadFeedForTab(pageTitles[page], false, 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNavSelection();
        startBackgroundSchedulers();
        loadFeedForTab(pageTitles[currentPage], false, 1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopActiveVideo();
        stopBackgroundSchedulers();
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
        if ("Timeline".equals(tabName) && listViewTimeline != null && listViewTimeline.getFirstVisiblePosition() == 0) {
            loadFeedForTab("Timeline", true, 1);
        }
    }

    public void refreshFeed() {
        stopActiveVideo();
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
            String currentFeedClean = actualFeed.trim();

            if ("Linka".equalsIgnoreCase(currentFeedClean)) {
                String feedUrl = baseUrl + "/feed?page=" + targetPage + "&limit=20";
                if (username != null && username.length() > 0) {
                    feedUrl += "&username=" + username;
                }
                new FetchFeedTask("timeline", feedUrl, "GET", null, targetPage).execute();
            } else if ("Timeline".equalsIgnoreCase(currentFeedClean)) {
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
            } else if ("Friends".equalsIgnoreCase(currentFeedClean)) {
                try {
                    JSONObject jsonFriends = new JSONObject();
                    jsonFriends.put("username", username);
                    new FetchFeedTask("timeline", baseUrl + "/friends-feed?page=" + targetPage + "&limit=20", "POST", jsonFriends, targetPage).execute();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
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

                    if (jsonArray.length() == 0) {
                        hasMoreTimeline = false;
                        return;
                    }

                    if (pageRequested == 1) {
                        targetList.clear();
                        hasMoreTimeline = true;
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

    private class VotePollTask extends AsyncTask<Void, Void, String> {
        private JSONObject postObj;
        private JSONObject pollObj;
        private int pollId;
        private int optionId;

        public VotePollTask(JSONObject postObj, JSONObject pollObj, int pollId, int optionId) {
            this.postObj = postObj;
            this.pollObj = pollObj;
            this.pollId = pollId;
            this.optionId = optionId;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                String baseUrl = (url != null && url.length() > 0) ? url : "http://linkaProject.pythonanywhere.com";
                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }

                JSONObject payload = new JSONObject();
                payload.put("username", username);
                payload.put("token_session", tokenSession);
                payload.put("poll_id", pollId);
                payload.put("option_id", optionId);

                return requestHTTP(baseUrl + "/vote-poll", "POST", payload);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && result.trim().length() > 0) {
                try {
                    JSONObject resp = new JSONObject(result);
                    if (resp.has("poll")) {
                        postObj.put("poll", resp.getJSONObject("poll"));
                        if (adapterTimeline != null) adapterTimeline.notifyDataSetChanged();
                        return;
                    }
                    String status = resp.optString("status", "");
                    if (status.contains("error") || status.contains("forbidden")) {
                        Toast.makeText(HomeActivity.this, resp.optString("message", status), Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (Exception ignored) {}

                try {
                    int previousOptionId = pollObj.optInt("user_voted_option_id", -1);
                    if (previousOptionId != optionId) {
                        JSONArray options = pollObj.optJSONArray("options");
                        if (options != null) {
                            for (int i = 0; i < options.length(); i++) {
                                JSONObject opt = options.getJSONObject(i);
                                int id = opt.optInt("id", -1);
                                int votes = opt.optInt("votes", 0);
                                if (id == previousOptionId && votes > 0) {
                                    opt.put("votes", votes - 1);
                                } else if (id == optionId) {
                                    opt.put("votes", votes + 1);
                                }
                            }
                        }
                        pollObj.put("user_voted_option_id", optionId);
                        if (adapterTimeline != null) adapterTimeline.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(HomeActivity.this, "Erro ao votar na enquete", Toast.LENGTH_SHORT).show();
            }
        }
    }

    static class ViewHolder {
        ImageView avatarPost;
        FrameLayout containerMedia;
        GifImageView imgPost;
        ProportionalVideoView videoPost;
        ImageButton btnPlayVideo;
        TextView txtMediaCount;
        Button btnPrevImage;
        Button btnNextImage;
        TextView tvUsername;
        TextView tvText;
        LinearLayout containerPollCard;
        LinearLayout containerEmbedCard;
        LinearLayout containerRepostCard;
        TextView tvDate;
        ImageButton btnComments;
        TextView txtCommentCount;
        ImageButton btnBookmarks;
        LinearLayout containerBookmarkOptions;
        Button btnPublicBookmarks;
        Button btnPrivateBookmarks;
        ImageButton btnCloseBookmarks;
        ImageButton btnReply;
        Button btnPostOptions;
        Spinner optionsSpinner;
        Button btnFollow;
    }

    private class PostAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<JSONObject> list;
        private String tabType;
        private ImageLoader imageLoader;
        private Bookmark bookmarkManager;

        public PostAdapter(Context context, ArrayList<JSONObject> list, String tabType) {
            this.context = context;
            this.list = list;
            this.tabType = tabType;
            this.imageLoader = new ImageLoader();
            this.bookmarkManager = new Bookmark(context);
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

        private void openPostComments(JSONObject post) {
            if (post == null) return;
            String postUsername = post.optString("username", post.optString("user", "entity404"));
            String textPost = post.optString("text_post", post.optString("text", ""));
            String origin_server = post.optString("origin_server", "");
            String datetime = post.optString("datetime", post.optString("date", ""));
            String commentsCount = post.optString("comments_count", post.optString("comment_count", "0"));
            String postUniqueId = post.optString("unique_id", "");
            int id = post.optInt("id", -1);
            String originalUrl = post.optString("original_url", "");
            boolean isExternal = origin_server.length() > 0 && !origin_server.equalsIgnoreCase("linka");

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
            intent.putExtra("post_author", postUsername);
            intent.putExtra("post_date", datetime);
            intent.putExtra("post_text", textPost);
            intent.putExtra("post_community", "");
            intent.putExtra("avatar_url", post.optString("avatar_url", ""));
            JSONArray mediaArr = post.optJSONArray("media_list");
            intent.putExtra("media_list", mediaArr != null ? mediaArr.toString() : "[]");
            intent.putExtra("comment_count", commentsCount);
            intent.putExtra("isGuest", isGuest);
            intent.putExtra("post_id", String.valueOf(id));
            startActivity(intent);
        }
        
        private LinearLayout ensurePollContainer(View convertView, TextView tvText) {
            View existing = convertView.findViewWithTag("dynamic_poll_container");
            if (existing instanceof LinearLayout) {
                return (LinearLayout) existing;
            }
            if (tvText != null && tvText.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) tvText.getParent();
                int idx = parent.indexOfChild(tvText);
                LinearLayout pollLayout = new LinearLayout(context);
                pollLayout.setTag("dynamic_poll_container");
                pollLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.FILL_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                lp.setMargins(12, 8, 12, 8);
                pollLayout.setLayoutParams(lp);
                pollLayout.setVisibility(View.GONE);
                parent.addView(pollLayout, idx + 1);
                return pollLayout;
            }
            return null;
        }

        private LinearLayout ensureEmbedContainer(View convertView, TextView tvText) {
            View existing = convertView.findViewWithTag("dynamic_embed_container");
            if (existing instanceof LinearLayout) {
                return (LinearLayout) existing;
            }
            if (tvText != null && tvText.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) tvText.getParent();
                int idx = parent.indexOfChild(tvText);
                LinearLayout embedLayout = new LinearLayout(context);
                embedLayout.setTag("dynamic_embed_container");
                embedLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.FILL_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                lp.setMargins(16, 6, 16, 10);
                embedLayout.setLayoutParams(lp);
                embedLayout.setVisibility(View.GONE);
                parent.addView(embedLayout, idx + 1);
                return embedLayout;
            }
            return null;
        }

        private LinearLayout ensureRepostContainer(View convertView, TextView tvText) {
            View existing = convertView.findViewWithTag("dynamic_repost_container");
            if (existing instanceof LinearLayout) {
                return (LinearLayout) existing;
            }
            if (tvText != null && tvText.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) tvText.getParent();
                int idx = parent.indexOfChild(tvText);
                LinearLayout repostLayout = new LinearLayout(context);
                repostLayout.setTag("dynamic_repost_container");
                repostLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.FILL_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                lp.setMargins(16, 8, 16, 10);
                repostLayout.setLayoutParams(lp);
                repostLayout.setVisibility(View.GONE);
                parent.addView(repostLayout, idx + 1);
                return repostLayout;
            }
            return null;
        }

        private void renderRepostCard(LinearLayout container, final JSONObject post) {
            if (container == null) return;
            container.removeAllViews();

            JSONObject repostObj = post.optJSONObject("repost");
            int repostId = (repostObj != null) ? repostObj.optInt("original_id", -1) : post.optInt("repost_id", -1);
            final String repostUser = (repostObj != null) ? repostObj.optString("username", "") : post.optString("repost_username", "");
            String repostText = (repostObj != null) ? repostObj.optString("text_post", "") : post.optString("repost_text", "");
            String repostDate = (repostObj != null) ? repostObj.optString("relative_datetime", repostObj.optString("datetime", "")) : post.optString("repost_datetime", "");
            String repostAvatar = (repostObj != null) ? repostObj.optString("avatar_url", "") : post.optString("repost_avatar_url", "");
            final String repostMedia = (repostObj != null) ? repostObj.optString("media_url", "") : post.optString("repost_media_url", "");
            String repostWStr = (repostObj != null) ? repostObj.optString("width", "") : post.optString("repost_media_width", "");
            String repostHStr = (repostObj != null) ? repostObj.optString("heigth", repostObj.optString("height", "")) : post.optString("repost_media_height", "");

            if ((repostId <= 0 && repostUser.length() == 0 && repostText.length() == 0 && repostMedia.length() == 0)) {
                container.setVisibility(View.GONE);
                return;
            }

            container.setVisibility(View.VISIBLE);
            container.setBackgroundResource(R.drawable.bg_card_classic);
            container.setPadding(14, 12, 14, 12);

            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openPostComments(post);
                }
            });

            LinearLayout headerRow = new LinearLayout(context);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            ImageView avatarView = new ImageView(context);
            LinearLayout.LayoutParams avParams = new LinearLayout.LayoutParams(32, 32);
            avParams.setMargins(0, 0, 10, 0);
            avatarView.setLayoutParams(avParams);
            avatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap defaultAv = getCachedAvatar(repostUser.length() > 0 ? repostUser : "user");
            avatarView.setImageBitmap(defaultAv);
            if (repostAvatar.length() > 0 && !"null".equalsIgnoreCase(repostAvatar)) {
                avatarView.setTag(repostAvatar);
                imageLoader.LoadImageUrl(repostAvatar, avatarView);
            }
            headerRow.addView(avatarView);

            TextView tvUser = new TextView(context);
            LinearLayout.LayoutParams userParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            tvUser.setLayoutParams(userParams);
            tvUser.setTextSize(13);
            tvUser.setTypeface(null, Typeface.BOLD);
            tvUser.setTextColor(Color.parseColor("#222222"));
            tvUser.setText(repostUser.startsWith("@") ? repostUser : "@" + repostUser);
            headerRow.addView(tvUser);

            if (repostDate.length() > 0) {
                TextView tvDt = new TextView(context);
                tvDt.setTextSize(11);
                tvDt.setTextColor(Color.parseColor("#777777"));
                tvDt.setText(repostDate);
                headerRow.addView(tvDt);
            }

            container.addView(headerRow);

            String cleanRepostText = repostText;
            int tagIdx = cleanRepostText.indexOf("[IMAGE]");
            if (tagIdx != -1) {
                cleanRepostText = cleanRepostText.substring(0, tagIdx).trim();
            }
            if (cleanRepostText.length() > 0) {
                TextView tvBody = new TextView(context);
                tvBody.setTextSize(13);
                tvBody.setTextColor(Color.parseColor("#333333"));
                tvBody.setPadding(0, 8, 0, 4);
                highlightMentions(context, tvBody, cleanRepostText);
                container.addView(tvBody);
            }

            if (repostMedia.length() > 0 && !"null".equalsIgnoreCase(repostMedia)) {
                int parsedW = 0;
                int parsedH = 0;
                try {
                    if (repostWStr.length() > 0 && repostHStr.length() > 0 && !"null".equals(repostWStr) && !"null".equals(repostHStr)) {
                        parsedW = Integer.parseInt(repostWStr);
                        parsedH = Integer.parseInt(repostHStr);
                    }
                } catch (Exception ignored) {}

                int innerWidth = Math.max(200, screenWidth - 60);
                int imgHeight = (parsedW > 0 && parsedH > 0)
                        ? Math.min((innerWidth * parsedH) / parsedW, (int) (innerWidth * 1.15f))
                        : (innerWidth * 9) / 16;

                ImageView mediaView = new ImageView(context);
                LinearLayout.LayoutParams mParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.FILL_PARENT,
                        imgHeight
                );
                mParams.setMargins(0, 8, 0, 2);
                mediaView.setLayoutParams(mParams);
                mediaView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                mediaView.setBackgroundColor(Color.parseColor("#141414"));
                container.addView(mediaView);
                imageLoader.LoadImageUrl(repostMedia, mediaView);

                mediaView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(HomeActivity.this, ViewPicture.class);
                        intent.putExtra("type", "Image");
                        intent.putExtra("url", repostMedia);
                        startActivity(intent);
                    }
                });
            }

            if (repostUser.length() > 0) {
                tvUser.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(HomeActivity.this, ViewProfile.class);
                        intent.putExtra("usernameProfile", repostUser);
                        startActivity(intent);
                    }
                });
            }
        }

        private void renderEmbedCard(LinearLayout container, JSONObject embedObj) {
            if (container == null) return;
            container.removeAllViews();

            if (embedObj == null || embedObj.length() == 0) {
                container.setVisibility(View.GONE);
                return;
            }

            final String targetUrl = embedObj.optString("url", "");
            if (targetUrl.length() == 0) {
                container.setVisibility(View.GONE);
                return;
            }

            String platform = embedObj.optString("platform", "Link");
            String title = embedObj.optString("title", "");
            String description = embedObj.optString("description", "");
            String author = embedObj.optString("author", "");
            String thumbUrl = embedObj.optString("thumbnail", "");

            container.setVisibility(View.VISIBLE);
            container.setBackgroundColor(Color.parseColor("#F7F5F0"));
            container.setPadding(10, 10, 10, 10);

            if (thumbUrl.length() > 0 && !"null".equalsIgnoreCase(thumbUrl)) {
                ImageView thumbView = new ImageView(context);
                LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.FILL_PARENT,
                        (screenWidth * 9) / 20
                );
                imgParams.setMargins(0, 0, 0, 8);
                thumbView.setLayoutParams(imgParams);
                thumbView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                thumbView.setBackgroundColor(Color.parseColor("#1C1917"));
                container.addView(thumbView);
                imageLoader.LoadImageUrl(thumbUrl, thumbView);
            }

            TextView tvMeta = new TextView(context);
            tvMeta.setTextSize(11);
            tvMeta.setTypeface(null, Typeface.BOLD);
            tvMeta.setTextColor(Color.parseColor("#2B5B84"));
            String metaText = platform.toUpperCase();
            if (author.length() > 0) {
                metaText += " • " + author;
            }
            tvMeta.setText(metaText);
            container.addView(tvMeta);

            if (title.length() > 0) {
                TextView tvTitle = new TextView(context);
                tvTitle.setTextSize(13);
                tvTitle.setTypeface(null, Typeface.BOLD);
                tvTitle.setTextColor(Color.parseColor("#1C1917"));
                tvTitle.setMaxLines(2);
                tvTitle.setEllipsize(TextUtils.TruncateAt.END);
                tvTitle.setPadding(0, 2, 0, 2);
                tvTitle.setText(title);
                container.addView(tvTitle);
            }

            if (description.length() > 0) {
                TextView tvDesc = new TextView(context);
                tvDesc.setTextSize(12);
                tvDesc.setTextColor(Color.parseColor("#57534E"));
                tvDesc.setMaxLines(3);
                tvDesc.setEllipsize(TextUtils.TruncateAt.END);
                tvDesc.setText(description);
                container.addView(tvDesc);
            }

            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
                        context.startActivity(browserIntent);
                    } catch (Exception e) {
                        Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        private void renderPoll(LinearLayout container, final JSONObject post, final JSONObject pollObj) {
            if (container == null) return;
            container.removeAllViews();

            if (pollObj == null || pollObj.length() == 0) {
                container.setVisibility(View.GONE);
                return;
            }

            JSONArray options = pollObj.optJSONArray("options");
            if (options == null || options.length() == 0) {
                container.setVisibility(View.GONE);
                return;
            }

            container.setVisibility(View.VISIBLE);
            container.setBackgroundColor(Color.parseColor("#14888888"));
            container.setPadding(12, 10, 12, 10);

            final int pollId = pollObj.optInt("poll_id", -1);
            final int votedOptionId = pollObj.optInt("user_voted_option_id", -1);
            boolean hasVoted = votedOptionId > 0;

            int totalVotes = 0;
            for (int i = 0; i < options.length(); i++) {
                JSONObject opt = options.optJSONObject(i);
                if (opt != null) {
                    totalVotes += opt.optInt("votes", 0);
                }
            }

            for (int i = 0; i < options.length(); i++) {
                JSONObject opt = options.optJSONObject(i);
                if (opt == null) continue;

                final int optionId = opt.optInt("id", -1);
                String optionText = opt.optString("text", "");
                int votes = opt.optInt("votes", 0);
                int percent = (totalVotes > 0) ? Math.round((votes * 100.0f) / totalVotes) : 0;
                boolean isSelectedOption = (optionId == votedOptionId);

                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(12, 10, 12, 10);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.FILL_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                rowParams.setMargins(0, 0, 0, 6);
                row.setLayoutParams(rowParams);

                if (isSelectedOption) {
                    row.setBackgroundColor(Color.parseColor("#330066CC"));
                } else {
                    row.setBackgroundColor(Color.parseColor("#22888888"));
                }

                View radioDot = new View(context);
                LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(16, 16);
                dotParams.setMargins(0, 4, 10, 0);
                radioDot.setLayoutParams(dotParams);
                radioDot.setBackgroundResource(isSelectedOption ? R.drawable.circle_selected : R.drawable.circle_normal);
                row.addView(radioDot);

                TextView txtOpt = new TextView(context);
                LinearLayout.LayoutParams txtParams = new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1.0f
                );
                txtOpt.setLayoutParams(txtParams);
                txtOpt.setTextSize(13);
                txtOpt.setTextColor(Color.parseColor("#222222"));
                if (isSelectedOption) {
                    txtOpt.setTypeface(null, Typeface.BOLD);
                    txtOpt.setText("✓ " + optionText);
                } else {
                    txtOpt.setText(optionText);
                }
                row.addView(txtOpt);

                if (hasVoted || totalVotes > 0) {
                    TextView txtPct = new TextView(context);
                    txtPct.setTextSize(12);
                    txtPct.setTextColor(Color.parseColor("#555555"));
                    txtPct.setText(percent + "% (" + votes + ")");
                    row.addView(txtPct);
                }

                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isGuest) {
                            Toast.makeText(HomeActivity.this, "Login required to vote", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (pollId != -1 && optionId != -1) {
                            new VotePollTask(post, pollObj, pollId, optionId).execute();
                        }
                    }
                });

                container.addView(row);
            }

            TextView txtFooter = new TextView(context);
            txtFooter.setTextSize(11);
            txtFooter.setTextColor(Color.parseColor("#666666"));
            txtFooter.setPadding(2, 2, 2, 0);
            txtFooter.setText(totalVotes == 1 ? "1 vote" : totalVotes + " votes");
            container.addView(txtFooter);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.item_post, parent, false);
                holder = new ViewHolder();
                holder.avatarPost = (ImageView) convertView.findViewById(R.id.postAvatar);
                holder.containerMedia = (FrameLayout) convertView.findViewById(R.id.containerMedia);
                holder.imgPost = (GifImageView) convertView.findViewById(R.id.imgPost);
                holder.videoPost = (ProportionalVideoView) convertView.findViewById(R.id.videoPost);
                holder.btnPlayVideo = (ImageButton) convertView.findViewById(R.id.btnPlayVideo);
                holder.txtMediaCount = (TextView) convertView.findViewById(R.id.txtMediaCount);
                holder.btnPrevImage = (Button) convertView.findViewById(R.id.btnPrevImage);
                holder.btnNextImage = (Button) convertView.findViewById(R.id.btnNextImage);
                holder.tvUsername = (TextView) convertView.findViewById(R.id.postUsername);
                holder.tvText = (TextView) convertView.findViewById(R.id.postText);
                holder.containerPollCard = ensurePollContainer(convertView, holder.tvText);
                holder.containerEmbedCard = ensureEmbedContainer(convertView, holder.tvText);
                holder.containerRepostCard = ensureRepostContainer(convertView, holder.tvText);
                holder.tvDate = (TextView) convertView.findViewById(R.id.postDate);
                holder.btnComments = (ImageButton) convertView.findViewById(R.id.btnComments);
                holder.txtCommentCount = (TextView) convertView.findViewById(R.id.txtCommentCount);
                holder.btnBookmarks = (ImageButton) convertView.findViewById(R.id.btnBookmarks);
                holder.containerBookmarkOptions = (LinearLayout) convertView.findViewById(R.id.containerBookmarkOptions);
                holder.btnPublicBookmarks = (Button) convertView.findViewById(R.id.btnPublicBookmarks);
                holder.btnPrivateBookmarks = (Button) convertView.findViewById(R.id.btnPrivateBookmarks);
                holder.btnCloseBookmarks = (ImageButton) convertView.findViewById(R.id.btnCloseBookmarks);
                holder.btnReply = (ImageButton) convertView.findViewById(R.id.btnReply);
                holder.btnPostOptions = (Button) convertView.findViewById(R.id.btnPostOptions);
                holder.optionsSpinner = (Spinner) convertView.findViewById(R.id.optionsSpinner);
                holder.btnFollow = (Button) convertView.findViewById(R.id.btnFollow);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.btnPostOptions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final JSONObject post = list.get(position);
                    final String postUsername = post.optString("username", post.optString("user", "entity404"));
                    final String[] optionsPost;
                    if (postUsername.equals(username)) {
                        optionsPost = new String[]{"Edit Post", "Delete Post", "View Profile", "Share"};
                    } else {
                        optionsPost = new String[]{"View Profile", "Share"};
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                    builder.setItems(optionsPost, new DialogInterface.OnClickListener() {
                        final String textPost = post.optString("text_post", post.optString("text", ""));
                        final int id = post.optInt("id", -1);
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String chosen = optionsPost[which];
                            if ("Edit Post".equals(chosen)) {
                                Intent intent = new Intent(HomeActivity.this, newPost.class);
                                intent.putExtra("isEdited", true);
                                intent.putExtra("postId", id);
                                intent.putExtra("textPost", textPost);
                                startActivity(intent);
                            } else if ("Delete Post".equals(chosen)) {
                                try {
                                    JSONObject jsonDelete = new JSONObject();
                                    jsonDelete.put("post_id", id);
                                    jsonDelete.put("username", username);
                                    request.requestHTTP(url + "/delete-post", "post", jsonDelete, HomeActivity.this);

                                    if (position >= 0 && position < list.size()) {
                                        list.remove(position);
                                        notifyDataSetChanged();
                                        Toast.makeText(HomeActivity.this, "Post removed", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else if ("View Profile".equals(chosen)) {
                                Intent intent = new Intent(HomeActivity.this, ViewProfile.class);
                                intent.putExtra("usernameProfile", postUsername);
                                startActivity(intent);
                            } else if ("Share".equals(chosen)) {
                                String baseShareUrl = (url != null && url.length() > 0) ? url.trim() : "http://linkaProject.pythonanywhere.com";
                                if (baseShareUrl.endsWith("/")) {
                                    baseShareUrl = baseShareUrl.substring(0, baseShareUrl.length() - 1);
                                }
                                if (baseShareUrl.endsWith("/api")) {
                                    baseShareUrl = baseShareUrl.substring(0, baseShareUrl.length() - 4);
                                }

                                if (id > 0) {
                                    String shareUrl = baseShareUrl + "/post/" + id;
                                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                                    sendIntent.putExtra(Intent.EXTRA_TEXT, shareUrl);
                                    sendIntent.setType("text/plain");
                                    startActivity(Intent.createChooser(sendIntent, "Share post"));
                                } else {
                                    Toast.makeText(HomeActivity.this, "Cannot share this post", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
                    builder.show();
                }
            });

            try {
                final JSONObject post = list.get(position);
                final String postUsername = post.optString("username", post.optString("user", "entity404"));
                final String textPost = post.optString("text_post", post.optString("text", ""));
                final String origin_server = post.optString("origin_server", "");
                final String datetime = post.optString("datetime", post.optString("date", ""));
                final String commentsCount = post.optString("comments_count", post.optString("comment_count", "0"));
                final String postUniqueId = post.optString("unique_id", "");
                final String relative_datetime = post.optString("relative_datetime", "");
                final int id = post.optInt("id", -1);
                final String originalUrl = post.optString("original_url", "");
                final boolean isFollowing = post.optBoolean("is_following", false);

                final boolean isExternal = origin_server.length() > 0 && !origin_server.equalsIgnoreCase("linka");

                if (holder.btnFollow != null) {
                    if (isFollowing || isGuest || postUsername.equals(username)) {
                        holder.btnFollow.setVisibility(View.GONE);
                    } else {
                        holder.btnFollow.setVisibility(View.VISIBLE);
                        holder.btnFollow.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                new Follow().FollowUsername(HomeActivity.this, postUsername);
                                try {
                                    post.put("is_following", true);
                                } catch (JSONException ignored) {}
                                holder.btnFollow.setVisibility(View.GONE);
                            }
                        });
                    }
                }

                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openPostComments(post);
                    }
                });

                holder.tvText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openPostComments(post);
                    }
                });

                holder.btnReply.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Intent intent = new Intent(HomeActivity.this, newPost.class);
                        intent.putExtra("repostPostId", id);
                        intent.putExtra("repostUser", postUsername);
                        intent.putExtra("repostText", textPost);
                        intent.putExtra("repostDatetime", datetime);
                        intent.putExtra("repostAvatarUrl", post.optString("avatar_url", ""));

                        JSONArray jsonArray = post.optJSONArray("media_list");
                        if (jsonArray != null && jsonArray.length() > 0) {
                            JSONObject media = jsonArray.optJSONObject(0);
                            if (media != null) {
                                intent.putExtra("repostMediaUrl", media.optString("media_url", ""));
                                intent.putExtra("repostMediaWidth", media.optString("width", "0"));
                                intent.putExtra("repostMediaHeight", media.optString("height", media.optString("heigth", "0")));
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
                    if (currentPlayingVideo == holder.videoPost) {
                        currentPlayingVideo = null;
                        currentPlayButton = null;
                        currentThumbnail = null;
                    }
                    try {
                        holder.videoPost.stopPlayback();
                    } catch (Exception e) {}
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
                        openPostComments(post);
                    }
                });

                holder.txtCommentCount.setText(commentsCount);
                holder.tvUsername.setText(postUsername.startsWith("@") ? postUsername : "@" + postUsername);
                holder.tvDate.setText(relative_datetime.length() > 0 ? relative_datetime : datetime);
                Bitmap defaultAvatar = getCachedAvatar(postUsername);
                holder.avatarPost.setImageBitmap(defaultAvatar);

                if ("timeline".equals(actualTab)) {
                    String avatarUrl = post.optString("avatar_url", "").trim();
                    if (!avatarUrl.isEmpty() && !"null".equalsIgnoreCase(avatarUrl)) {
                        holder.avatarPost.setTag(avatarUrl);
                        imageLoader.LoadImageUrl(avatarUrl, holder.avatarPost);
                    } else {
                        holder.avatarPost.setTag(null);
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
                                        stopActiveVideo();
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
                                        stopActiveVideo();
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

                JSONObject pollObj = post.optJSONObject("poll");
                renderPoll(holder.containerPollCard, post, pollObj);

                JSONObject embedObj = post.optJSONObject("embed");
                renderEmbedCard(holder.containerEmbedCard, embedObj);

                renderRepostCard(holder.containerRepostCard, post);

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

            int parsedW = 0;
            int parsedH = 0;
            try {
                if (width.length() > 0 && height.length() > 0 && !width.equals("null") && !height.equals("null")) {
                    parsedW = Integer.parseInt(width);
                    parsedH = Integer.parseInt(height);
                }
            } catch (Exception ignored) {}

            ViewGroup.LayoutParams params = holder.imgPost.getLayoutParams();
            params.width = ViewGroup.LayoutParams.FILL_PARENT;

            if (parsedW > 0 && parsedH > 0) {
                int calculatedHeight = (screenWidth * parsedH) / parsedW;
                params.height = Math.min(calculatedHeight, (int) (screenWidth * 1.35f));
            } else {
                params.height = (screenWidth * 9) / 16;
            }
            holder.imgPost.setLayoutParams(params);
            holder.imgPost.setScaleType(ImageView.ScaleType.FIT_CENTER);

            boolean isVideo = "video".equalsIgnoreCase(mediaType) || rawUrl.endsWith(".mp4") || rawUrl.contains("video.twimg.com");

            final int initialVideoW = parsedW;
            final int initialVideoH = parsedH;

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
                            stopActiveVideo();

                            holder.imgPost.setVisibility(View.GONE);
                            holder.btnPlayVideo.setVisibility(View.GONE);
                            holder.videoPost.setVisibility(View.VISIBLE);

                            if (initialVideoW > 0 && initialVideoH > 0) {
                                holder.videoPost.setVideoSize(initialVideoW, initialVideoH);
                            } else {
                                holder.videoPost.setVideoSize(16, 9);
                            }

                            MediaController mediaController = new MediaController(HomeActivity.this);
                            mediaController.setAnchorView(holder.videoPost);
                            holder.videoPost.setMediaController(mediaController);

                            holder.videoPost.setVideoPath(rawUrl);
                            holder.videoPost.requestFocus();

                            holder.videoPost.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    int realW = mp.getVideoWidth();
                                    int realH = mp.getVideoHeight();
                                    if (realW > 0 && realH > 0) {
                                        holder.videoPost.setVideoSize(realW, realH);
                                    }
                                    mp.setLooping(false);
                                    mp.start();
                                }
                            });

                            holder.videoPost.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    stopActiveVideo();
                                }
                            });

                            holder.videoPost.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                                @Override
                                public boolean onError(MediaPlayer mp, int what, int extra) {
                                    stopActiveVideo();
                                    Toast.makeText(HomeActivity.this, "Erro ao reproduzir vídeo", Toast.LENGTH_SHORT).show();
                                    return true;
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
                            });

                            currentPlayingVideo = holder.videoPost;
                            currentPlayButton = holder.btnPlayVideo;
                            currentThumbnail = holder.imgPost;
                        }
                    });
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
        stopActiveVideo();
        stopBackgroundSchedulers();
    }
}