package com.agudezavisual.tv;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KioskKeyFilterService extends AccessibilityService {
    private static final String APP_PACKAGE = "com.agudezavisual.tv.kiosk";
    private static final String VENDOR_VIDEO_PACKAGE = "com.android.smart.terminal";
    private static final String GOOGLE_TV_LAUNCHER = "com.google.android.tvlauncher";
    private static final String ANDROID_TV_SETTINGS = "com.android.tv.settings";
    private static final String ANDROID_SETTINGS = "com.android.settings";
    private long lastAdminRequestAt = 0L;

    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = getServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        setServiceInfo(info);
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (!BuildConfig.KIOSK_MODE) {
            return false;
        }
        if (isNavigationUnlocked()) {
            return false;
        }
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                requestAdminAccess();
            }
            return true;
        }
        return !isAllowedApplicationKey(keyCode);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!BuildConfig.KIOSK_MODE
                || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || event.getPackageName() == null) {
            return;
        }

        String packageName = event.getPackageName().toString();
        if (APP_PACKAGE.equals(packageName)) {
            return;
        }
        if (isNavigationUnlocked()) {
            return;
        }
        if (GOOGLE_TV_LAUNCHER.equals(packageName)
                || ANDROID_TV_SETTINGS.equals(packageName)
                || ANDROID_SETTINGS.equals(packageName)) {
            requestAdminAccess();
            return;
        }
        if (VENDOR_VIDEO_PACKAGE.equals(packageName)) {
            performGlobalAction(GLOBAL_ACTION_HOME);
            Intent appIntent = new Intent(this, MainActivity.class);
            appIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );
            startActivity(appIntent);
            return;
        }
        returnToKiosk();
    }

    private boolean isAllowedApplicationKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
            case KeyEvent.KEYCODE_CHANNEL_UP:
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
            case KeyEvent.KEYCODE_PAGE_UP:
            case KeyEvent.KEYCODE_PAGE_DOWN:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                return true;
            default:
                return false;
        }
    }

    private void requestAdminAccess() {
        long now = System.currentTimeMillis();
        if (now - lastAdminRequestAt < 800L) {
            return;
        }
        lastAdminRequestAt = now;
        Intent adminIntent = new Intent(this, MainActivity.class);
        adminIntent.putExtra("open_admin", true);
        adminIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        startActivity(adminIntent);
    }

    private boolean isNavigationUnlocked() {
        return getSharedPreferences("kiosk_admin", MODE_PRIVATE)
            .getBoolean("navigation_unlocked", false);
    }

    private void returnToKiosk() {
        Intent appIntent = new Intent(this, MainActivity.class);
        appIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        startActivity(appIntent);
    }

    @Override
    public void onInterrupt() {
    }
}
