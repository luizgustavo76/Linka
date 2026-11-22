package com.LinkaProject.linkaLite;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.LinkaProject.linkaLite.BaseActivity;
import com.LinkaProject.linkaLite.ChangeServer;
import com.LinkaProject.linkaLite.HomeActivity;
import com.LinkaProject.linkaLite.LinkaDialog;
import com.LinkaProject.linkaLite.R;
import com.LinkaProject.linkaLite.SignupActivity;
import com.LinkaProject.linkaLite.config;
import com.LinkaProject.linkaLite.request;
import com.LinkaProject.linkaLite.tokenManager;

import org.json.JSONObject;

public class LoginActivity extends BaseActivity {
    private static final String TAG = "LinkaLogin";

    private EditText edtUsername;
    private EditText edtPassword;
    private Button btnServer;
    private Button btnLogin;
    private TextView txtGoToSignup;
    private TextView txtGuest;
    private ImageView bgImageView;

    private Bitmap bgBitmap = null;

    private volatile String serverUrl = (Build.VERSION.SDK_INT >= 19)
            ? "https://patient-hall-2915.luizsgustavo76.workers.dev"
            : "http://patient-hall-2915.luizsgustavo76.workers.dev";
    private LoginTask currentLoginTask;

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

    private Bitmap decodeSingleBackground(int resId) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            // No Galaxy Y (telas pequenas), reduz pela metade para economizar RAM
            if (getResources().getDisplayMetrics().widthPixels <= 320) {
                opts.inSampleSize = 2;
            }
            return BitmapFactory.decodeResource(getResources(), resId, opts);
        } catch (OutOfMemoryError oom) {
            Log.e(TAG, "Out of memory decoding bg_login", oom);
            return null;
        }
    }

    private void executeLogin(String username, String password) {
        if (currentLoginTask != null && currentLoginTask.getStatus() == AsyncTask.Status.RUNNING) {
            currentLoginTask.cancel(true);
        }
        currentLoginTask = new LoginTask();
        if (Build.VERSION.SDK_INT >= 11) {
            currentLoginTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, username, password);
        } else {
            currentLoginTask.execute(username, password);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        bgImageView = (ImageView) findViewById(R.id.img_bg_polaroid);
        btnServer = (Button) findViewById(R.id.btnServer);
        edtUsername = (EditText) findViewById(R.id.edtUsername);
        edtPassword = (EditText) findViewById(R.id.edtPassword);
        txtGuest = (TextView) findViewById(R.id.txtGuest);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        txtGoToSignup = (TextView) findViewById(R.id.txtGoToSignup);

        // Decodifica apenas o R.drawable.bg_login do drawable-nodpi
        bgBitmap = decodeSingleBackground(R.drawable.bg_login);
        if (bgBitmap != null && bgImageView != null) {
            bgImageView.setImageBitmap(bgBitmap);

            // Efeito Ken Burns suave
            ScaleAnimation kenBurns = new ScaleAnimation(
                    1.0f, 1.07f, 1.0f, 1.07f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            kenBurns.setDuration(14000);
            kenBurns.setRepeatCount(Animation.INFINITE);
            kenBurns.setRepeatMode(Animation.REVERSE);
            bgImageView.startAnimation(kenBurns);
        }

        // Animação de entrada em cascata
        View boxTitle = findViewById(R.id.box_title);
        View cardLogin = findViewById(R.id.card_login);

        if (boxTitle != null) {
            AnimationSet titleAnim = new AnimationSet(true);
            titleAnim.addAnimation(new AlphaAnimation(0.0f, 1.0f));
            titleAnim.addAnimation(new TranslateAnimation(0, 0, -25, 0));
            titleAnim.setDuration(450);
            boxTitle.startAnimation(titleAnim);
        }

        if (cardLogin != null) {
            AnimationSet cardAnim = new AnimationSet(true);
            cardAnim.addAnimation(new AlphaAnimation(0.0f, 1.0f));
            cardAnim.addAnimation(new TranslateAnimation(0, 0, 35, 0));
            cardAnim.setDuration(500);
            cardAnim.setStartOffset(150);
            cardLogin.startAnimation(cardAnim);
        }

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
    protected void onDestroy() {
        super.onDestroy();
        if (currentLoginTask != null) {
            currentLoginTask.cancel(true);
            currentLoginTask.dismissDialogSafely();
        }
        if (bgImageView != null) {
            bgImageView.clearAnimation();
            bgImageView.setImageDrawable(null);
        }
        if (bgBitmap != null && !bgBitmap.isRecycled()) {
            bgBitmap.recycle();
            bgBitmap = null;
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
                if (Build.VERSION.SDK_INT < 19) {
                    if (baseUrl.startsWith("https://")) {
                        baseUrl = "http://" + baseUrl.substring(8);
                    }
                } else if (baseUrl.startsWith("http://")) {
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
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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