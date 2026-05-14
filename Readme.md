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

## License

Open source under MIT License. Your data stays on your device.
