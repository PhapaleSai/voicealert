package com.phapalesai.voicealert.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.phapalesai.voicealert.data.DeviceProfile
import com.phapalesai.voicealert.data.DeviceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BluetoothDeviceManager(private val context: Context) {

    private val _connectedDeviceFlow = MutableStateFlow<DeviceProfile?>(null)
    val connectedDeviceFlow: StateFlow<DeviceProfile?> = _connectedDeviceFlow

    init {
        checkConnectedDevices()
    }

    fun checkConnectedDevices() {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
                val activeDevice = pairedDevices?.firstOrNull()
                if (activeDevice != null) {
                    val deviceName = activeDevice.name ?: "Bluetooth Device"
                    val type = when {
                        deviceName.lowercase().contains("car") || deviceName.lowercase().contains("pioneer") -> DeviceType.CAR
                        deviceName.lowercase().contains("speaker") || deviceName.lowercase().contains("tv") -> DeviceType.SHARED
                        else -> DeviceType.PERSONAL
                    }
                    _connectedDeviceFlow.value = DeviceProfile(
                        address = activeDevice.address ?: "00:11:22:33:44:55",
                        name = deviceName,
                        type = type,
                        voiceEnabled = type == DeviceType.PERSONAL
                    )
                    return
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        // Fallback default simulation for testing
        _connectedDeviceFlow.value = DeviceProfile(
            address = "AA:BB:CC:DD:EE:FF",
            name = "OnePlus Buds Pro",
            type = DeviceType.PERSONAL,
            voiceEnabled = true
        )
    }

    fun updateDeviceType(type: DeviceType) {
        val current = _connectedDeviceFlow.value ?: return
        _connectedDeviceFlow.value = current.copy(
            type = type,
            voiceEnabled = type == DeviceType.PERSONAL
        )
    }
}
