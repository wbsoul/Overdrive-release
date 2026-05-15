package com.overdrive.app.fcm;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Sends FCM push notifications to the Companion app via the FCM V1 HTTP API.
 *
 * Authentication uses OAuth2 with a service account private key (RS256 JWT).
 * Access tokens are cached and refreshed automatically (tokens are valid for 1 hour).
 *
 * The service account JSON is bundled in the app at:
 *   app/src/main/assets/fcm_service_account.json
 *
 * Obtain it from:
 *   Firebase Console → Project Settings → Service Accounts → Generate new private key
 *
 * Note: google-services.json is the client-side config for apps that RECEIVE FCM.
 * The service account JSON is the server-side credential needed to SEND via the V1 API.
 *
 * Initialise once from OverdriveApplication: FcmSender.init(context)
 */
public class FcmSender {

    private static final String TAG = "FcmSender";

    private static final String FCM_V1_URL = "https://fcm.googleapis.com/v1/projects/%s/messages:send";
    private static final String OAUTH_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String ASSET_NAME = "fcm_service_account.json";
    private static final long TOKEN_REFRESH_MARGIN_MS = 5 * 60 * 1000L; // refresh 5 min before expiry

    private static volatile Context appContext;

    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    // Single-threaded: all sends and token refreshes run sequentially — no lock needed for cache.
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "FcmSender");
        t.setDaemon(true);
        return t;
    });

    // Access token cache (only accessed from EXECUTOR thread)
    private static String cachedAccessToken = null;
    private static long tokenExpiryMs = 0L;

    private FcmSender() {}
    public static void init(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
            if (appContext == null) appContext = context;
        }
        // If context is null (daemon process with no Android context),
        // appContext stays null and loadServiceAccount() will fall back to filesystem.
    }
    // -------------------------------------------------------------------------
    // Public send methods
    // -------------------------------------------------------------------------

    public static void notifyMotion(String aiDetection, float confidence) {
        notifyMotion(aiDetection, confidence, null);
    }

    public static void notifyMotion(String aiDetection, float confidence, String videoFilename) {
        String title = "Motion Detected";
        String body = (aiDetection != null && !aiDetection.isEmpty())
                ? aiDetection.substring(0, 1).toUpperCase() + aiDetection.substring(1)
                  + " detected (" + Math.round(confidence * 100) + "%)"
                : "Motion detected";
        sendAsync(title, body, "motion", videoFilename != null ? videoFilename : null);
    }

    public static void notifyVideoReady(String filePath, String aiDetection, int durationSeconds) {
        String title = "Recording Saved";
        StringBuilder body = new StringBuilder(durationSeconds + "s clip");
        if (aiDetection != null && !aiDetection.isEmpty()) {
            body.append(" — ").append(aiDetection).append(" detected");
        }
        sendAsync(title, body.toString(), "video", filePath);
    }

    public static void notifyCritical(String criticalType, String details) {
        sendAsync("Critical Alert", criticalType + ": " + details, "critical");
    }

    public static void notifyTunnelUrl(String url, boolean isNew) {
        sendAsync(isNew ? "Tunnel Connected" : "Tunnel URL Changed", url, "tunnel");
    }

    public static void notifyProximity(String triggerLevel, String timestamp) {
        String distance = "RED".equals(triggerLevel) ? "0–0.5 m" : "0–0.8 m";
        sendAsync("Proximity Alert", triggerLevel + " — " + distance + " at " + timestamp, "proximity");
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private static void sendAsync(String title, String body, String eventType) {
        sendAsync(title, body, eventType, null);
    }

    private static void sendAsync(String title, String body, String eventType, String filePath) {
        EXECUTOR.execute(() -> {
            try {
                String deviceToken = FcmTokenStore.getInstance().getToken();
                if (deviceToken == null || deviceToken.isEmpty()) {
                    Log.w(TAG, "No Companion FCM token registered, skipping send");
                    return;
                }

                ServiceAccount sa = loadServiceAccount();
                if (sa == null) {
                    Log.w(TAG, "Service account not found in assets/" + ASSET_NAME + ", skipping send");
                    return;
                }

                String accessToken = getAccessToken(sa);
                if (accessToken == null) {
                    Log.w(TAG, "Failed to obtain OAuth2 access token, skipping send");
                    return;
                }
                // FCM V1 message payload
                JSONObject notification = new JSONObject();
                notification.put("title", title);
                notification.put("body", body);

                JSONObject data = new JSONObject();
                data.put("event_type", eventType);
                if (filePath != null && !filePath.isEmpty()) {
                    data.put("action", "play_video");
                    String fileName = new java.io.File(filePath).getName();
                    data.put("file_name", fileName);
                    // Provide the full streaming URL so the companion app can play
                    // the clip directly via the remote access tunnel.
                    String tunnelUrl = readTunnelUrl();
                    if (tunnelUrl != null) {
                        String thumbUrl = tunnelUrl + "/thumb/" + fileName;
                        data.put("video_url", tunnelUrl + "/events.html?play=" + fileName);
                        // Include direct thumbnail URL — uses detection-frame sidecar if available,
                        // falls back to lazy video frame extraction via the /thumb/ endpoint.
                        data.put("thumbnail_url", thumbUrl);
                        // FCM v1: image field causes FCM to download + display the thumbnail
                        // as a BigPictureStyle notification — no companion app code required.
                        notification.put("image", thumbUrl);
                    }
                } else {
                    data.put("action", "open_events");
                }

                JSONObject message = new JSONObject();
                message.put("token", deviceToken);
                message.put("notification", notification);
                message.put("data", data);

                JSONObject payload = new JSONObject();
                payload.put("message", message);

                String url = String.format(FCM_V1_URL, sa.projectId);
                RequestBody requestBody = RequestBody.create(payload.toString(), JSON_MEDIA);
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody)
                        .build();

                try (Response response = HTTP_CLIENT.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        Log.w(TAG, "FCM V1 send failed [" + eventType + "]: HTTP " + response.code());
                    } else {
                        Log.d(TAG, "FCM V1 sent [" + eventType + "]: " + title);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "FCM send error [" + eventType + "]: " + e.getMessage());
            }
        });
    }

    /**
     * Returns a valid access token, refreshing it if it is expired or close to expiry.
     * Called only from the single-threaded EXECUTOR — no synchronization needed.
     */
    private static String getAccessToken(ServiceAccount sa) {
        long now = System.currentTimeMillis();
        if (cachedAccessToken != null && now < tokenExpiryMs - TOKEN_REFRESH_MARGIN_MS) {
            return cachedAccessToken;
        }
        try {
            String jwt = createJwt(sa);
            String newToken = exchangeJwtForToken(jwt);
            if (newToken != null) {
                cachedAccessToken = newToken;
                tokenExpiryMs = now + 3600_000L; // tokens are valid for 1 hour
            }
            return newToken;
        } catch (Exception e) {
            Log.e(TAG, "Failed to refresh access token: " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a signed RS256 JWT for the service account OAuth2 flow.
     */
    private static String createJwt(ServiceAccount sa) throws Exception {
        long nowSec = System.currentTimeMillis() / 1000L;

        JSONObject header = new JSONObject();
        header.put("alg", "RS256");
        header.put("typ", "JWT");
        String encodedHeader = base64UrlEncode(header.toString().getBytes(StandardCharsets.UTF_8));

        JSONObject claims = new JSONObject();
        claims.put("iss", sa.clientEmail);
        claims.put("scope", FCM_SCOPE);
        claims.put("aud", OAUTH_TOKEN_URL);
        claims.put("iat", nowSec);
        claims.put("exp", nowSec + 3600L);
        String encodedClaims = base64UrlEncode(claims.toString().getBytes(StandardCharsets.UTF_8));

        String signingInput = encodedHeader + "." + encodedClaims;

        PrivateKey privateKey = loadPrivateKey(sa.privateKeyPem);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
        String encodedSignature = base64UrlEncode(sig.sign());

        return signingInput + "." + encodedSignature;
    }

    /**
     * Exchanges a signed JWT for a Google OAuth2 access token.
     */
    private static String exchangeJwtForToken(String jwt) throws IOException {
        RequestBody form = new FormBody.Builder()
                .add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                .add("assertion", jwt)
                .build();

        Request request = new Request.Builder()
                .url(OAUTH_TOKEN_URL)
                .post(form)
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                Log.w(TAG, "Token exchange failed: HTTP " + response.code());
                return null;
            }
            JSONObject json = new JSONObject(response.body().string());
            return json.optString("access_token", null);
        } catch (Exception e) {
            Log.e(TAG, "Token exchange error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses a PKCS#8 PEM private key (as stored in service account JSON).
     */
    private static PrivateKey loadPrivateKey(String pem) throws Exception {
        String stripped = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.decode(stripped, Base64.DEFAULT);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private static final java.io.File SERVICE_ACCOUNT_FILE =
            new java.io.File("/data/local/tmp/fcm_service_account.json");

    private static ServiceAccount loadServiceAccount() {
        // Prefer assets (app process); fall back to extracted filesystem copy (daemon process).
        InputStream is = null;
        try {
            if (appContext != null) {
                is = appContext.getAssets().open(ASSET_NAME);
            } else if (SERVICE_ACCOUNT_FILE.exists()) {
                is = new java.io.FileInputStream(SERVICE_ACCOUNT_FILE);
            } else {
                Log.e(TAG, "Service account not available (no context, no file at " + SERVICE_ACCOUNT_FILE + ")");
                return null;
            }
            byte[] data = new byte[is.available()];
            //noinspection ResultOfMethodCallIgnored
            is.read(data);
            JSONObject json = new JSONObject(new String(data, StandardCharsets.UTF_8));
            ServiceAccount sa = new ServiceAccount();
            sa.projectId = json.getString("project_id");
            sa.clientEmail = json.getString("client_email");
            sa.privateKeyPem = json.getString("private_key");
            return sa;
        } catch (Exception e) {
            Log.e(TAG, "Failed to read service account: " + e.getMessage());
            return null;
        } finally {
            if (is != null) try { is.close(); } catch (Exception ignored) {}
        }
    }

    private static String readTunnelUrl() {
        try {
            java.io.File f = new java.io.File(com.overdrive.app.daemon.proxy.Enc.TELEGRAM_URL_FILE);
            if (!f.exists()) return null;
            java.util.Scanner scanner = new java.util.Scanner(f);
            String url = scanner.hasNextLine() ? scanner.nextLine().trim() : null;
            scanner.close();
            return (url != null && !url.isEmpty()) ? url : null;
        } catch (Exception e) {
            Log.w(TAG, "Could not read tunnel URL: " + e.getMessage());
            return null;
        }
    }

    private static String base64UrlEncode(byte[] data) {
        return Base64.encodeToString(data, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    private static class ServiceAccount {
        String projectId;
        String clientEmail;
        String privateKeyPem;
    }
}
