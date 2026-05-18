<p align="center">
  <img src="https://overdrive-5lc.pages.dev/app-icon.webp" width="120" alt="OverDrive Logo">
</p>

<h1 align="center">OverDrive</h1>
<p align="center">Advanced Sentry Mode for BYD Vehicles</p>
<p align="center">
  <a href="https://github.com/yash-srivastava/Overdrive-release/releases/tag/alpha">Download Alpha</a> •
  <a href="https://overdrive-5lc.pages.dev/">Website</a> •
  <a href="https://discord.gg/PZutk9fg4h">Discord</a> •
  <a href="#features">Features</a> •
  <a href="#quick-start-use-pre-built-apk">Setup Guide</a>
</p>

Free, open-source dashcam and sentry mode app built specifically for BYD vehicles with DiLink v3. All data stays on your device — no cloud, no accounts, no subscriptions.

---

> **⚠️ Experimental Fork — Proof of Concept**
>
> **This repository is a fork of [yash-srivastava/Overdrive-release](https://github.com/yash-srivastava/Overdrive-release), modified as an experimental proof of concept to explore companion-app integration with the [OverDrive Companion](https://github.com/wbsoul/OverDriveCompanion) project.**
>
> The changes introduced here — including FCM push notifications, deep-link navigation, and extended telemetry — are exploratory in nature and are not part of the official OverDrive release. They are intended to validate the technical feasibility of a companion application that bridges the on-vehicle OverDrive daemon with an external Android device, enabling remote monitoring and alerting without any third-party cloud dependency.
>
> **For the stable, production-ready release, please refer to the [upstream repository](https://github.com/yash-srivastava/Overdrive-release).**

---

<p align="center">
  <a href="https://player.cloudinary.com/embed/?cloud_name=dhwuuoz67&public_id=Demo_nqf0ky">
    <img src="https://github.com/user-attachments/assets/d5faeb2a-96dd-4737-86f4-2e87af52ec4c" alt="Click to Watch OverDrive Demo" width="100%">
  </a>
</p>

---


## Quick Start (Use Pre-built APK)

Download the latest APK from [GitHub Releases](https://github.com/yash-srivastava/Overdrive-release/releases/tag/alpha) and install it directly on your BYD head unit.

### 1. Prerequisites
- Ensure **Wireless ADB** is enabled on your device before launching the app.

### 2. Initial Configuration
1. **Authorize ADB:** On first launch, accept the ADB authentication prompt on your device screen.
2. **Background Persistence:** In Settings, ensure the **"Disable Autostart"** toggle is **unchecked**. This is critical for reliable background operation.

> ⚠️ **CRITICAL: Hard Reboot Required**
> After the first installation and initial run, you must hard reboot the device:
> Press and hold the **Volume Down** button for 5 seconds. Wait for the system to fully restart.
> This step is necessary to finalize the installation.

### 3. Network & Tunnel Setup

**Option A: Dedicated Wi-Fi Hotspot (Recommended)**
- Keep the Sing-box Proxy disabled.
- Directly enable your preferred tunnel (Zrok, Cloudflared or Tailscale).

**Option B: Public / BYD SIM**
- Toggle the **"Public"** switch at the top right of the dashboard.
- Go to Daemon View and verify the Sing-box Proxy Daemon is running.
- Once verified, enable your preferred tunnel (Zrok, Cloudflared or Tailscale).

### Telegram Notifications Setup
1. Message [@BotFather](https://t.me/BotFather) on Telegram → `/newbot` → follow prompts → get your bot token
2. Message [@userinfobot](https://t.me/userinfobot) → `/start` → copy your Chat ID
3. In OverDrive: Settings → Notifications → enter bot token & chat ID

---

## Why OverDrive?

| Feature | OverDrive | Other Apps |
|---|---|---|
| CPU Usage | **<28%** | 70–90% |
| Proximity Recording | ✅ Market First | ❌ |
| Real-time Performance Monitor | ✅ Built-in | ❌ |
| ISP Blocklist Bypass | ✅ Via BYD SIM | ❌ Requires WiFi Hotspot |
| Remote Access | 4 methods (LAN, Cloudflared, Zrok, Tailscale) | Usually 1 (if any) |
| ADB Shell Runner | ✅ | ❌ |
| Telegram Notifications | ✅ Free | Paid or None |
| Data Privacy | 100% On-Device | Often Cloud-Required |
| Price | **Free Forever** | $5–50/month |

## Features

- **Optimized Recording Pipeline** — <28% CPU, ~150MB memory, <3s boot time
- **Proximity Recording (Market First)** — Uses BYD's 8 parking radar sensors to trigger recording only when objects approach. Configurable trigger levels, pre-event buffer, and 500ms debouncing.
- **Advanced Sentry Mode** — 24/7 surveillance with motion detection and AI object recognition
- **Real-time Performance Monitor** — CPU, GPU, memory usage, and battery voltage dashboard
- **ISP Blocklist Bypass** — Browse via BYD's built-in SIM card without a dedicated hotspot
- **ADB Shell Runner** — Built-in terminal for running commands, checking processes, and viewing logs
- **Telegram Notifications** — Instant alerts for motion detection, recording events, and low battery
- **Recording Library** — Calendar view for browsing and managing recordings

## Remote Access

Three options for viewing your car's cameras remotely:

### Local Network (LAN)
Access at `http://<car-ip>:8080` when on the same WiFi. Zero setup, fastest streaming.

### Cloudflare Tunnel
Access from anywhere via `https://<random>.trycloudflare.com`. No port forwarding, HTTPS by default. Video streaming can be slow due to Cloudflare limitations.

### Zrok Tunnel (Recommended)
Free, open-source tunneling with no bandwidth limits at `https://<your-share>.share.zrok.io`. Best for video streaming.

**Quick Zrok setup:**
1. Sign up at [zrok.io](https://zrok.io)
2. Get your invite token from email
3. Enter token in OverDrive settings
4. Done — tunnel URL is auto-generated

### Tailscale Tunnel
Free, with no bandwidth limits. Connect from any device connected to tailscale.

**Quick Tailscale setup:**
1. Sign up at [tailscale.com](https://tailscale.com/)
2. Open tailscale settings in Overdrive
3. Generate a login URL and login
4. Optionally, disable key expiry in tailscale if you would not like to log in every 6 months

## Tech Specs

| Category | Detail |
|---|---|
| Resolution | Up to 2560×1920 |
| Codec | H.264 / H.265 (HEVC) |
| Bitrate | 2–12 Mbps (configurable) |
| FPS | 15–30 fps |
| CPU Usage | <28% (optimized) |
| Memory | ~150MB |
| Streaming Latency | <100ms |
| Boot Time | <3 seconds |
| AI Detection | Hardware accelerated, real-time (vehicles, people, objects) |
| Tested On | BYD Seal (Global) |
| Platform | DiLink v3 |
| Android | 10+ (API 29+) |
| Architecture | arm64-v8a |

> Should work on all BYD vehicles with DiLink v3 and panoramic camera system.

## Building from Source

```bash
git clone https://github.com/yash-srivastava/Overdrive-release.git
```

Set up signing by exporting these environment variables before building:

```bash
export KEYSTORE_FILE=/path/to/your/release.jks
export KEYSTORE_PASSWORD=your_password
export KEY_PASSWORD=your_key_password
export KEY_ALIAS=your_alias
```

Then build with Gradle:

```bash
./gradlew assembleRelease
```

## VLESS Proxy Setup (Optional)

The ISP blocklist bypass feature uses a VLESS Reality proxy. The app ships with placeholder credentials — you need to supply your own.

1. Edit `app/src/main/cpp/secrets/secrets.json` and fill in your VLESS server details:
   ```json
   "proxy": {
     "PROXY_SERVER_IP": "your.server.ip",
     "PROXY_SERVER_PORT": "443",
     "PROXY_UUID": "your-uuid-here",
     "PROXY_SHORT_ID": "your-short-id",
     "PROXY_PUBLIC_KEY": "your-public-key",
     "PROXY_SNI": "google.com"
   }
   ```

2. Encrypt each value using the helper script:
   ```bash
   pip install pycryptodome
   python3 generate_safe_enc.py "your.server.ip"
   ```

3. Replace the corresponding `Safe.s("...")` values in `app/src/main/java/com/overdrive/app/daemon/GlobalProxyDaemon.java` (lines 71–79).

4. Rebuild the app.

If you don't need the proxy feature, you can skip this — the app works fine without it.

## Zrok Token Setup (Optional)

If you want to use Zrok tunneling for remote access, you need your own Zrok invite token:

1. Sign up at [zrok.io](https://zrok.io) and get your invite token from email
2. Enter the token in the app: Daemons → Zrok settings
3. If building from source, also replace `YOUR_ZROK_TOKEN` in `app/src/main/java/com/overdrive/app/daemon/telegram/DaemonCommandHandler.java` with your token (this is only used for the Telegram bot's `/tunnel zrok` command)

## Privacy

- 100% local storage — all recordings saved on device
- No account required
- No cloud upload — remote viewing is direct via tunnels
- Open source — audit the code yourself

## Community

- [Discord Server](https://discord.gg/PZutk9fg4h)
- [Report Issues](https://github.com/yash-srivastava/Overdrive-release/issues)

## Acknowledgments

- **Native Bangcle Crypto Engine** — Full Java port of BYD's proprietary white-box AES encryption, based on the reverse engineering work by [Niek/BYD-re](https://github.com/Niek/BYD-re) and [jkaberg/pyBYD](https://github.com/jkaberg/pyBYD). Zero new dependencies — uses the existing OkHttp stack and Java crypto libraries.
- **3D BYD Vehicle Models** — Vehicle Control page uses base models from [ddiaz-design's BYD collection on Sketchfab](https://sketchfab.com/ddiaz-design/collections/byd-base-models-5bf92ab5f2be4ff6be5c3ac49f7099f3).

## Changelog

### POC 1.07 — May 2026: AI Detection Pipeline & FCM Notification Image Fix

**✨ Features**
- **AI Detection Zone Filtering** — YOLO detections are now filtered against the per-camera ROI (Detection Zone) polygon. A detection must at least partially intersect the configured zone to be considered — checked via 5-point bbox sampling (center + 4 corners) plus polygon-vertex-inside-bbox test. Ensures objects outside the zone of interest are suppressed even when motion blocks overlap
- **Best-Per-Type Detection Tracking** — The engine now tracks the highest-confidence detection per object type (person, car, bike) across all YOLO runs during a motion sequence, yielding up to 3 representative detections instead of just 1. The `.ai.json` sidecar format changes from `{"type":"person","conf":87}` to `{"detections":[{"type":"person","conf":87},{"type":"car","conf":65}]}` with full backward compatibility in all parsers
- **Hero Frame Selection** — The detection-frame thumbnail (`.thumb.jpg`) is now selected based on the highest-confidence detection across the entire motion sequence, rather than being overwritten on every YOLO run. This produces more meaningful thumbnails for event listings and FCM notifications
- **Multi-Detection Push Notifications** — FCM and Telegram notifications now include all detected object types instead of just one. Example: `Person (87%), Car (65%) detected`. The notification pipeline passes the full `bestDetectionPerType` map through `MotionEvent` → `FcmEventListener` → `FcmSender`, falling back to single-label format for motion-only triggers
- **Native Events Page — Storage Stats Bar** — Added a compact storage stats bar to the native Android events page showing total disk usage with a fill bar and per-type breakdown (Normal / Sentry / Proximity counts with colored indicators). Stats refresh on page load, resume, and after deletions
- **Native Events Page — Pagination** — Added Prev/Next pagination controls (12 items per page, matching the remote portal) to the native events list. Previously all recordings for a date loaded at once with no paging. Page resets on filter or date change

**🐛 Bug Fixes**
- **FCM Notification Image Not Showing (Companion App)** — `OdcMessagingService.onMessageReceived()` extracted `thumbnail_url` from the FCM data payload but never used it — the notification was built as text-only. Added `HttpURLConnection` image download with 10-second timeouts; on success the notification uses `NotificationCompat.BigPictureStyle` with the detection-frame thumbnail. Falls back gracefully to text-only if the download fails (tunnel down, etc.)
- **FCM Notification Image Not Showing (Background State)** — The FCM payload previously included a `notification` block alongside `data`. When the companion app was in the background, FCM auto-displayed the notification using the `notification` block but could not reliably fetch `notification.image` from a zrok tunnel URL at delivery time. Changed to a **data-only** payload with `android.priority = HIGH`. This ensures `onMessageReceived()` is always called regardless of app state, giving the companion app full control over image download and display
- **FCM OAuth2 400 — Missing Error Details** — `exchangeJwtForToken()` logged only the HTTP status code on failure, hiding Google's `error_description` field. Now logs the full response body (e.g. `{"error":"invalid_grant","error_description":"Invalid JWT Signature."}`) to aid diagnosis of service account key issues
- **FCM OAuth2 — InputStream Truncation** — `loadServiceAccount()` used `InputStream.available()` to allocate the read buffer, which is only an estimate and can truncate the service account JSON for large `AssetManager` streams, silently corrupting the RSA private key. Fixed to read all bytes via a loop into a `ByteArrayOutputStream`
- **FCM Service Account — Load Source Logging** — Added `D/FcmSender` log on successful load showing source path, project ID, client email, and private key length to confirm which key file is in use

**🔍 Diagnostics**
- **Surveillance Settings WebView Debug Logging** — Added diagnostic logging across the surveillance config loading pipeline to investigate settings not populating on Android WebView: JS-side `loadConfig()` logs fetch status/body/keys, `WebViewFragment.shouldInterceptRequest` logs API request/response details, and `SurveillanceApiHandler.sendConfig()` logs config source and key fields

### POC 1.06 — May 2026: FCM Tunnel URL Detection Fix & Surveillance Settings Apply Button Fix

**🐛 Bug Fixes**
- **FCM Tunnel URL Detection (SELinux)** — `FcmSender.probeTunnelUrlFromLogs()` used `Runtime.getRuntime().exec()` to run `grep` in a shell subprocess. This fails silently in the daemon's SELinux execution context — no subprocess spawning is permitted — so `video_url` / `thumbnail_url` were dropped from FCM payloads even when the tunnel was running. Replaced with pure Java `BufferedReader` + `Pattern.matcher()` scanning of `cloudflared.log` and `zrok.log`. Added startup URL probe in `FcmSender.init()` to seed the cache when the daemon starts
- **Zrok URL Pattern Missing Hyphens** — The regex used to extract the zrok tunnel URL (`[a-z0-9]+\.share\.zrok\.io`) did not allow hyphens in the subdomain. Fixed to `[a-z0-9-]+` in both `FcmSender.java` and `DaemonCommandHandler.java`
- **FCM Service Account File — EACCES After Reinstall** — `HttpServer` called `setWritable(false)` after writing `fcm_service_account.json`, permanently blocking re-extraction on subsequent restarts. On reinstall the file's owner UID changes, making `setWritable(true)` a no-op and `FileOutputStream` throw EACCES. Fixed: delete the file before write; removed `setWritable(false)`
- **Sentry Sidecar Files Not World-Readable** — `.thumb.jpg` and `.ai.json` sidecars written by `SurveillanceEngineGpu` were not marked world-readable. The companion app (different UID) could not read them. Fixed with `setReadable(true, false)`
- **RecordingAdapter Stale Thumbnail Cache** — Sentry recordings loaded a generic video frame (from `MediaMetadataRetriever`) and cached it before the `.thumb.jpg` sidecar became readable. Subsequent list scrolls returned the stale frame. Fixed with `sidecarConfirmedPaths` / `sidecarTriedPaths` sets — the cache is bypassed once per recording until the sidecar state is confirmed
- **Material Slider IllegalStateException** — `SentryConfigFragment` set the surveillance storage limit directly on the Material Slider without snapping to the step size, triggering an `IllegalStateException`. Fixed: limit is rounded to the nearest 100 MB before being applied
- **Surveillance Settings Apply Button Pre-Activated** — `btnApply` had no `disabled` attribute in the HTML, so it rendered enabled the moment the page displayed — before `init()` completed its async API calls (1–2 s). Users saw "Apply Changes" as active immediately on page load. Fixed: button starts `disabled`; `updateUI()` now also clears the `has-changes` CSS class and uses null-safe element access
- **Surveillance Config `lastModified` Timestamp** — `GET /api/surveillance/config` returned `lastModified: System.currentTimeMillis()` when no config file existed. This caused the 10-second `reloadConfig()` poll to always find a newer timestamp and re-run `updateUI()` on every tick when no config was saved. Fixed: returns `lastModified: 0` when the file is absent; subsequent polls compare `0 > 0` (false) and skip the reload
- **SentryConfigViewModel Error Logging** — `GET_CONFIG` returning `success=false` was silently swallowed. Added error log and `_error.postValue()` to surface the failure in the UI

---

### POC 1.05 — May 2026: Surveillance Settings Persistence Fix & FCM Tunnel URL Fix

**🐛 Bug Fixes**
- **Settings Not Loading on Restart** — `GET_CONFIG` response was missing `notifyIfNoObjectDetected`, `minConfidencePerson`, `minConfidenceCar`, `minConfidenceBike`, and `schedulingEnabled` — all four controls reset to defaults every app restart
- **scheduleEnabled Key Mismatch** — Daemon returned `"scheduleEnabled"` but the ViewModel read `"schedulingEnabled"`, so the schedule toggle was always off after restart
- **Per-Class Confidence Not Persisted** — `SET_CONFIG` in `applyConfig()` ignored `notifyIfNoObjectDetected` and per-class confidence fields — changes were applied in-memory but never written to the config file, lost on next restart
- **FCM Notifications Missing video_url / thumbnail_url** — `video_url`, `thumbnail_url`, and `notification.image` were silently dropped from the FCM payload when the tunnel URL file (`/data/local/tmp/tunnel_url.txt`) was absent. The file was only written when the tunnel was started via a Telegram bot command; if cloudflared/zrok was already running (survived a daemon restart or started another way) the file was never written. Fixed with a three-layer fallback: (1) read the file if present, (2) probe `cloudflared.log` / `zrok.log` directly and re-persist the URL, (3) use the last URL seen in-process since startup

---

### POC 1.04 — May 2026: FCM Notification Thumbnail Fix

**🐛 Bug Fixes**
- **FCM Thumbnail — Synchronous Generation** — The `/thumb/` endpoint previously returned `HTTP 202 Accepted` + JSON when a thumbnail was not yet cached. FCM's image downloader makes a single attempt; a non-200 or non-image response means the image is silently dropped. The endpoint now generates the JPEG synchronously and always returns a real image on first request
- **FCM Thumbnail — Android Platform Override** — Added `android.notification.image` to the FCM payload alongside the existing `notification.image`. The top-level field is the cross-platform FCM field; the Android-specific platform field is what actually triggers `BigPictureStyle` rendering on Android devices

---

### POC 1.03 — May 2026: Portal AI Badges Fix & FCM Thumbnail Auth

**🐛 Bug Fixes**
- **Portal AI Badges (No-Sidecar Fallback)** — `RecordingsApiHandler.parseRecording()` now reads `.ai.json` even when the `.json` sidecar does not exist yet. Previously, AI detection data was only loaded as a fallback if the `.json` sidecar was present — recordings that were still in-progress (sidecar not yet written at end) showed no badges in the portal
- **FCM Thumbnail Auth** — Added `/thumb/` to `AuthMiddleware.PUBLIC_PREFIXES` so FCM servers can fetch thumbnail images without a JWT. Previously FCM's one-shot image downloader received a 401 and silently dropped the image from the notification
- **Portal AI Badge Size** — Bumped `.ai-badge` CSS `font-size` from `10px` to `13px` for readable emoji rendering in the event listing

---

### POC 1.02 — May 2026: AI Badges Fix & Android GUI Events Parity

**🐛 Bug Fixes**
- **AI Badges Fix (hasActiveMotion Gate)** — Removed async `hasActiveMotion` gate that was silently dropping YOLO events from `.json` sidecars, causing AI badges to never appear in the portal event listing
- **AI Badges Fix (.ai.json Fallback)** — `RecordingsApiHandler` now falls back to the `.ai.json` sidecar when the `.json` events file exists but contains no AI detections

**✨ New Features**
- **Android GUI Events Parity** — Rewrote `RecordingAdapter.kt` to match the portal's event card layout: recording type badge (sentry/ACC/manual), AI detection badges (person/car/bike with confidence %), filename, date, time, and file size
  - Badge colours: person = red `#EF4444`, car = blue `#3B82F6`, bike = orange `#F97316`, each with 15% alpha background and 6dp rounded corners
  - Badge format: emoji + narrow-no-break-space + confidence% (e.g. `🚶 87%`)
- **FCM Notification Thumbnail** — Added `notification.image` field to FCM V1 payload so FCM servers fetch and attach the `/thumb/` JPEG as a `BigPictureStyle` notification image — no companion app code required

---

### POC 1.01 — May 2026: YOLO Thumbnails & AI Badges in Event Listing

**✨ New Features**
- **Detection-Frame Thumbnails** — `SurveillanceEngineGpu` now saves the actual YOLO detection frame (with bounding box overlay) as a `.thumb.jpg` sidecar alongside each MP4 at recording start. The `/thumb/` endpoint serves this sidecar when available, falling back to `MediaMetadataRetriever` frame extraction
- **AI Detection Badges in Portal** — The event listing in `events.html` shows coloured emoji badges for detected objects (person 🚶, car 🚗, bike 🚲) with confidence percentages, read from the `.ai.json` sidecar written by `YoloDetector`

---

### POC 1.0 — May 2026: Companion App Integration, YOLO26n & AI Surveillance Settings

**✨ New Features**
- **YOLO26n AI Model** — Upgraded AI object detection to YOLO26n for improved accuracy and reduced false positives
- **Per-Class Confidence Thresholds** — New sliders in Surveillance Settings to set independent minimum confidence levels (0–100%) for Person, Car, and Bike detection. Tune each class separately to balance sensitivity vs. false alarms
- **Notify-If-No-Object Toggle** — Option to suppress alerts when motion is detected but no AI-recognised object (person/car/bike) is present in the frame
- **FCM Push Notifications** — Firebase Cloud Messaging integration to deliver real-time sentry event alerts to the [OverDrive Companion](https://github.com/wbsoul/OverDriveCompanion) Android app
- **FCM Deep-Link Navigation** — Tapping a notification deep-links directly to the relevant event or page in the companion app
- **Web Portal Notifications Page** — Dedicated page in the remote portal to review recent sentry events and notification history

**⚡ Optimizations & Fixes**
- **Settings Persistence Fix** — Resolved issue where surveillance configuration changes were not persisting across daemon restarts
- **Slider Change Detection Fix** — Confidence threshold sliders now correctly trigger the unsaved-changes indicator via `oninput` (fixes Save button not activating in older Android WebViews)
- **Mobile UI Polish** — Fixed burger menu not appearing on the notifications page; centred page titles in the desktop header
- **System Daemon Rename** — Renamed `CameraDaemon` → `SystemDaemon` throughout the codebase and all UI labels
- **OTA Channel Migration** — Auto-detects when the embedded update channel no longer exists (e.g. upgrading from a pre-POC `alpha` build) and falls back to the active `poc` channel, ensuring update prompts are never silently missed
- **Portal Mobile Overflow Fix** — Resolved horizontal scroll/crop on narrow mobile screens; all panels now fit within the viewport width without horizontal scrolling
- **Confidence Slider UI** — Restyled the per-class confidence sliders in the portal to match the existing Loitering Time slider (teal thumb, monospace value label, consistent sizing)

## License

This project is a fork of [yash-srivastava/Overdrive-release](https://github.com/yash-srivastava/Overdrive-release), which is licensed under the [MIT License](LICENSE).

The modifications and additions in this fork — including YOLO26n AI integration, FCM push notifications, companion app features, and all associated code — are licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)**, in compliance with the [Ultralytics YOLO](https://github.com/ultralytics/ultralytics) dependency which is itself AGPL-3.0 licensed.

Under AGPL-3.0, any modifications you distribute or deploy as a network service must also be made available under AGPL-3.0. See the [AGPL-3.0 license text](https://www.gnu.org/licenses/agpl-3.0.html) for full terms.

Your data stays on your device.
