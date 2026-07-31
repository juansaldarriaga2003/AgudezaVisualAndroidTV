package com.agudezavisual.tv;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

public class MainActivity extends Activity {
    private static final String APP_URL =
        "https://appassets.androidplatform.net/assets/index.html";

    private WebView webView;
    private long lastBackPress = 0;
    private final Handler adminHandler = new Handler(Looper.getMainLooper());
    private boolean adminLongPressTriggered = false;
    private boolean adminDialogVisible = false;
    private boolean kioskNoticeShown = false;
    private final Runnable showAdminRunnable = new Runnable() {
        @Override
        public void run() {
            adminLongPressTriggered = true;
            showAdminExitDialog();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        enterImmersiveMode();

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUserAgentString(
            settings.getUserAgentString() + " AgudezaVisualOffline/2.1"
        );

        WebView.setWebContentsDebuggingEnabled(false);
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
            .build();
        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(
                    WebView view, android.webkit.WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public android.webkit.WebResourceResponse shouldInterceptRequest(
                    WebView view, String url) {
                return assetLoader.shouldInterceptRequest(android.net.Uri.parse(url));
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        setContentView(webView);

        if (savedInstanceState == null) {
            webView.loadUrl(APP_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }

        webView.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BuildConfig.KIOSK_MODE) {
            activateKioskMode();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (BuildConfig.KIOSK_MODE && isConfirmKey(event.getKeyCode())) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getRepeatCount() == 0 && !adminDialogVisible) {
                    adminLongPressTriggered = false;
                    adminHandler.postDelayed(showAdminRunnable, 4000);
                }
                return true;
            }
            if (event.getAction() == KeyEvent.ACTION_UP) {
                adminHandler.removeCallbacks(showAdminRunnable);
                if (!adminLongPressTriggered && !adminDialogVisible) {
                    sendWebKey("Enter", 13);
                }
                return true;
            }
        }

        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event);
        }

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_CHANNEL_UP:
            case KeyEvent.KEYCODE_PAGE_UP:
                sendWebKey("ArrowUp", 38);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
            case KeyEvent.KEYCODE_PAGE_DOWN:
                sendWebKey("ArrowDown", 40);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                sendWebKey("ArrowLeft", 37);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                sendWebKey("ArrowRight", 39);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_BUTTON_SELECT:
            case KeyEvent.KEYCODE_BUTTON_START:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                sendWebKey("Enter", 13);
                return true;
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                handleBack();
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    private boolean isConfirmKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER
            || keyCode == KeyEvent.KEYCODE_ENTER
            || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
            || keyCode == KeyEvent.KEYCODE_BUTTON_A
            || keyCode == KeyEvent.KEYCODE_BUTTON_SELECT
            || keyCode == KeyEvent.KEYCODE_BUTTON_START
            || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
            || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
    }

    private void sendWebKey(String key, int keyCode) {
        String script =
            "window.dispatchEvent(new KeyboardEvent('keydown',{" +
            "key:'" + key + "',code:'" + key + "',keyCode:" + keyCode + "," +
            "which:" + keyCode + ",bubbles:true,cancelable:true}));";
        webView.evaluateJavascript(script, null);
    }

    private void handleBack() {
        if (BuildConfig.KIOSK_MODE) {
            sendWebKey("Backspace", 8);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastBackPress < 1200) {
            finish();
            return;
        }
        lastBackPress = now;
        sendWebKey("Backspace", 8);
        Toast.makeText(this, R.string.back_hint, Toast.LENGTH_SHORT).show();
    }

    private void activateKioskMode() {
        DevicePolicyManager policyManager =
            (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, KioskDeviceAdminReceiver.class);

        if (policyManager.isDeviceOwnerApp(getPackageName())) {
            policyManager.setLockTaskPackages(admin, new String[] { getPackageName() });
            policyManager.setUninstallBlocked(admin, getPackageName(), true);
            IntentFilter homeFilter = new IntentFilter(Intent.ACTION_MAIN);
            homeFilter.addCategory(Intent.CATEGORY_HOME);
            homeFilter.addCategory(Intent.CATEGORY_DEFAULT);
            policyManager.addPersistentPreferredActivity(
                admin,
                homeFilter,
                new ComponentName(this, MainActivity.class)
            );
            policyManager.addUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT);
            policyManager.addUserRestriction(admin, UserManager.DISALLOW_ADD_USER);
            policyManager.addUserRestriction(admin, UserManager.DISALLOW_CREATE_WINDOWS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                policyManager.setLockTaskFeatures(
                    admin,
                    DevicePolicyManager.LOCK_TASK_FEATURE_NONE
                );
            }
        }

        if (policyManager.isLockTaskPermitted(getPackageName())
                && !isInLockTaskMode()) {
            startLockTask();
        } else if (!policyManager.isLockTaskPermitted(getPackageName())
                && !kioskNoticeShown) {
            kioskNoticeShown = true;
            Toast.makeText(
                this,
                R.string.kiosk_not_provisioned,
                Toast.LENGTH_LONG
            ).show();
        }
    }

    private boolean isInLockTaskMode() {
        ActivityManager activityManager =
            (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return activityManager.getLockTaskModeState()
                == ActivityManager.LOCK_TASK_MODE_LOCKED;
        }
        return false;
    }

    private void showAdminExitDialog() {
        if (adminDialogVisible || isFinishing()) {
            return;
        }
        adminDialogVisible = true;

        EditText pinInput = new EditText(this);
        pinInput.setHint(R.string.admin_pin_hint);
        pinInput.setInputType(
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD
        );
        pinInput.setSingleLine(true);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.admin_access)
            .setView(pinInput)
            .setPositiveButton(R.string.admin_exit, null)
            .setNegativeButton(R.string.cancel, null)
            .create();

        dialog.setOnDismissListener(ignored -> {
            adminDialogVisible = false;
            adminLongPressTriggered = false;
            enterImmersiveMode();
            webView.requestFocus();
        });
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                if ("2580".contentEquals(pinInput.getText())) {
                    exitKioskMode();
                    dialog.dismiss();
                } else {
                    pinInput.setError(getString(R.string.admin_pin_error));
                    pinInput.setText("");
                }
            });
        });
        dialog.show();
        pinInput.requestFocus();
    }

    private void exitKioskMode() {
        DevicePolicyManager policyManager =
            (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, KioskDeviceAdminReceiver.class);
        if (policyManager.isDeviceOwnerApp(getPackageName())) {
            policyManager.setUninstallBlocked(admin, getPackageName(), false);
            policyManager.clearPackagePersistentPreferredActivities(admin, getPackageName());
            policyManager.clearUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT);
            policyManager.clearUserRestriction(admin, UserManager.DISALLOW_ADD_USER);
            policyManager.clearUserRestriction(admin, UserManager.DISALLOW_CREATE_WINDOWS);
        }
        if (isInLockTaskMode()) {
            try {
                stopLockTask();
            } catch (IllegalStateException ignored) {
                // El sistema ya había liberado el bloqueo.
            }
        }

        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        adminHandler.removeCallbacks(showAdminRunnable);
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enterImmersiveMode();
            webView.requestFocus();
        }
    }

    private void enterImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }
}
