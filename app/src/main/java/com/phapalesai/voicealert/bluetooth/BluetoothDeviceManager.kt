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
import com.phapalesai.voicealert.data.defaultVolume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Reports the actual audio-capable Bluetooth device currently connected (A2DP: headphones,
 * speakers, car units) — never a placeholder. If nothing is connected, [connectedDeviceFlow]
 * is null and the UI should say so rather than pretending a device is present.
 */
class BluetoothDeviceManager(private val context: Context) {

    private val _connectedDeviceFlow = MutableStateFlow<DeviceProfile?>(null)
    val connectedDeviceFlow: StateFlow<DeviceProfile?> = _connectedDeviceFlow

    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    // The user can manually override the auto-detected profile (e.g. force "Home Speaker") from
    // the Devices screen; remembered per address so it survives an A2DP proxy refresh but not a
    // disconnect/reconnect to a different device.
    private var manualOverride: Pair<String, DeviceType>? = null

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
                            }
                        }
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                    if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                        _connectedDeviceFlow.value = null
                    }
                }
                ACTION_BATTERY_LEVEL_CHANGED -> {
                    val device = getDeviceExtra(intent) ?: return
                    val current = _connectedDeviceFlow.value
                    if (current != null && current.address == device.address) {
                        val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1).takeIf { it in 0..100 }
                        _connectedDeviceFlow.value = current.copy(batteryLevel = level)
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

        val type = manualOverride?.takeIf { it.first == address }?.second ?: classifyDeviceType(name)
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
                    if (level != null && _connectedDeviceFlow.value?.address == address) {
                        _connectedDeviceFlow.value = _connectedDeviceFlow.value?.copy(batteryLevel = level)
                    }
                    adapter.closeProfileProxy(profile, proxy)
                }

                override fun onServiceDisconnected(profile: Int) {}
            }, BluetoothProfile.HEADSET)
        } catch (e: SecurityException) {
            Log.w("BluetoothDeviceManager", "Missing permission to query HFP battery", e)
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

    fun updateDeviceType(type: DeviceType) {
        val current = _connectedDeviceFlow.value ?: return
        manualOverride = current.address to type
        _connectedDeviceFlow.value = current.copy(
            type = type,
            voiceEnabled = type != DeviceType.SILENT,
            volume = type.defaultVolume()
        )
    }

    companion object {
        // Not part of the public SDK, but sent by the platform Bluetooth stack for any app to
        // observe — this is how Settings/System UI keep earbud battery indicators live.
        private const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        private const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
    }
}
