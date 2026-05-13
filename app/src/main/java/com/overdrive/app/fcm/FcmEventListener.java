package com.overdrive.app.fcm;

import android.util.Log;

import com.overdrive.app.telegram.event.CriticalEvent;
import com.overdrive.app.telegram.event.ITelegramEventBus;
import com.overdrive.app.telegram.event.MotionEvent;
import com.overdrive.app.telegram.event.SystemEvent;
import com.overdrive.app.telegram.event.TunnelEvent;
import com.overdrive.app.telegram.event.VideoEvent;

/**
 * Listens to TelegramEventBus and forwards events to FcmSender.
 *
 * This is the only hook between the event system and FCM.
 * TelegramNotifier.java is NOT modified — it publishes to the bus as normal,
 * and this listener picks up those events independently.
 *
 * Register in OverdriveApplication:
 *   TelegramEventBus.getInstance().subscribe(new FcmEventListener());
 *
 * Note: Proximity alerts bypass TelegramEventBus entirely (they go via
 * TelegramNotifier.sendMessage() IPC directly). Those are handled by a
 * direct FcmSender call in ProximityRecordingHandler.java.
 */
public class FcmEventListener implements ITelegramEventBus.EventListener {

    private static final String TAG = "FcmEventListener";

    @Override
    public void onEvent(SystemEvent event) {
        FcmPreferences prefs = FcmPreferences.getInstance();

        if (!prefs.isMasterEnabled()) return;
        if (!FcmTokenStore.getInstance().hasToken()) return;

        try {
            switch (event.getType()) {
                case MOTION:
                    if (prefs.isMotionEnabled()) {
                        MotionEvent me = (MotionEvent) event;
                        FcmSender.notifyMotion(me.getAiDetection(), me.getConfidence(), me.getVideoFilename());
                    }
                    break;

                case VIDEO:
                    if (prefs.isVideoEnabled()) {
                        VideoEvent ve = (VideoEvent) event;
                        FcmSender.notifyVideoReady(ve.getFilePath(), ve.getAiDetection(), ve.getDurationSeconds());
                    }
                    break;

                case CRITICAL:
                    if (prefs.isCriticalEnabled()) {
                        CriticalEvent ce = (CriticalEvent) event;
                        FcmSender.notifyCritical(ce.getCriticalType().name(), ce.getDetails());
                    }
                    break;

                case TUNNEL:
                    if (prefs.isTunnelEnabled()) {
                        TunnelEvent te = (TunnelEvent) event;
                        FcmSender.notifyTunnelUrl(te.getUrl(), te.isNew());
                    }
                    break;

                case CONNECTIVITY:
                    // Not surfaced via FCM — connectivity changes are low-value
                    // for push notifications and handled by tunnel events instead.
                    break;

                default:
                    Log.w(TAG, "Unknown event type: " + event.getType());
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling event: " + e.getMessage());
        }
    }
}
