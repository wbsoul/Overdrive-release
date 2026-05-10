package com.overdrive.app

import android.app.Application
import com.overdrive.app.fcm.FcmEventListener
import com.overdrive.app.fcm.FcmPreferences
import com.overdrive.app.fcm.FcmSender
import com.overdrive.app.fcm.FcmTokenStore
import com.overdrive.app.logging.LogConfig
import com.overdrive.app.logging.LogManager
import com.overdrive.app.services.DaemonKeepaliveService
import com.overdrive.app.telegram.event.TelegramEventBus
import com.overdrive.app.ui.util.PreferencesManager

/**
 * Application class for Overdrive.
 * Initializes global singletons before any Activity is created.
 */
class OverdriveApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize LogConfig with app's cache directory for file logging
        LogConfig.init(this)
        
        // Initialize LogManager with file logging enabled
        LogManager.getInstance(LogConfig.default())
        
        // Initialize PreferencesManager before any ViewModel is created
        PreferencesManager.init(this)

        // Initialize FCM storage and preferences
        FcmTokenStore.init(this)
        FcmPreferences.init(this)
        FcmSender.init(this)

        // Register FCM event listener — subscribes to TelegramEventBus to forward
        // events to the Companion app. TelegramNotifier.java is not modified.
        TelegramEventBus.getInstance().subscribe(FcmEventListener())

        // Privileged shell (UID 1000) DISABLED — causes BYD default dashcam
        // to show "no signal" by elevating app's camera priority via accmodemanager.
        // All daemons now run via ADB shell (UID 2000) which is sufficient.
        // PrivilegedShellSetup.init(this)
        // PrivilegedShellSetup.setup(...)

        // Start DaemonKeepaliveService - handles:
        // - Foreground service with START_STICKY
        // - PARTIAL_WAKE_LOCK to prevent CPU sleep
        // - SCREEN_OFF receiver registration
        // - Daemon startup
        DaemonKeepaliveService.start(this)
    }
}
