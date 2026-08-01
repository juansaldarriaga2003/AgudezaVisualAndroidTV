package com.agudezavisual.tv;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KioskKeyFilterService extends AccessibilityService {
    private static final String SETTINGS_PACKAGE = "com.android.tv.settings";

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
        int keyCode = event.getKeyCode();
        return keyCode == KeyEvent.KEYCODE_SETTINGS
            || keyCode == KeyEvent.KEYCODE_SEARCH
            || keyCode == KeyEvent.KEYCODE_ASSIST
            || keyCode == KeyEvent.KEYCODE_VOICE_ASSIST;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!BuildConfig.KIOSK_MODE
                || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || event.getPackageName() == null
                || !SETTINGS_PACKAGE.contentEquals(event.getPackageName())) {
            return;
        }

        long maintenanceUntil = getSharedPreferences("kiosk_admin", MODE_PRIVATE)
            .getLong("maintenance_until", 0L);
        if (System.currentTimeMillis() > maintenanceUntil) {
            performGlobalAction(GLOBAL_ACTION_HOME);
            Intent adminIntent = new Intent(this, MainActivity.class);
            adminIntent.putExtra("open_admin", true);
            adminIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );
            startActivity(adminIntent);
        }
    }

    @Override
    public void onInterrupt() {
        // No mantiene operaciones que deban interrumpirse.
    }
}
