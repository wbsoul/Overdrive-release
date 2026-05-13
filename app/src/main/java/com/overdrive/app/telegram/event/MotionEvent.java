package com.overdrive.app.telegram.event;

import androidx.annotation.Nullable;

/**
 * Event emitted when motion is detected.
 */
public class MotionEvent extends SystemEvent {
    @Nullable private final String aiDetection;  // e.g., "person", "car", null for generic motion
    private final float confidence;
    @Nullable private final String videoFilename; // e.g., "event_20260113_143022.mp4", null if not yet known

    public MotionEvent(@Nullable String aiDetection, float confidence) {
        this(aiDetection, confidence, null);
    }

    public MotionEvent(@Nullable String aiDetection, float confidence, @Nullable String videoFilename) {
        super(EventType.MOTION);
        this.aiDetection = aiDetection;
        this.confidence = confidence;
        this.videoFilename = videoFilename;
    }

    @Nullable public String getAiDetection() { return aiDetection; }
    public float getConfidence() { return confidence; }
    @Nullable public String getVideoFilename() { return videoFilename; }
    
    @Override
    public String getMessage() {
        if (aiDetection != null) {
            return "🚨 " + capitalize(aiDetection) + " detected (" + Math.round(confidence * 100) + "%)";
        } else {
            return "👁 Motion detected";
        }
    }
    
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
