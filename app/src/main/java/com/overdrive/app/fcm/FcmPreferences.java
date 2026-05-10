package com.overdrive.app.fcm;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Per-event FCM notification toggles.
 *
 * Stored in plain SharedPreferences (no sensitive data — just boolean flags).
 * Completely independent from Telegram's NotificationPreferences.
 *
 * Singleton — call init(Context) once from OverdriveApplication.onCreate().
 */
public class FcmPreferences {

    private static final String PREFS_NAME = "fcm_notification_prefs";

    private static final String KEY_MASTER = "fcm_enabled";
    private static final String KEY_MOTION = "fcm_motion";
    private static final String KEY_VIDEO = "fcm_video";
    private static final String KEY_CRITICAL = "fcm_critical";
    private static final String KEY_TUNNEL = "fcm_tunnel";
    private static final String KEY_PROXIMITY = "fcm_proximity";

    private static volatile FcmPreferences instance;

    private final SharedPreferences prefs;

    private FcmPreferences(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void init(Context context) {
        if (instance == null) {
            synchronized (FcmPreferences.class) {
                if (instance == null) {
                    instance = new FcmPreferences(context.getApplicationContext());
                }
            }
        }
    }

    public static FcmPreferences getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FcmPreferences.init(context) must be called first");
        }
        return instance;
    }

    public boolean isMasterEnabled() {
        return prefs.getBoolean(KEY_MASTER, true);
    }

    public void setMasterEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_MASTER, enabled).apply();
    }

    public boolean isMotionEnabled() {
        return prefs.getBoolean(KEY_MOTION, true);
    }

    public void setMotionEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_MOTION, enabled).apply();
    }

    public boolean isVideoEnabled() {
        return prefs.getBoolean(KEY_VIDEO, true);
    }

    public void setVideoEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VIDEO, enabled).apply();
    }

    public boolean isCriticalEnabled() {
        return prefs.getBoolean(KEY_CRITICAL, true);
    }

    public void setCriticalEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_CRITICAL, enabled).apply();
    }

    public boolean isTunnelEnabled() {
        return prefs.getBoolean(KEY_TUNNEL, true);
    }

    public void setTunnelEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_TUNNEL, enabled).apply();
    }

    public boolean isProximityEnabled() {
        return prefs.getBoolean(KEY_PROXIMITY, true);
    }

    public void setProximityEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_PROXIMITY, enabled).apply();
    }
}
