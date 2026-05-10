package com.overdrive.app.fcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Encrypted storage for the Companion app's FCM device token.
 *
 * Singleton — call init(Context) once from OverdriveApplication.onCreate(),
 * then use getInstance() everywhere else.
 */
public class FcmTokenStore {

    private static final String TAG = "FcmTokenStore";
    private static final String PREFS_NAME = "fcm_token_store";

    private static final String KEY_TOKEN = "fcm_token";
    private static final String KEY_CREATED_AT = "fcm_created_at";
    private static final String KEY_UPDATED_AT = "fcm_updated_at";

    private static volatile FcmTokenStore instance;

    private final SharedPreferences prefs;

    private FcmTokenStore(Context context) {
        this.prefs = createPrefs(context);
    }

    public static void init(Context context) {
        if (instance == null) {
            synchronized (FcmTokenStore.class) {
                if (instance == null) {
                    instance = new FcmTokenStore(context.getApplicationContext());
                }
            }
        }
    }

    public static FcmTokenStore getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FcmTokenStore.init(context) must be called first");
        }
        return instance;
    }

    private SharedPreferences createPrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.w(TAG, "Failed to create encrypted prefs, falling back to plain prefs", e);
            return context.getSharedPreferences(PREFS_NAME + "_fallback", Context.MODE_PRIVATE);
        } catch (Exception e) {
            // EncryptedSharedPreferences can throw RuntimeException (e.g. KeyStore not ready
            // on a freshly booted device before the user unlocks the screen).
            Log.w(TAG, "Unexpected error creating encrypted prefs, falling back to plain prefs", e);
            return context.getSharedPreferences(PREFS_NAME + "_fallback", Context.MODE_PRIVATE);
        }
    }

    /**
     * Store or overwrite the Companion FCM token.
     * Updates updatedAt every time; sets createdAt only on first registration.
     */
    public void upsertToken(String token) {
        long now = System.currentTimeMillis();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_TOKEN, token);
        editor.putLong(KEY_UPDATED_AT, now);
        if (!hasToken()) {
            editor.putLong(KEY_CREATED_AT, now);
        }
        editor.apply();
    }

    @Nullable
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public boolean hasToken() {
        String token = prefs.getString(KEY_TOKEN, null);
        return token != null && !token.isEmpty();
    }

    /** Epoch millis of first registration, or 0 if never registered. */
    public long getCreatedAt() {
        return prefs.getLong(KEY_CREATED_AT, 0L);
    }

    /** Epoch millis of last update, or 0 if never registered. */
    public long getUpdatedAt() {
        return prefs.getLong(KEY_UPDATED_AT, 0L);
    }

    /** Remove the stored token (e.g. user taps "Remove registered device"). */
    public void clear() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_CREATED_AT)
                .remove(KEY_UPDATED_AT)
                .apply();
    }
}
