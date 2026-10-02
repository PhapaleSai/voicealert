package com.phapalesai.voicealert.bluetooth

import android.Manifest
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.phapalesai.voicealert.data.DeviceProfile
import com.phapalesai.voicealert.data.DeviceType
import com.phapalesai.voicealert.data.PreferencesRepository
import com.phapalesai.voicealert.data.defaultVolume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val LOW_BATTERY_THRESHOLD = 20

/**
 * Reports the actual audio-capable Bluetooth device currently connected (A2DP: headphones,
 * speakers, car units) — never a placeholder. If nothing is connected, [connectedDeviceFlow]
 * is null and the UI should say so rather than pretending a device is present.
 */
class BluetoothDeviceManager(
    private val context: Context,
    private val repository: PreferencesRepository
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _connectedDeviceFlow = MutableStateFlow<DeviceProfile?>(null)
    val connectedDeviceFlow: StateFlow<DeviceProfile?> = _connectedDeviceFlow

    // Fires once each time a connected device's battery first drops to/under the low threshold,
    // so the service can speak a single warning instead of repeating it on every battery tick.
    private val _lowBatteryEventFlow = MutableSharedFlow<DeviceProfile>(extraBufferCapacity = 1)
    val lowBatteryEventFlow: SharedFlow<DeviceProfile> = _lowBatteryEventFlow
    private var lowBatteryWarnedAddress: String? = null

    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val connectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(receivedContext: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    // ACL is the radio-level link, not the audio profile — a device can stay
                    // ACL-connected while A2DP disconnects (e.g. it drops to a low-power/idle
                    // state), so re-verify against the actual profile rather than trusting this.
                    refreshCurrentlyConnectedDevice()
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = getDeviceExtra(intent)
                    if (device != null && device.address == _connectedDeviceFlow.value?.address) {
                        _connectedDeviceFlow.value = null
                        lowBatteryWarnedAddress = null
                    }
                }
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    // The precise signal for "can we actually play audio on this device right
                    // now" — this is what was missing before, so disconnecting only the A2DP
                    // profile (common: earbuds going idle, or manually toggling media audio off
                    // for the device) left the UI stuck showing it as connected.
                    val device = getDeviceExtra(intent) ?: return
                    val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                    when (state) {
                        BluetoothProfile.STATE_CONNECTED -> applyConnectedDevice(device)
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            if (device.address == _connectedDeviceFlow.value?.address) {
                                _connectedDeviceFlow.value = null
                                lowBatteryWarnedAddress = null
                            }
                        }
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                    if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                        _connectedDeviceFlow.value = null
                        lowBatteryWarnedAddress = null
                    }
                }
                ACTION_BATTERY_LEVEL_CHANGED -> {
                    val device = getDeviceExtra(intent) ?: return
                    val current = _connectedDeviceFlow.value
                    if (current != null && current.address == device.address) {
                        val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1).takeIf { it in 0..100 }
                        applyBatteryLevel(current, level)
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(ACTION_BATTERY_LEVEL_CHANGED)
        }
        ContextCompat.registerReceiver(context, connectionReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        refreshCurrentlyConnectedDevice()
    }

    private fun hasBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** Asks the A2DP profile (music/call audio — headphones, speakers, car units) who's connected right now. */
    fun refreshCurrentlyConnectedDevice() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled || !hasBluetoothPermission()) {
            _connectedDeviceFlow.value = null
            return
        }

        try {
            adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    val connected = try {
                        proxy.connectedDevices.firstOrNull()
                    } catch (e: SecurityException) {
                        null
                    }
                    if (connected != null) {
                        applyConnectedDevice(connected)
                    } else {
                        _connectedDeviceFlow.value = null
                    }
                    adapter.closeProfileProxy(profile, proxy)
                }

                override fun onServiceDisconnected(profile: Int) {}
            }, BluetoothProfile.A2DP)
        } catch (e: SecurityException) {
            Log.w("BluetoothDeviceManager", "Missing permission to query A2DP devices", e)
            _connectedDeviceFlow.value = null
        }
    }

    private fun getDeviceExtra(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    private fun applyConnectedDevice(device: BluetoothDevice) {
        if (!hasBluetoothPermission()) return
        val name = resolveDisplayName(device)
        val address = device.address ?: return

        scope.launch {
            val savedProfiles = repository.deviceProfilesFlow.first()
            val type = savedProfiles[address]?.let {
                try { DeviceType.valueOf(it) } catch (e: IllegalArgumentException) { null }
            } ?: classifyDeviceType(name)

            _connectedDeviceFlow.value = DeviceProfile(
                address = address,
                name = name,
                type = type,
                voiceEnabled = type != DeviceType.SILENT,
                volume = type.defaultVolume(),
                batteryLevel = readBatteryLevelReflectively(device)
            )

            // The A2DP device object often doesn't carry a battery value even when the device does
            // report one over HFP — check that profile too, asynchronously, and patch it in if found.
            if (_connectedDeviceFlow.value?.batteryLevel == null) {
                queryHeadsetBatteryLevel(address)
            } else {
                _connectedDeviceFlow.value?.let { applyBatteryLevel(it, it.batteryLevel) }
            }
        }
    }

    private fun queryHeadsetBatteryLevel(address: String) {
        val adapter = bluetoothAdapter ?: return
        try {
            adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    val match = try {
                        proxy.connectedDevices.firstOrNull { it.address == address }
                    } catch (e: SecurityException) {
                        null
                    }
                    val level = match?.let { readBatteryLevelReflectively(it) }
                    val current = _connectedDeviceFlow.value
                    if (level != null && current?.address == address) {
                        val updated = current.copy(batteryLevel = level)
                        _connectedDeviceFlow.value = updated
                        applyBatteryLevel(updated, level)
                    }
                    adapter.closeProfileProxy(profile, proxy)
                }

                override fun onServiceDisconnected(profile: Int) {}
            }, BluetoothProfile.HEADSET)
        } catch (e: SecurityException) {
            Log.w("BluetoothDeviceManager", "Missing permission to query HFP battery", e)
        }
    }

    /** Updates the stored battery level and fires a one-time low-battery event when it first drops low. */
    private fun applyBatteryLevel(device: DeviceProfile, level: Int?) {
        _connectedDeviceFlow.value = _connectedDeviceFlow.value?.takeIf { it.address == device.address }
            ?.copy(batteryLevel = level) ?: return

        if (level == null) return
        if (level <= LOW_BATTERY_THRESHOLD) {
            if (lowBatteryWarnedAddress != device.address) {
                lowBatteryWarnedAddress = device.address
                _lowBatteryEventFlow.tryEmit(device.copy(batteryLevel = level))
            }
        } else {
            // Recovered above the threshold (charged up) — allow a fresh warning if it drops again.
            if (lowBatteryWarnedAddress == device.address) {
                lowBatteryWarnedAddress = null
            }
        }
    }

    /**
     * The system Bluetooth Settings page shows the alias the user gave a paired device (e.g.
     * "Sai's Boult Z60"), not its raw advertised Bluetooth name (e.g. "Boult Audio Airbass").
     * getAlias() is the public API for that (since API 30); older devices fall back to the raw name.
     */
    private fun resolveDisplayName(device: BluetoothDevice): String {
        val alias = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) device.alias else null
        } catch (e: SecurityException) {
            null
        }
        val rawName = try { device.name } catch (e: SecurityException) { null }
        return alias ?: rawName ?: "Unknown Device"
    }

    /**
     * BluetoothDevice.getBatteryLevel() has existed in AOSP for years but isn't in the public
     * SDK stubs, so it's called via reflection — the same trick Android's own Settings/Bluetooth
     * UI effectively relies on. Not every device (or every profile connection to the same device)
     * reports battery this way, so [queryHeadsetBatteryLevel] also tries the HEADSET (HFP) profile
     * asynchronously, since that's often the one that actually carries the AT+BATT report.
     */
    private fun readBatteryLevelReflectively(device: BluetoothDevice): Int? {
        return try {
            val method = device.javaClass.getMethod("getBatteryLevel")
            val level = method.invoke(device) as? Int
            level?.takeIf { it in 0..100 }
        } catch (e: Exception) {
            null
        }
    }

    private fun classifyDeviceType(name: String): DeviceType {
        val lower = name.lowercase()
        return when {
            lower.contains("car") || lower.contains("pioneer") || lower.contains("auto") -> DeviceType.CAR
            lower.contains("speaker") || lower.contains("tv") || lower.contains("home") -> DeviceType.SHARED
            else -> DeviceType.PERSONAL
        }
    }

    /** Changes the current device's profile and remembers it permanently for this address. */
    fun updateDeviceType(type: DeviceType) {
        val current = _connectedDeviceFlow.value ?: return
        _connectedDeviceFlow.value = current.copy(
            type = type,
            voiceEnabled = type != DeviceType.SILENT,
            volume = type.defaultVolume()
        )
        scope.launch { repository.setDeviceProfile(current.address, type.name) }
    }

    companion object {
        // Not part of the public SDK, but sent by the platform Bluetooth stack for any app to
        // observe — this is how Settings/System UI keep earbud battery indicators live.
        private const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        private const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
    }
}
