package com.overdrive.app.ui.model

import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A single AI detection label and confidence from the event sidecar.
 */
data class AiDetection(val type: String, val confPct: Int)

/**
 * Represents a recorded video file.
 * SOTA: Supports both direct file access and MediaStore content URIs.
 */
data class RecordingFile(
    val file: File,
    val cameraId: Int,
    val timestamp: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val type: RecordingType,
    val contentUri: Uri? = null,  // SOTA: MediaStore content URI for cross-UID access
    val aiDetections: List<AiDetection> = emptyList()
) {
    // Secondary constructor for MediaStore results
    constructor(
        file: File,
        name: String,
        sizeBytes: Long,
        timestamp: Long,
        durationMs: Long,
        type: RecordingType,
        contentUri: Uri?
    ) : this(
        file = file,
        cameraId = extractCameraId(name),
        timestamp = timestamp,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        type = type,
        contentUri = contentUri
    )
    
    val name: String get() = file.name
    val path: String get() = file.absolutePath
    
    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    
    val formattedDuration: String
        get() {
            val seconds = durationMs / 1000
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, secs)
            } else {
                String.format("%d:%02d", minutes, secs)
            }
        }
    
    val formattedSize: String
        get() = when {
            sizeBytes >= 1_000_000_000 -> String.format("%.1f GB", sizeBytes / 1_000_000_000.0)
            sizeBytes >= 1_000_000 -> String.format("%.1f MB", sizeBytes / 1_000_000.0)
            sizeBytes >= 1_000 -> String.format("%.1f KB", sizeBytes / 1_000.0)
            else -> "$sizeBytes B"
        }
    
    enum class RecordingType {
        NORMAL,     // Regular recordings (cam_*.mp4)
        SENTRY,     // Sentry event recordings (event_*.mp4)
        PROXIMITY   // Proximity guard recordings (proximity_*.mp4)
    }
    
    companion object {
        // Parse filename like: cam1_20251224_132630.mp4 or cam_20251224_132630.mp4
        private val CAM_FILENAME_PATTERN = Regex("""cam(\d+)?_(\d{8})_(\d{6})\.mp4""")
        // Parse filename like: event_20251224_132630.mp4
        private val EVENT_FILENAME_PATTERN = Regex("""event_(\d{8})_(\d{6})\.mp4""")
        // Parse filename like: proximity_20251224_132630.mp4
        private val PROXIMITY_FILENAME_PATTERN = Regex("""proximity_(\d{8})_(\d{6})\.mp4""")
        private val DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        
        fun fromFile(file: File, type: RecordingType = RecordingType.NORMAL): RecordingFile? {
            // Try type-specific parsing first
            val result = when (type) {
                RecordingType.NORMAL -> parseNormalRecording(file)
                RecordingType.SENTRY -> parseSentryRecording(file)
                RecordingType.PROXIMITY -> parseProximityRecording(file)
            }
            
            // If type-specific parsing failed, try fallback parsing
            // This handles any .mp4 file that doesn't match expected patterns
            return result ?: parseFallbackRecording(file, type)
        }
        
        /**
         * Fallback parser for .mp4 files that don't match expected patterns.
         * Uses file modification time as timestamp.
         */
        private fun parseFallbackRecording(file: File, type: RecordingType): RecordingFile? {
            if (!file.name.endsWith(".mp4")) return null
            
            return RecordingFile(
                file = file,
                cameraId = 0,
                timestamp = file.lastModified(),
                durationMs = 0,
                sizeBytes = file.length(),
                type = type
            )
        }
        
        private fun parseNormalRecording(file: File): RecordingFile? {
            val match = CAM_FILENAME_PATTERN.matchEntire(file.name) ?: return null
            val cameraId = match.groupValues[1].toIntOrNull() ?: 0  // 0 for mosaic recordings
            val dateStr = "${match.groupValues[2]}_${match.groupValues[3]}"
            val timestamp = try {
                DATE_FORMAT.parse(dateStr)?.time ?: file.lastModified()
            } catch (e: Exception) {
                file.lastModified()
            }
            
            return RecordingFile(
                file = file,
                cameraId = cameraId,
                timestamp = timestamp,
                durationMs = 0, // Would need MediaMetadataRetriever to get actual duration
                sizeBytes = file.length(),
                type = RecordingType.NORMAL
            )
        }
        
        private fun parseSentryRecording(file: File): RecordingFile? {
            val match = EVENT_FILENAME_PATTERN.matchEntire(file.name) ?: return null
            val dateStr = "${match.groupValues[1]}_${match.groupValues[2]}"
            val timestamp = try {
                DATE_FORMAT.parse(dateStr)?.time ?: file.lastModified()
            } catch (e: Exception) {
                file.lastModified()
            }
            
            return RecordingFile(
                file = file,
                cameraId = 0,  // Sentry events are mosaic recordings
                timestamp = timestamp,
                durationMs = 0,
                sizeBytes = file.length(),
                type = RecordingType.SENTRY,
                aiDetections = readAiDetections(file)
            )
        }

        /**
         * Read AI detections from sidecar files alongside the MP4.
         * Tries .ai.json (written at trigger time) first, then the full .json timeline sidecar.
         * Returns detections in priority order: person, car, bike.
         *
         * Handles two .ai.json formats:
         *   New: {"detections":[{"type":"person","conf":87},{"type":"car","conf":65}]}
         *   Old: {"type":"person","conf":87}
         */
        private fun readAiDetections(file: File): List<AiDetection> {
            val base = file.name.removeSuffix(".mp4")
            val dir = file.parentFile ?: return emptyList()

            // --- Try .ai.json first ---
            val aiFile = File(dir, "$base.ai.json")
            if (aiFile.exists() && aiFile.length() > 0) {
                try {
                    val json = org.json.JSONObject(aiFile.readText())
                    // New format: {"detections":[...]}
                    val detsArr = json.optJSONArray("detections")
                    if (detsArr != null && detsArr.length() > 0) {
                        val result = mutableListOf<AiDetection>()
                        for (i in 0 until detsArr.length()) {
                            val d = detsArr.optJSONObject(i) ?: continue
                            val type = d.optString("type", "")
                            val conf = d.optInt("conf", 0)
                            if (type == "person" || type == "car" || type == "bike") {
                                result.add(AiDetection(type, conf))
                            }
                        }
                        if (result.isNotEmpty()) return result
                    }
                    // Old format: {"type":"person","conf":87}
                    val type = json.optString("type", "")
                    val conf = json.optInt("conf", 0)
                    if (type == "person" || type == "car" || type == "bike") {
                        return listOf(AiDetection(type, conf))
                    }
                } catch (_: Exception) {}
            }

            // --- Fall back to full .json timeline sidecar ---
            val sidecar = File(dir, "$base.json")
            if (!sidecar.exists() || sidecar.length() == 0L) return emptyList()
            return try {
                val root = org.json.JSONObject(sidecar.readText())
                val events = root.optJSONArray("events") ?: return emptyList()
                val maxConf = mutableMapOf<String, Int>()
                for (i in 0 until events.length()) {
                    val ev = events.optJSONObject(i) ?: continue
                    val t = ev.optString("type", "")
                    if (t != "person" && t != "car" && t != "bike") continue
                    val c = (ev.optDouble("maxConf", 0.0) * 100).toInt()
                    if (c > (maxConf[t] ?: 0)) maxConf[t] = c
                }
                // Fallback from stats block if events had no conf values
                val stats = root.optJSONObject("stats")
                if (stats != null) {
                    for (cls in listOf("person", "car", "bike")) {
                        if (stats.optInt(cls, 0) > 0 && !maxConf.containsKey(cls)) {
                            maxConf[cls] = 0
                        }
                    }
                }
                listOf("person", "car", "bike").mapNotNull { cls ->
                    maxConf[cls]?.let { AiDetection(cls, it) }
                }
            } catch (_: Exception) { emptyList() }
        }
        
        private fun parseProximityRecording(file: File): RecordingFile? {
            val match = PROXIMITY_FILENAME_PATTERN.matchEntire(file.name) ?: return null
            val dateStr = "${match.groupValues[1]}_${match.groupValues[2]}"
            val timestamp = try {
                DATE_FORMAT.parse(dateStr)?.time ?: file.lastModified()
            } catch (e: Exception) {
                file.lastModified()
            }
            
            return RecordingFile(
                file = file,
                cameraId = 0,  // Proximity events are mosaic recordings
                timestamp = timestamp,
                durationMs = 0,
                sizeBytes = file.length(),
                type = RecordingType.PROXIMITY
            )
        }
        
        /**
         * Extract camera ID from filename.
         */
        private fun extractCameraId(name: String): Int {
            val match = CAM_FILENAME_PATTERN.matchEntire(name)
            return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
        }
    }
}
