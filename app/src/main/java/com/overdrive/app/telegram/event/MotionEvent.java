package com.overdrive.app.telegram.event;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Event emitted when motion is detected.
 */
public class MotionEvent extends SystemEvent {
    @Nullable private final String aiDetection;  // e.g., "person", "car", null for generic motion
    private final float confidence;
    @Nullable private final String videoFilename; // e.g., "event_20260113_143022.mp4", null if not yet known
    private final Map<String, Integer> detectionsByType; // best-per-type: {"person":87,"car":65}

    public MotionEvent(@Nullable String aiDetection, float confidence) {
        this(aiDetection, confidence, null);
    }

    public MotionEvent(@Nullable String aiDetection, float confidence, @Nullable String videoFilename) {
        this(aiDetection, confidence, videoFilename, null);
    }

    public MotionEvent(@Nullable String aiDetection, float confidence,
                       @Nullable String videoFilename,
                       @Nullable Map<String, Integer> detectionsByType) {
        super(EventType.MOTION);
        this.aiDetection = aiDetection;
        this.confidence = confidence;
        this.videoFilename = videoFilename;
        this.detectionsByType = detectionsByType != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(detectionsByType))
                : Collections.emptyMap();
    }

    @Nullable public String getAiDetection() { return aiDetection; }
    public float getConfidence() { return confidence; }
    @Nullable public String getVideoFilename() { return videoFilename; }
    public Map<String, Integer> getDetectionsByType() { return detectionsByType; }
    
    @Override
    public String getMessage() {
        if (!detectionsByType.isEmpty()) {
            StringBuilder sb = new StringBuilder("🚨 ");
            boolean first = true;
            for (String type : new String[]{"person", "car", "bike"}) {
                Integer conf = detectionsByType.get(type);
                if (conf != null) {
                    if (!first) sb.append(", ");
                    sb.append(capitalize(type)).append(" (").append(conf).append("%)");
                    first = false;
                }
            }
            sb.append(" detected");
            return sb.toString();
        } else if (aiDetection != null) {
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
