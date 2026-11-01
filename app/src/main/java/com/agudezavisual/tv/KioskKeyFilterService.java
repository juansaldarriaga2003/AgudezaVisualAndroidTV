package com.agudezavisual.tv;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KioskKeyFilterService extends AccessibilityService {
    private static final String APP_PACKAGE = "com.agudezavisual.tv.kiosk";
    private static final String VENDOR_VIDEO_PACKAGE = "com.android.smart.terminal";

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
        return keyCode == KeyEvent.KEYCODE_HOME
            || keyCode == KeyEvent.KEYCODE_APP_SWITCH
            || keyCode == KeyEvent.KEYCODE_SETTINGS
            || keyCode == KeyEvent.KEYCODE_SEARCH
            || keyCode == KeyEvent.KEYCODE_ASSIST
            || keyCode == KeyEvent.KEYCODE_VOICE_ASSIST
            || keyCode == KeyEvent.KEYCODE_GUIDE
            || keyCode == KeyEvent.KEYCODE_TV
            || keyCode == KeyEvent.KEYCODE_TV_INPUT;
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
