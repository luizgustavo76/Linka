package com.LinkaProject.linkaLite;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommunityFeedActivity extends BaseActivity {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_]+(@[a-zA-Z0-9_.-]+)?)");

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

    private ListView listViewCommunityFeed;
    private CommunityPostAdapter adapterPosts;
    private final ArrayList<JSONObject> communityPosts = new ArrayList<JSONObject>();

    private String communityId = "";
    private String communityName = "Community";
    private String serverUrl = "https://patient-hall-2915.luizsgustavo76.workers.dev";
    private ImageLoader imageLoader;
    private String iconUrl = "";
    private String bannerUrl = "";
    private String description = "";

    private ImageView imgBanner;
    private ImageView imgCommunityAvatar;
    private TextView txtDescription;
    private TextView txtHeaderCommunityName;
    private TextView txtCommunityTitle;
    private ImageButton newPost, btnBack, btnRefreshCommunity, btnProfile;
    private View topHeaderContainer;

    private String username = "";
    private String tokenSession = "";
    private boolean isGuest = false;
    private int screenWidth;
    private int feedPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;

    private boolean isHeaderVisible = true;
    private boolean isHeaderAnimating = false;
    private int lastFirstVisibleItem = 0;
    private int lastTop = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_feed);

        Display display = getWindowManager().getDefaultDisplay();
        screenWidth = display.getWidth();
        imageLoader = new ImageLoader();

        topHeaderContainer = findViewById(R.id.topHeaderContainer);
        newPost = (ImageButton) findViewById(R.id.newPost);
        btnBack = (ImageButton) findViewById(R.id.btnBack);
        btnRefreshCommunity = (ImageButton) findViewById(R.id.btnRefreshCommunity);
        btnProfile = (ImageButton) findViewById(R.id.btnProfile);
        txtCommunityTitle = (TextView) findViewById(R.id.txtCommunityTitle);
        listViewCommunityFeed = (ListView) findViewById(R.id.listViewCommunityFeed);

        Intent intent = getIntent();
        if (intent != null) {
            communityId = intent.hasExtra("community_id") ? intent.getStringExtra("community_id") : "";
            communityName = intent.hasExtra("community_name") ? intent.getStringExtra("community_name") :
                            (intent.hasExtra("name") ? intent.getStringExtra("name") : "Community");
            iconUrl = intent.hasExtra("icon_url") ? intent.getStringExtra("icon_url") : "";
            bannerUrl = intent.hasExtra("banner_url") ? intent.getStringExtra("banner_url") : "";
            description = intent.hasExtra("description") ? intent.getStringExtra("description") : "";
            isGuest = intent.getBooleanExtra("isGuest", false);
        }

        if (txtCommunityTitle != null && communityName != null && communityName.length() > 0) {
            txtCommunityTitle.setText(communityName);
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
                JSONObject fastLogin = jsonCfg.optJSONObject("FAST_LOGIN");
                if (fastLogin != null) {
                    username = fastLogin.optString("username", "");
                    tokenSession = fastLogin.optString("token_session", "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (username == null || username.length() == 0) {
            isGuest = true;
        }

        // Cabeçalho visual da comunidade adicionado como HeaderView do ListView
        View headerView = buildCommunityHeaderView();
        if (listViewCommunityFeed != null) {
            listViewCommunityFeed.addHeaderView(headerView, null, false);
            adapterPosts = new CommunityPostAdapter(this, communityPosts);
            listViewCommunityFeed.setAdapter(adapterPosts);

            listViewCommunityFeed.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {}

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (!isLoading && hasMore && totalItemCount > 1 && (firstVisibleItem + visibleItemCount >= totalItemCount - 2)) {
                        feedPage++;
                        loadCommunityPosts(feedPage, false);
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
            });
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopActiveVideo();
                    finish();
                }
            });
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(CommunityFeedActivity.this, profile.class));
                }
            });
        }

        if (btnRefreshCommunity != null) {
            btnRefreshCommunity.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    refreshCommunityFeed();
                }
            });
        }

        if (newPost != null) {
            newPost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isGuest) {
                        Toast.makeText(CommunityFeedActivity.this, "Login required to post", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent postIntent = new Intent(CommunityFeedActivity.this, NewPostCommunity.class);
                    postIntent.putExtra("community_id", communityId);
                    postIntent.putExtra("community_name", communityName);
                    startActivity(postIntent);
                }
            });
        }

        loadCommunityPosts(1, true);
    }

    private View buildCommunityHeaderView() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        AbsListView.LayoutParams cardParams = new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        card.setLayoutParams(cardParams);

        FrameLayout bannerFrame = new FrameLayout(this);
        LinearLayout.LayoutParams bannerFrameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                (int) (screenWidth * 0.34f)
        );
        bannerFrame.setLayoutParams(bannerFrameParams);
        bannerFrame.setBackgroundColor(Color.parseColor("#2A3441"));

        imgBanner = new ImageView(this);
        imgBanner.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT
        ));
        imgBanner.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bannerFrame.addView(imgBanner);

        card.addView(bannerFrame);

        LinearLayout infoRow = new LinearLayout(this);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);
        infoRow.setGravity(Gravity.CENTER_VERTICAL);
        infoRow.setPadding(16, 12, 16, 8);
        card.addView(infoRow);

        imgCommunityAvatar = new ImageView(this);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(64, 64);
        avatarParams.setMargins(0, 0, 14, 0);
        imgCommunityAvatar.setLayoutParams(avatarParams);
        imgCommunityAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgCommunityAvatar.setBackgroundColor(Color.parseColor("#D8DEE6"));
        infoRow.addView(imgCommunityAvatar);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        ));
        infoRow.addView(textCol);

        txtHeaderCommunityName = new TextView(this);
        txtHeaderCommunityName.setTextSize(17);
        txtHeaderCommunityName.setTypeface(null, Typeface.BOLD);
        txtHeaderCommunityName.setTextColor(Color.parseColor("#111111"));
        txtHeaderCommunityName.setText(communityName != null ? communityName : "Community");
        textCol.addView(txtHeaderCommunityName);

        txtDescription = new TextView(this);
        txtDescription.setTextSize(13);
        txtDescription.setTextColor(Color.parseColor("#444444"));
        txtDescription.setPadding(16, 0, 16, 14);
        if (description != null && description.trim().length() > 0) {
            txtDescription.setText(description);
        } else {
            txtDescription.setVisibility(View.GONE);
        }
        card.addView(txtDescription);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 8));
        divider.setBackgroundColor(Color.parseColor("#E5E8EC"));
        card.addView(divider);

        if (iconUrl != null && iconUrl.trim().length() > 0 && !"null".equalsIgnoreCase(iconUrl)) {
            imageLoader.LoadImageUrl(iconUrl, imgCommunityAvatar);
        } else {
            imgCommunityAvatar.setImageBitmap(AvatarGenerator.generateAvatar(communityName, 128));
        }

        if (bannerUrl != null && bannerUrl.trim().length() > 0 && !"null".equalsIgnoreCase(bannerUrl)) {
            imageLoader.LoadImageUrl(bannerUrl, imgBanner);
        }

        return card;
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

    public void refreshCommunityFeed() {
        stopActiveVideo();
        feedPage = 1;
        hasMore = true;
        communityPosts.clear();
        if (adapterPosts != null) adapterPosts.notifyDataSetChanged();
        loadCommunityPosts(1, true);
    }

    private void loadCommunityPosts(int page, boolean forceClear) {
        if (isLoading) return;
        isLoading = true;
        new FetchCommunityPostsTask(page, forceClear).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (communityPosts.size() == 0) {
            loadCommunityPosts(1, true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopActiveVideo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopActiveVideo();
    }

    private String getCleanBaseUrl() {
        String baseUrl = (serverUrl != null && serverUrl.trim().length() > 0)
                ? serverUrl.trim()
                : "https://patient-hall-2915.luizsgustavo76.workers.dev";
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private class FetchCommunityPostsTask extends AsyncTask<Void, Void, String> {
        private final int pageRequested;
        private final boolean forceClear;

        FetchCommunityPostsTask(int pageRequested, boolean forceClear) {
            this.pageRequested = pageRequested;
            this.forceClear = forceClear;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                String targetUrl = getCleanBaseUrl() + "/view-posts?page=" + pageRequested + "&limit=20";
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("community_id", communityId);
                jsonParam.put("page", pageRequested);
                jsonParam.put("limit", 20);
                if (username != null && username.length() > 0) {
                    jsonParam.put("username", username);
                }

                return request.requestHTTP(targetUrl, "POST", jsonParam, 0, CommunityFeedActivity.this);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            isLoading = false;
            if (isFinishing()) return;

            if (result != null && result.trim().length() > 0) {
                try {
                    String trimmed = result.trim();
                    if (trimmed.startsWith("[")) {
                        JSONArray jsonArray = new JSONArray(trimmed);
                        if (jsonArray.length() < 20) {
                            hasMore = false;
                        }
                        if (forceClear || pageRequested == 1) {
                            communityPosts.clear();
                        }
                        for (int i = 0; i < jsonArray.length(); i++) {
                            communityPosts.add(jsonArray.getJSONObject(i));
                        }
                        if (adapterPosts != null) adapterPosts.notifyDataSetChanged();
                    } else if (trimmed.startsWith("{")) {
                        JSONObject errObj = new JSONObject(trimmed);
                        String msg = errObj.optString("error", errObj.optString("status", "Error loading community posts"));
                        Toast.makeText(CommunityFeedActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                hasMore = false;
                if (pageRequested == 1) {
                    Toast.makeText(CommunityFeedActivity.this, "Failed to load community feed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private class VotePollTask extends AsyncTask<Void, Void, String> {
        private final JSONObject postObj;
        private final JSONObject pollObj;
        private final int pollId;
        private final int optionId;

        public VotePollTask(JSONObject postObj, JSONObject pollObj, int pollId, int optionId) {
            this.postObj = postObj;
            this.pollObj = pollObj;
            this.pollId = pollId;
            this.optionId = optionId;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("username", username);
                payload.put("token_session", tokenSession);
                payload.put("poll_id", pollId);
                payload.put("option_id", optionId);

                return request.requestHTTP(getCleanBaseUrl() + "/vote-poll", "POST", payload, CommunityFeedActivity.this);
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
                        if (adapterPosts != null) adapterPosts.notifyDataSetChanged();
                        return;
                    }
                    String status = resp.optString("status", "");
                    if (status.contains("error") || status.contains("forbidden")) {
                        Toast.makeText(CommunityFeedActivity.this, resp.optString("message", status), Toast.LENGTH_SHORT).show();
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
                        if (adapterPosts != null) adapterPosts.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(CommunityFeedActivity.this, "Erro ao votar na enquete", Toast.LENGTH_SHORT).show();
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
    }

    private class CommunityPostAdapter extends BaseAdapter {
        private final Context context;
        private final ArrayList<JSONObject> list;
        private final ImageLoader imageLoader;
        private final Bookmark bookmarkManager;

        public CommunityPostAdapter(Context context, ArrayList<JSONObject> list) {
            this.context = context;
            this.list = list;
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
                lp.setMargins(0, 8, 0, 8);
                pollLayout.setLayoutParams(lp);
                pollLayout.setVisibility(View.GONE);
                parent.addView(pollLayout, idx + 1);
                return pollLayout;
            }
            return null;
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
                            Toast.makeText(CommunityFeedActivity.this, "Login required to vote", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (pollId != -1 && optionId != -1) {
                            new VotePollTask(post, pollObj, pollId, optionId).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

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
                final boolean isExternal = origin_server.length() > 0 && !origin_server.equalsIgnoreCase("linka");

                if (holder.btnPostOptions != null) {
                    holder.btnPostOptions.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final String[] optionsPost = postUsername.equals(username)
                                    ? new String[]{"Edit Post", "Delete Post", "View Profile", "Share"}
                                    : new String[]{"View Profile", "Share"};

                            AlertDialog.Builder builder = new AlertDialog.Builder(CommunityFeedActivity.this);
                            builder.setItems(optionsPost, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String chosen = optionsPost[which];
                                    if ("Edit Post".equals(chosen)) {
                                        Intent intent = new Intent(CommunityFeedActivity.this, newPost.class);
                                        intent.putExtra("isEdited", true);
                                        intent.putExtra("postId", id);
                                        intent.putExtra("textPost", textPost);
                                        intent.putExtra("community_id", communityId);
                                        startActivity(intent);
                                    } else if ("Delete Post".equals(chosen)) {
                                        new AsyncTask<Void, Void, Void>() {
                                            @Override
                                            protected Void doInBackground(Void... params) {
                                                try {
                                                    JSONObject jsonDelete = new JSONObject();
                                                    jsonDelete.put("post_id", id);
                                                    jsonDelete.put("username", username);
                                                    jsonDelete.put("community_id", communityId);
                                                    request.requestHTTP(getCleanBaseUrl() + "/delete-post", "POST", jsonDelete, CommunityFeedActivity.this);
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                                return null;
                                            }
                                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                                        if (position >= 0 && position < list.size()) {
                                            list.remove(position);
                                            notifyDataSetChanged();
                                            Toast.makeText(CommunityFeedActivity.this, "Post removed", Toast.LENGTH_SHORT).show();
                                        }
                                    } else if ("View Profile".equals(chosen)) {
                                        Intent intent = new Intent(CommunityFeedActivity.this, ViewProfile.class);
                                        intent.putExtra("usernameProfile", postUsername);
                                        startActivity(intent);
                                    } else if ("Share".equals(chosen)) {
                                        Intent sendIntent = new Intent();
                                        sendIntent.setAction(Intent.ACTION_SEND);
                                        sendIntent.putExtra(Intent.EXTRA_TEXT, "@" + postUsername + " (" + communityName + "): " + textPost);
                                        sendIntent.setType("text/plain");
                                        startActivity(Intent.createChooser(sendIntent, "Share post"));
                                    }
                                }
                            });
                            builder.show();
                        }
                    });
                }

                if (holder.btnReply != null) {
                    holder.btnReply.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(CommunityFeedActivity.this, NewPostCommunity.class);
                            intent.putExtra("community_id", communityId);
                            intent.putExtra("repostPostId", id);
                            intent.putExtra("repostUser", postUsername);
                            intent.putExtra("repostText", textPost);
                            intent.putExtra("repostDatetime", datetime);

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
                }

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
                        Intent intent = new Intent(CommunityFeedActivity.this, ViewProfile.class);
                        intent.putExtra("usernameProfile", postUsername);
                        startActivity(intent);
                    }
                });

                holder.btnComments.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(CommunityFeedActivity.this, comments_activity.class);
                        intent.putExtra("isExternal", isExternal);
                        if (isExternal) {
                            intent.putExtra("externalUrl", origin_server);
                            intent.putExtra("uniqueId", postUniqueId);
                            intent.putExtra("originalUrl", originalUrl);
                        }
                        intent.putExtra("post_author", postUsername);
                        intent.putExtra("post_date", datetime);
                        intent.putExtra("post_text", textPost);
                        intent.putExtra("post_community", communityName);
                        intent.putExtra("community_id", communityId);
                        intent.putExtra("avatar_url", post.optString("avatar_url", ""));
                        JSONArray mediaArr = post.optJSONArray("media_list");
                        intent.putExtra("media_list", mediaArr != null ? mediaArr.toString() : "[]");
                        intent.putExtra("comment_count", commentsCount);
                        intent.putExtra("isGuest", isGuest);
                        intent.putExtra("post_id", String.valueOf(id));
                        startActivity(intent);
                    }
                });

                holder.txtCommentCount.setText(commentsCount);
                holder.tvUsername.setText(postUsername.startsWith("@") ? postUsername : "@" + postUsername);
                holder.tvDate.setText(relative_datetime.length() > 0 ? relative_datetime : datetime);

                Bitmap defaultAvatar = AvatarGenerator.generateAvatar(postUsername, 128);
                holder.avatarPost.setImageBitmap(defaultAvatar);

                String avatarUrl = post.optString("avatar_url", "").trim();
                if (!avatarUrl.isEmpty() && !"null".equalsIgnoreCase(avatarUrl)) {
                    holder.avatarPost.setTag(avatarUrl);
                    imageLoader.LoadImageUrl(avatarUrl, holder.avatarPost);
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
                            fallbackObj.put("media_url", stringMedia);
                            fallbackObj.put("media_type", post.optString("media_type", "image"));
                            fallbackObj.put("width", post.optString("width", ""));
                            fallbackObj.put("heigth", post.optString("heigth", ""));
                            mediaItems.add(fallbackObj);
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

            } catch (Exception e) {
                Log.e("LINKA_DEBUG", "[COMMUNITY POST ERR] " + e.getMessage());
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

                            MediaController mediaController = new MediaController(CommunityFeedActivity.this);
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
                                    Toast.makeText(CommunityFeedActivity.this, "Erro ao reproduzir vídeo", Toast.LENGTH_SHORT).show();
                                    return true;
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
                        Intent intent = new Intent(CommunityFeedActivity.this, ViewPicture.class);
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
}