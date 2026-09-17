package com.phapalesai.voicealert

import android.app.Application
import com.phapalesai.voicealert.bluetooth.BluetoothDeviceManager
import com.phapalesai.voicealert.data.PreferencesRepository

class VoiceAlertApp : Application() {

    lateinit var preferencesRepository: PreferencesRepository
        private set

    lateinit var bluetoothManager: BluetoothDeviceManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesRepository = PreferencesRepository(applicationContext)
        bluetoothManager = BluetoothDeviceManager(applicationContext)
    }

    companion object {
        lateinit var instance: VoiceAlertApp
            private set
    }
}
