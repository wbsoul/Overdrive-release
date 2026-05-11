package com.overdrive.app.fcm;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Storage for the Companion app's FCM device token.
 *
 * Follows the same pattern as UnifiedConfigManager:
 * - Single JSON file at /data/local/tmp/fcm_token_store.json
 * - World-readable + world-writable (accessible by both shell UID 2000 and app UID)
 * - Atomic writes via tmp-file rename
 * - In-memory cache to avoid repeated disk reads
 * - No Context or SharedPreferences dependency
 *
 * Token file: /data/local/tmp/fcm_token_store.json
 */
public class FcmTokenStore {

    private static final String TAG = "FcmTokenStore";
    private static final File TOKEN_FILE = new File("/data/local/tmp/fcm_token_store.json");

    private static final String KEY_TOKEN = "fcm_token";
    private static final String KEY_INSTALLATION_ID = "fcm_installation_id";
    private static final String KEY_CREATED_AT = "fcm_created_at";
    private static final String KEY_UPDATED_AT = "fcm_updated_at";

    private static volatile FcmTokenStore instance;

    @Nullable private volatile JSONObject cache;
    private final AtomicLong lastModified = new AtomicLong(0);

    private FcmTokenStore() {}

    /** Call once before use. Context parameter kept for API compatibility but unused. */
    public static void init(Context context) {
        if (instance == null) {
            synchronized (FcmTokenStore.class) {
                if (instance == null) {
                    instance = new FcmTokenStore();
                    Log.i(TAG, "Initialized — token file: " + TOKEN_FILE);
                }
            }
        }
    }

    public static FcmTokenStore getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FcmTokenStore.init() must be called first");
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Store or overwrite the Companion FCM token.
     * Updates updatedAt every time; sets createdAt only on first registration.
     */
    public void upsertToken(String token) {
        upsertRegistration(token, null);
    }

    /**
     * Store or overwrite the Companion FCM token and optional Firebase Installation ID.
     * Updates updatedAt every time; sets createdAt only on first registration.
     */
    public void upsertRegistration(String token, @Nullable String installationId) {
        long now = System.currentTimeMillis();
        JSONObject json = load();
        try {
            boolean isFirst = json.optString(KEY_TOKEN, "").isEmpty();
            json.put(KEY_TOKEN, token);
            json.put(KEY_UPDATED_AT, now);
            if (isFirst) json.put(KEY_CREATED_AT, now);
            if (installationId != null && !installationId.isEmpty()) {
                json.put(KEY_INSTALLATION_ID, installationId);
            }
        } catch (Exception e) {
            Log.e(TAG, "JSON error in upsertRegistration", e);
        }
        save(json);
    }

    @Nullable
    public String getToken() {
        String val = load().optString(KEY_TOKEN, "");
        return val.isEmpty() ? null : val;
    }

    public boolean hasToken() {
        return getToken() != null;
    }

    /** Epoch millis of first registration, or 0 if never registered. */
    public long getCreatedAt() {
        return load().optLong(KEY_CREATED_AT, 0L);
    }

    /** Epoch millis of last update, or 0 if never registered. */
    public long getUpdatedAt() {
        return load().optLong(KEY_UPDATED_AT, 0L);
    }

    /** Firebase Installation ID, or null if not provided at registration. */
    @Nullable
    public String getInstallationId() {
        String val = load().optString(KEY_INSTALLATION_ID, "");
        return val.isEmpty() ? null : val;
    }

    /** Remove the stored token (e.g. user taps "Remove registered device"). */
    public void clear() {
        save(new JSONObject());
    }

    // -------------------------------------------------------------------------
    // File I/O — same pattern as UnifiedConfigManager
    // -------------------------------------------------------------------------

    private JSONObject load() {
        // Return cache if file hasn't changed
        if (cache != null && TOKEN_FILE.exists()) {
            long fileModified = TOKEN_FILE.lastModified();
            if (fileModified <= lastModified.get()) {
                return cache;
            }
        }
        synchronized (this) {
            try {
                if (TOKEN_FILE.exists()) {
                    String content = new String(java.nio.file.Files.readAllBytes(TOKEN_FILE.toPath()));
                    JSONObject json = new JSONObject(content);
                    cache = json;
                    lastModified.set(TOKEN_FILE.lastModified());
                    return json;
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to load token file, returning empty", e);
            }
            return new JSONObject();
        }
    }

    private void save(JSONObject json) {
        synchronized (this) {
            try {
                File tmpFile = new File(TOKEN_FILE.getParentFile(), TOKEN_FILE.getName() + ".tmp");
                try (FileWriter writer = new FileWriter(tmpFile)) {
                    writer.write(json.toString(2));
                }
                // World-readable + world-writable: shared between shell UID 2000 and app UID
                tmpFile.setReadable(true, false);
                tmpFile.setWritable(true, false);
                if (!tmpFile.renameTo(TOKEN_FILE)) {
                    // Fallback: direct write if rename fails
                    try (FileWriter writer = new FileWriter(TOKEN_FILE)) {
                        writer.write(json.toString(2));
                    }
                    TOKEN_FILE.setReadable(true, false);
                    TOKEN_FILE.setWritable(true, false);
                    tmpFile.delete();
                }
                cache = json;
                lastModified.set(TOKEN_FILE.lastModified());
            } catch (Exception e) {
                Log.e(TAG, "Failed to save token file", e);
            }
        }
    }
}
