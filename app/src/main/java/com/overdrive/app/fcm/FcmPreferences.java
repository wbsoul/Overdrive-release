package com.overdrive.app.fcm;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-event FCM notification toggles.
 *
 * Follows the same pattern as UnifiedConfigManager:
 * - Single JSON file at /data/local/tmp/fcm_prefs.json
 * - World-readable + world-writable (accessible by both shell UID 2000 and app UID)
 * - Atomic writes via tmp-file rename
 * - In-memory cache to avoid repeated disk reads
 * - No Context or SharedPreferences dependency
 *
 * Prefs file: /data/local/tmp/fcm_prefs.json
 */
public class FcmPreferences {

    private static final String TAG = "FcmPreferences";
    private static final File PREFS_FILE = new File("/data/local/tmp/fcm_prefs.json");

    private static final String KEY_MASTER    = "fcm_enabled";
    private static final String KEY_MOTION    = "fcm_motion";
    private static final String KEY_VIDEO     = "fcm_video";
    private static final String KEY_CRITICAL  = "fcm_critical";
    private static final String KEY_TUNNEL    = "fcm_tunnel";
    private static final String KEY_PROXIMITY = "fcm_proximity";

    private static volatile FcmPreferences instance;

    private volatile JSONObject cache;
    private final AtomicLong lastModified = new AtomicLong(0);

    private FcmPreferences() {}

    /** Call once before use. Context parameter kept for API compatibility but unused. */
    public static void init(Context context) {
        if (instance == null) {
            synchronized (FcmPreferences.class) {
                if (instance == null) {
                    instance = new FcmPreferences();
                    Log.i(TAG, "Initialized — prefs file: " + PREFS_FILE);
                }
            }
        }
    }

    public static FcmPreferences getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FcmPreferences.init() must be called first");
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Public API — all boolean getters default to true
    // -------------------------------------------------------------------------

    public boolean isMasterEnabled()   { return getBool(KEY_MASTER,    true); }
    public boolean isMotionEnabled()   { return getBool(KEY_MOTION,    true); }
    public boolean isVideoEnabled()    { return getBool(KEY_VIDEO,     true); }
    public boolean isCriticalEnabled() { return getBool(KEY_CRITICAL,  true); }
    public boolean isTunnelEnabled()   { return getBool(KEY_TUNNEL,    true); }
    public boolean isProximityEnabled(){ return getBool(KEY_PROXIMITY, true); }

    public void setMasterEnabled(boolean v)   { setBool(KEY_MASTER,    v); }
    public void setMotionEnabled(boolean v)   { setBool(KEY_MOTION,    v); }
    public void setVideoEnabled(boolean v)    { setBool(KEY_VIDEO,     v); }
    public void setCriticalEnabled(boolean v) { setBool(KEY_CRITICAL,  v); }
    public void setTunnelEnabled(boolean v)   { setBool(KEY_TUNNEL,    v); }
    public void setProximityEnabled(boolean v){ setBool(KEY_PROXIMITY, v); }

    // -------------------------------------------------------------------------
    // File I/O — same pattern as UnifiedConfigManager
    // -------------------------------------------------------------------------

    private boolean getBool(String key, boolean defaultVal) {
        return load().optBoolean(key, defaultVal);
    }

    private void setBool(String key, boolean value) {
        JSONObject json = load();
        try { json.put(key, value); } catch (Exception e) { Log.e(TAG, "JSON error", e); }
        save(json);
    }

    private JSONObject load() {
        if (cache != null && PREFS_FILE.exists()) {
            long fileModified = PREFS_FILE.lastModified();
            if (fileModified <= lastModified.get()) return cache;
        }
        synchronized (this) {
            try {
                if (PREFS_FILE.exists()) {
                    String content = new String(java.nio.file.Files.readAllBytes(PREFS_FILE.toPath()));
                    JSONObject json = new JSONObject(content);
                    cache = json;
                    lastModified.set(PREFS_FILE.lastModified());
                    return json;
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to load prefs file, using defaults", e);
            }
            return new JSONObject();
        }
    }

    private void save(JSONObject json) {
        synchronized (this) {
            String content = null;
            try { content = json.toString(2); } catch (Exception e) { Log.e(TAG, "JSON error", e); return; }

            // Try atomic rename first (preferred — avoids partial reads by other processes).
            // This requires write permission on the directory, which only the daemon (shell UID)
            // has. If creating the tmp file fails (e.g. app process lacks directory write
            // permission), fall through to direct overwrite of the existing world-writable file.
            boolean saved = false;
            File tmpFile = new File(PREFS_FILE.getParentFile(), PREFS_FILE.getName() + ".tmp");
            try (FileWriter writer = new FileWriter(tmpFile)) {
                writer.write(content);
                tmpFile.setReadable(true, false);
                tmpFile.setWritable(true, false);
                if (tmpFile.renameTo(PREFS_FILE)) {
                    saved = true;
                } else {
                    tmpFile.delete();
                }
            } catch (Exception ignored) {
                // Cannot create tmp file (no directory write permission from app process) —
                // fall through to direct write below.
            }

            // Fallback: write directly to the existing file (world-writable, set by daemon).
            if (!saved) {
                try (FileWriter writer = new FileWriter(PREFS_FILE)) {
                    writer.write(content);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to save prefs file", e);
                    return;
                }
                PREFS_FILE.setReadable(true, false);
                PREFS_FILE.setWritable(true, false);
            }

            cache = json;
            lastModified.set(PREFS_FILE.lastModified());
        }
    }
}
