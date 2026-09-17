package com.phapalesai.voicealert.bluetooth

import android.Manifest
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
            val device = getDeviceExtra(intent) ?: return
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> applyConnectedDevice(device)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    if (device.address == _connectedDeviceFlow.value?.address) {
                        _connectedDeviceFlow.value = null
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
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
        val name = try { device.name } catch (e: SecurityException) { null } ?: "Unknown Device"
        val address = device.address ?: return

        val type = manualOverride?.takeIf { it.first == address }?.second ?: classifyDeviceType(name)
        _connectedDeviceFlow.value = DeviceProfile(
            address = address,
            name = name,
            type = type,
            voiceEnabled = type != DeviceType.SILENT,
            volume = type.defaultVolume()
        )
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
}
