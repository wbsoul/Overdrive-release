package com.overdrive.app.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.overdrive.app.R
import com.overdrive.app.fcm.FcmPreferences
import com.overdrive.app.fcm.FcmTokenStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Companion App Push Notifications (FCM) settings page.
 *
 * Completely independent from TelegramSettingsFragment — separate store,
 * separate preferences, separate UI.
 */
class FcmSettingsFragment : Fragment() {

    private lateinit var tvFcmStatusDot: TextView
    private lateinit var tvFcmStatus: TextView
    private lateinit var tvFcmUpdatedAt: TextView
    private lateinit var btnFcmRemoveDevice: MaterialButton
    private lateinit var switchFcmMaster: SwitchMaterial
    private lateinit var cardFcmEvents: MaterialCardView
    private lateinit var switchFcmMotion: SwitchMaterial
    private lateinit var switchFcmCritical: SwitchMaterial
    private lateinit var switchFcmTunnel: SwitchMaterial
    private lateinit var switchFcmVideo: SwitchMaterial
    private lateinit var switchFcmProximity: SwitchMaterial

    private val dateFormat = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fcm_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        loadState()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        // Refresh status each time the screen is shown — token may have been
        // registered by the Companion app while this screen was in the background.
        refreshRegistrationStatus()
    }

    private fun initViews(view: View) {
        tvFcmStatusDot = view.findViewById(R.id.tvFcmStatusDot)
        tvFcmStatus = view.findViewById(R.id.tvFcmStatus)
        tvFcmUpdatedAt = view.findViewById(R.id.tvFcmUpdatedAt)
        btnFcmRemoveDevice = view.findViewById(R.id.btnFcmRemoveDevice)
        switchFcmMaster = view.findViewById(R.id.switchFcmMaster)
        cardFcmEvents = view.findViewById(R.id.cardFcmEvents)
        switchFcmMotion = view.findViewById(R.id.switchFcmMotion)
        switchFcmCritical = view.findViewById(R.id.switchFcmCritical)
        switchFcmTunnel = view.findViewById(R.id.switchFcmTunnel)
        switchFcmVideo = view.findViewById(R.id.switchFcmVideo)
        switchFcmProximity = view.findViewById(R.id.switchFcmProximity)
    }

    private fun loadState() {
        val prefs = FcmPreferences.getInstance()

        switchFcmMaster.isChecked = prefs.isMasterEnabled()
        updateEventCardEnabled(prefs.isMasterEnabled())

        switchFcmMotion.isChecked = prefs.isMotionEnabled()
        switchFcmCritical.isChecked = prefs.isCriticalEnabled()
        switchFcmTunnel.isChecked = prefs.isTunnelEnabled()
        switchFcmVideo.isChecked = prefs.isVideoEnabled()
        switchFcmProximity.isChecked = prefs.isProximityEnabled()

        refreshRegistrationStatus()
    }

    private fun setupListeners() {
        val prefs = FcmPreferences.getInstance()

        switchFcmMaster.setOnCheckedChangeListener { _, isChecked ->
            prefs.setMasterEnabled(isChecked)
            updateEventCardEnabled(isChecked)
        }

        switchFcmMotion.setOnCheckedChangeListener { _, isChecked ->
            prefs.setMotionEnabled(isChecked)
        }

        switchFcmCritical.setOnCheckedChangeListener { _, isChecked ->
            prefs.setCriticalEnabled(isChecked)
        }

        switchFcmTunnel.setOnCheckedChangeListener { _, isChecked ->
            prefs.setTunnelEnabled(isChecked)
        }

        switchFcmVideo.setOnCheckedChangeListener { _, isChecked ->
            prefs.setVideoEnabled(isChecked)
        }

        switchFcmProximity.setOnCheckedChangeListener { _, isChecked ->
            prefs.setProximityEnabled(isChecked)
        }

        btnFcmRemoveDevice.setOnClickListener {
            FcmTokenStore.getInstance().clear()
            refreshRegistrationStatus()
            Toast.makeText(requireContext(), "Companion device removed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshRegistrationStatus() {
        val store = FcmTokenStore.getInstance()
        if (store.hasToken()) {
            tvFcmStatusDot.setTextColor(Color.parseColor("#4CAF50")) // green
            tvFcmStatus.text = "Registered"
            val updatedAt = store.getUpdatedAt()
            if (updatedAt > 0L) {
                tvFcmUpdatedAt.text = "Last updated: ${dateFormat.format(Date(updatedAt))}"
                tvFcmUpdatedAt.visibility = View.VISIBLE
            }
            btnFcmRemoveDevice.visibility = View.VISIBLE
        } else {
            tvFcmStatusDot.setTextColor(Color.parseColor("#9E9E9E")) // grey
            tvFcmStatus.text = "Not registered"
            tvFcmUpdatedAt.visibility = View.GONE
            btnFcmRemoveDevice.visibility = View.GONE
        }
    }

    private fun updateEventCardEnabled(enabled: Boolean) {
        cardFcmEvents.alpha = if (enabled) 1.0f else 0.4f
        switchFcmMotion.isEnabled = enabled
        switchFcmCritical.isEnabled = enabled
        switchFcmTunnel.isEnabled = enabled
        switchFcmVideo.isEnabled = enabled
        switchFcmProximity.isEnabled = enabled
    }
}
