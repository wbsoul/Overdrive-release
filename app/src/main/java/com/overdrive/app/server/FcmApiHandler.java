package com.overdrive.app.server;

import android.util.Log;

import com.overdrive.app.fcm.FcmPreferences;
import com.overdrive.app.fcm.FcmTokenStore;

import org.json.JSONObject;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * FCM Companion registration API.
 *
 * Endpoints (all require JWT auth, handled by HttpServer before routing):
 *
 *   POST /api/fcm/register
 *     Body: { "token": "<fcm_device_token>" }
 *     Response: { "status": "ok", "updated": true }
 *
 *   GET /api/fcm/status
 *     Response: { "registered": true, "updatedAt": "2026-05-10 14:32:00" }
 *              or { "registered": false }
 *
 *   GET /api/fcm/prefs
 *     Response: { "master": true, "motion": true, "video": true,
 *                 "critical": true, "tunnel": true, "proximity": true }
 *
 *   POST /api/fcm/prefs
 *     Body: any subset of the prefs fields above
 *     Response: { "status": "ok" }
 */
public class FcmApiHandler {

    private static final String TAG = "FcmApiHandler";

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    public static boolean handle(String method, String path, String body, OutputStream out) throws Exception {
        try {
            return handleInternal(method, path, body, out);
        } catch (Exception e) {
            Log.e(TAG, "FCM handler error [" + method + " " + path + "]: " + e.getMessage(), e);
            throw e;
        }
    }

    private static boolean handleInternal(String method, String path, String body, OutputStream out) throws Exception {

        if (path.equals("/api/fcm/register") && method.equals("POST")) {
            handleRegister(body, out);
            return true;
        }

        if (path.equals("/api/fcm/status") && method.equals("GET")) {
            handleStatus(out);
            return true;
        }

        if (path.equals("/api/fcm/prefs") && method.equals("GET")) {
            handleGetPrefs(out);
            return true;
        }

        if (path.equals("/api/fcm/prefs") && method.equals("POST")) {
            handleSetPrefs(body, out);
            return true;
        }

        if (path.equals("/api/fcm/clear") && method.equals("POST")) {
            FcmTokenStore.getInstance().clear();
            JSONObject response = new JSONObject();
            response.put("status", "ok");
            HttpResponse.sendJson(out, response.toString());
            return true;
        }

        return false;
    }

    private static void handleRegister(String body, OutputStream out) throws Exception {
        if (body == null || body.isEmpty()) {
            HttpResponse.sendError(out, 400, "Request body is required");
            return;
        }

        JSONObject req;
        try {
            req = new JSONObject(body);
        } catch (Exception e) {
            HttpResponse.sendError(out, 400, "Invalid JSON body");
            return;
        }

        String token = req.optString("token", "").trim();
        if (token.isEmpty()) {
            HttpResponse.sendError(out, 400, "Missing required field: token");
            return;
        }

        FcmTokenStore.getInstance().upsertToken(token);

        JSONObject response = new JSONObject();
        response.put("status", "ok");
        response.put("updated", true);
        HttpResponse.sendJson(out, response.toString());
    }

    private static void handleStatus(OutputStream out) throws Exception {
        FcmTokenStore store = FcmTokenStore.getInstance();
        JSONObject response = new JSONObject();

        if (store.hasToken()) {
            long updatedAt = store.getUpdatedAt();
            String formattedDate = updatedAt > 0
                    ? DATE_FORMAT.format(new Date(updatedAt))
                    : "unknown";
            response.put("registered", true);
            response.put("updatedAt", formattedDate);
        } else {
            response.put("registered", false);
        }

        HttpResponse.sendJson(out, response.toString());
    }

    private static void handleGetPrefs(OutputStream out) throws Exception {
        FcmPreferences prefs = FcmPreferences.getInstance();
        JSONObject response = new JSONObject();
        response.put("master", prefs.isMasterEnabled());
        response.put("motion", prefs.isMotionEnabled());
        response.put("video", prefs.isVideoEnabled());
        response.put("critical", prefs.isCriticalEnabled());
        response.put("tunnel", prefs.isTunnelEnabled());
        response.put("proximity", prefs.isProximityEnabled());
        HttpResponse.sendJson(out, response.toString());
    }

    private static void handleSetPrefs(String body, OutputStream out) throws Exception {
        if (body == null || body.isEmpty()) {
            HttpResponse.sendError(out, 400, "Request body is required");
            return;
        }

        JSONObject req;
        try {
            req = new JSONObject(body);
        } catch (Exception e) {
            HttpResponse.sendError(out, 400, "Invalid JSON body");
            return;
        }

        FcmPreferences prefs = FcmPreferences.getInstance();
        if (req.has("master"))    prefs.setMasterEnabled(req.getBoolean("master"));
        if (req.has("motion"))    prefs.setMotionEnabled(req.getBoolean("motion"));
        if (req.has("video"))     prefs.setVideoEnabled(req.getBoolean("video"));
        if (req.has("critical"))  prefs.setCriticalEnabled(req.getBoolean("critical"));
        if (req.has("tunnel"))    prefs.setTunnelEnabled(req.getBoolean("tunnel"));
        if (req.has("proximity")) prefs.setProximityEnabled(req.getBoolean("proximity"));

        JSONObject response = new JSONObject();
        response.put("status", "ok");
        HttpResponse.sendJson(out, response.toString());
    }
}
