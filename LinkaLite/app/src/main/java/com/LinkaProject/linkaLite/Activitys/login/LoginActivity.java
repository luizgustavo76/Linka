package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.app.ProgressDialog;
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

import org.json.JSONException;
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
    private List<Integer> bannerCycle = new ArrayList<Integer>();
    private int currentBannerIndex = 0;
    private int currentDrawableRes = 0;

    private Bitmap currentBitmap = null;
    private Bitmap preloadedNextBitmap = null;
    private volatile boolean isDecoding = false;
    private volatile boolean isSlideRunning = false;

    private Handler slideHandler = new Handler();

    private String serverUrl = "http://patient-hall-2915.luizsgustavo76.workers.dev/";
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
            Log.d(TAG, "Canceling previous running LoginTask.");
            currentLoginTask.cancel(true);
        }
        currentLoginTask = new LoginTask();
        currentLoginTask.execute(username, password);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        bannerBackImageView = (ImageView) findViewById(R.id.imgBannerBack);
        bannerImageView = (ImageView) findViewById(R.id.imgBanner);

        refillCycle();
        currentDrawableRes = bannerCycle.get(0);
        currentBannerIndex = 1;
        currentBitmap = decodeBannerBitmap(currentDrawableRes);
        if (currentBitmap != null) {
            bannerImageView.setImageBitmap(currentBitmap);
            bannerBackImageView.setImageBitmap(currentBitmap);
        }

        preloadNextInBackground();

        btnServer = (Button) findViewById(R.id.btnServer);
        edtUsername = (EditText) findViewById(R.id.edtUsername);
        edtPassword = (EditText) findViewById(R.id.edtPassword);
        txtGuest = (TextView) findViewById(R.id.txtGuest);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        txtGoToSignup = (TextView) findViewById(R.id.txtGoToSignup);

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
                    Log.d(TAG, "[CONFIG] config.cfg does not exist. Creating default file...");
                    cfg.createDefaultConfig(LoginActivity.this, "config.cfg");
                }

                String fastUsername = "";
                String fastPassword = "";

                try {
                    String jsonString = cfg.loadCfgAsJson(LoginActivity.this, "config.cfg");
                    Log.d(TAG, "[CONFIG] Current content of config.cfg: " + jsonString);

                    JSONObject jsonCfg = new JSONObject(jsonString);
                    JSONObject server = jsonCfg.optJSONObject("SERVER");
                    if (server != null) {
                        String cfgUrl = server.optString("url", "").trim();
                        if (cfgUrl.length() > 0) {
                            serverUrl = cfgUrl;
                        }
                        Log.d(TAG, "[CONFIG] Active server URL: " + serverUrl);
                    }
                    JSONObject fastLogin = jsonCfg.optJSONObject("FAST_LOGIN");
                    if (fastLogin != null) {
                        fastUsername = fastLogin.optString("username", "");
                        fastPassword = fastLogin.optString("password", "");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "[CONFIG ERROR] Error processing configuration JSON:", e);
                }

                final String finalUser = fastUsername;
                final String finalPass = fastPassword;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing() && finalUser.length() > 0 && finalPass.length() > 0) {
                            Log.d(TAG, "[FAST LOGIN] Executing auto-login for user: " + finalUser);
                            executeLogin(finalUser, finalPass);
                        }
                    }
                });
            }
        }).start();

        btnServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ChangeServer.class);
                startActivity(intent);
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = edtUsername.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();
                if (username.length() == 0 || password.length() == 0) {
                    Toast.makeText(LoginActivity.this, "Fill all fields", Toast.LENGTH_SHORT).show();
                } else {
                    executeLogin(username, password);
                }
            }
        });

        txtGoToSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
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

    private class LoginTask extends AsyncTask<String, Void, String> {
        private ProgressDialog progressDialog;
        private String attemptedUsername;
        private String attemptedPassword;

        @Override
        protected void onPreExecute() {
            if (!LoginActivity.this.isFinishing()) {
                progressDialog = new ProgressDialog(LoginActivity.this);
                progressDialog.setTitle("Wait");
                progressDialog.setMessage("Connecting to server...");
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            if (isCancelled()) return null;

            attemptedUsername = params[0];
            attemptedPassword = params[1];

            try {
                String baseUrl = serverUrl.trim();
                while (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }

                String targetUrl = baseUrl + "/login";
                Log.d(TAG, "[HTTP] Sending POST to: " + targetUrl);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("username", attemptedUsername);
                jsonParam.put("password", attemptedPassword);

                return request.requestHTTP(targetUrl, "POST", jsonParam, 0, LoginActivity.this);
            } catch (Exception e) {
                Log.e(TAG, "[HTTP EXCEPTION] Error preparing login request: ", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            dismissDialogSafely();
            if (isCancelled() || LoginActivity.this.isFinishing()) return;

            Log.d(TAG, "[POST EXECUTE] Result received: " + result);

            if (result != null && result.length() > 0) {
                try {
                    JSONObject responseJson = new JSONObject(result);
                    String status = responseJson.optString("status", "");

                    if (status.equalsIgnoreCase("login is sucessful") ||
                        status.equalsIgnoreCase("login is successful") ||
                        status.equalsIgnoreCase("success")) {

                        Log.d(TAG, "[TOKEN] Requesting session token...");
                        String newToken = tokenManager.newSession(LoginActivity.this, attemptedUsername, attemptedPassword, serverUrl);

                        config cfg = new config();
                        cfg.updateCfg(LoginActivity.this, "config.cfg", "FAST_LOGIN", "username", attemptedUsername);
                        cfg.updateCfg(LoginActivity.this, "config.cfg", "FAST_LOGIN", "password", attemptedPassword);
                        if (newToken.length() > 0) {
                            cfg.updateCfg(LoginActivity.this, "config.cfg", "FAST_LOGIN", "token_session", newToken);
                        }

                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.w(TAG, "[LOGIN DENIED] Incorrect status: " + status);
                        Toast.makeText(LoginActivity.this, "Username or password incorrect!", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "[JSON ERROR] Exception reading returned JSON: ", e);
                    Toast.makeText(LoginActivity.this, "Error processing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "[CONNECTION FAILED] Empty or null response returned from network.");
                Toast.makeText(LoginActivity.this, "Connection with server failed", Toast.LENGTH_LONG).show();
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