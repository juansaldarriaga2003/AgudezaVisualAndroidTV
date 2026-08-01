package com.agudezavisual.tv;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KioskKeyFilterService extends AccessibilityService {
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
        // El servicio solo filtra teclas; no inspecciona contenido de pantalla.
    }

    @Override
    public void onInterrupt() {
        // No mantiene operaciones que deban interrumpirse.
    }
}
