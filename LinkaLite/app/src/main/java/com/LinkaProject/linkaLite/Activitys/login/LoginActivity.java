package com.LinkaProject.linkaLite;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoginActivity extends BaseActivity {
    private static final String TAG = "LinkaLogin";
    private static final int SLIDE_INTERVAL_MS = 5000;
    private static final int FADE_DURATION_MS = 1200;

    private EditText edtUsername;
    private EditText edtPassword;
    private Button btnServer;
    private Button btnLogin;
    private TextView txtGoToSignup;
    private TextView txtGuest;

    private ImageView bannerImageView;
    private ImageView bannerBackImageView;
    private final List<Integer> bannerCycle = new ArrayList<Integer>();
    private int currentBannerIndex = 0;
    private int currentDrawableRes = 0;

    private Bitmap currentBitmap = null;
    private volatile Bitmap preloadedNextBitmap = null;
    private volatile boolean isDecoding = false;
    private volatile boolean isSlideRunning = false;

    private final Handler slideHandler = new Handler();

    private volatile String serverUrl = "https://patient-hall-2915.luizsgustavo76.workers.dev";
    private LoginTask currentLoginTask;

    private final Runnable slideRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing() && isSlideRunning) {
                advanceSlide();
                slideHandler.postDelayed(this, SLIDE_INTERVAL_MS);
            }
        }
    };

    private static class LoginResult {
        final boolean success;
        final String title;
        final String message;

        LoginResult(boolean success, String title, String message) {
            this.success = success;
            this.title = title;
            this.message = message;
        }
    }

    private Bitmap decodeBannerBitmap(int resId) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            opts.inSampleSize = 2;
            return BitmapFactory.decodeResource(getResources(), resId, opts);
        } catch (OutOfMemoryError oom) {
            Log.e(TAG, "Out of memory decoding banner", oom);
            return null;
        }
    }

    private void refillCycle() {
        int lastShown = currentDrawableRes;
        bannerCycle.clear();
        bannerCycle.add(R.drawable.linka_login);
        bannerCycle.add(R.drawable.linka_login_2);
        bannerCycle.add(R.drawable.linka_login_3);
        bannerCycle.add(R.drawable.linka_login_4);
        bannerCycle.add(R.drawable.linka_login_5);
        Collections.shuffle(bannerCycle);

        if (lastShown != 0 && bannerCycle.get(0) == lastShown && bannerCycle.size() > 1) {
            Collections.swap(bannerCycle, 0, 1);
        }
        currentBannerIndex = 0;
    }

    private void preloadNextInBackground() {
        if (isDecoding || isFinishing()) return;

        if (currentBannerIndex >= bannerCycle.size()) {
            refillCycle();
        }

        final int nextResId = bannerCycle.get(currentBannerIndex);
        currentBannerIndex++;
        currentDrawableRes = nextResId;
        isDecoding = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap decoded = decodeBannerBitmap(nextResId);
                if (isFinishing()) {
                    if (decoded != null) decoded.recycle();
                    return;
                }
                preloadedNextBitmap = decoded;
                isDecoding = false;
            }
        }).start();
    }

    private void advanceSlide() {
        if (bannerImageView == null || bannerBackImageView == null) return;

        if (isDecoding || preloadedNextBitmap == null) {
            if (!isDecoding) preloadNextInBackground();
            return;
        }

        if (currentBitmap != null) {
            bannerBackImageView.setImageBitmap(currentBitmap);
        }

        currentBitmap = preloadedNextBitmap;
        preloadedNextBitmap = null;

        bannerImageView.setImageBitmap(currentBitmap);

        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(FADE_DURATION_MS);
        bannerImageView.startAnimation(fadeIn);

        preloadNextInBackground();
    }

    private void executeLogin(String username, String password) {
        if (currentLoginTask != null && currentLoginTask.getStatus() == AsyncTask.Status.RUNNING) {
            currentLoginTask.cancel(true);
        }
        currentLoginTask = new LoginTask();
        currentLoginTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, username, password);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        bannerBackImageView = (ImageView) findViewById(R.id.imgBannerBack);
        bannerImageView = (ImageView) findViewById(R.id.imgBanner);
        btnServer = (Button) findViewById(R.id.btnServer);
        edtUsername = (EditText) findViewById(R.id.edtUsername);
        edtPassword = (EditText) findViewById(R.id.edtPassword);
        txtGuest = (TextView) findViewById(R.id.txtGuest);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        txtGoToSignup = (TextView) findViewById(R.id.txtGoToSignup);

        refillCycle();
        final int firstResId = bannerCycle.get(0);
        currentDrawableRes = firstResId;
        currentBannerIndex = 1;

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap firstBmp = decodeBannerBitmap(firstResId);
                if (isFinishing()) {
                    if (firstBmp != null) firstBmp.recycle();
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing() && firstBmp != null) {
                            currentBitmap = firstBmp;
                            bannerImageView.setImageBitmap(currentBitmap);
                            bannerBackImageView.setImageBitmap(currentBitmap);
                            preloadNextInBackground();
                        }
                    }
                });
            }
        }).start();

        TextWatcher typingPauseWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                slideHandler.removeCallbacks(slideRunnable);
                if (isSlideRunning) {
                    slideHandler.postDelayed(slideRunnable, SLIDE_INTERVAL_MS);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        edtUsername.addTextChangedListener(typingPauseWatcher);
        edtPassword.addTextChangedListener(typingPauseWatcher);

        txtGuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                intent.putExtra("isGuest", true);
                startActivity(intent);
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                config cfg = new config();
                if (!config.configFileExists(LoginActivity.this, "config.cfg")) {
                    cfg.createDefaultConfig(LoginActivity.this, "config.cfg");
                }

                String fastUsername = "";
                String fastPassword = "";

                try {
                    String jsonString = cfg.loadCfgAsJson(LoginActivity.this, "config.cfg");
                    JSONObject jsonCfg = new JSONObject(jsonString);
                    JSONObject server = jsonCfg.optJSONObject("SERVER");
                    if (server != null) {
                        String cfgUrl = server.optString("url", "").trim();
                        if (cfgUrl.length() > 0) {
                            serverUrl = cfgUrl;
                        }
                    }
                    JSONObject fastLogin = jsonCfg.optJSONObject("FAST_LOGIN");
                    if (fastLogin != null) {
                        fastUsername = fastLogin.optString("username", "");
                        fastPassword = fastLogin.optString("password", "");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "[CONFIG ERROR] Error processing configuration JSON:", e);
                }

                final String finalUser = fastUsername;
                final String finalPass = fastPassword;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing() && finalUser.length() > 0 && finalPass.length() > 0) {
                            executeLogin(finalUser, finalPass);
                        }
                    }
                });
            }
        }).start();

        btnServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, ChangeServer.class));
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = edtUsername.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();
                if (username.length() == 0 || password.length() == 0) {
                    LinkaDialog.showAlert(LoginActivity.this, "Missing Fields", "Please fill in your username and password.");
                } else {
                    executeLogin(username, password);
                }
            }
        });

        txtGoToSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isSlideRunning = true;
        slideHandler.removeCallbacks(slideRunnable);
        slideHandler.postDelayed(slideRunnable, SLIDE_INTERVAL_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isSlideRunning = false;
        slideHandler.removeCallbacks(slideRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isSlideRunning = false;
        slideHandler.removeCallbacks(slideRunnable);
        if (currentLoginTask != null) {
            currentLoginTask.cancel(true);
            currentLoginTask.dismissDialogSafely();
        }
    }

    private class LoginTask extends AsyncTask<String, Void, LoginResult> {
        private Dialog progressDialog;

        @Override
        protected void onPreExecute() {
            if (!LoginActivity.this.isFinishing()) {
                progressDialog = LinkaDialog.showLoading(LoginActivity.this, "Connecting to server...");
            }
        }

        @Override
        protected LoginResult doInBackground(String... params) {
            if (isCancelled()) return null;

            String attemptedUsername = params[0];
            String attemptedPassword = params[1];

            try {
                String baseUrl = serverUrl.trim();
                if (baseUrl.startsWith("http://")) {
                    baseUrl = "https://" + baseUrl.substring(7);
                }
                while (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("username", attemptedUsername);
                jsonParam.put("password", attemptedPassword);

                String result = request.requestHTTP(baseUrl + "/login", "POST", jsonParam, 0, LoginActivity.this);
                if (result == null || result.length() == 0) {
                    return new LoginResult(false, "Connection Error", "Could not connect to the server. Check your internet connection.");
                }

                JSONObject responseJson = new JSONObject(result);
                String status = responseJson.optString("status", "");

                if (status.equalsIgnoreCase("login is sucessful") ||
                    status.equalsIgnoreCase("login is successful") ||
                    status.equalsIgnoreCase("success")) {

                    String newToken = tokenManager.newSession(LoginActivity.this, attemptedUsername, attemptedPassword, baseUrl);

                    config cfg = new config();
                    cfg.updateCfg(LoginActivity.this, "config.cfg", "FAST_LOGIN", "username", attemptedUsername);
                    cfg.updateCfg(LoginActivity.this, "config.cfg", "FAST_LOGIN", "password", attemptedPassword);
                    if (newToken != null && newToken.length() > 0) {
                        cfg.updateCfg(LoginActivity.this, "config.cfg", "FAST_LOGIN", "token_session", newToken);
                    }

                    return new LoginResult(true, "", "Login successful!");
                } else {
                    return new LoginResult(false, "Incorrect Login", "The username or password you entered is incorrect. Please try again.");
                }
            } catch (Exception e) {
                Log.e(TAG, "[LOGIN EXCEPTION] Error during login: ", e);
                return new LoginResult(false, "Error", "Error processing data: " + e.getMessage());
            }
        }

        @Override
        protected void onPostExecute(LoginResult loginResult) {
            dismissDialogSafely();
            if (isCancelled() || LoginActivity.this.isFinishing() || loginResult == null) return;

            if (loginResult.success) {
                Toast.makeText(LoginActivity.this, loginResult.message, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                finish();
            } else {
                LinkaDialog.showAlert(LoginActivity.this, loginResult.title, loginResult.message);
            }
        }

        @Override
        protected void onCancelled() {
            dismissDialogSafely();
        }

        public void dismissDialogSafely() {
            try {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error dismissing dialog", e);
            }
        }
    }
}