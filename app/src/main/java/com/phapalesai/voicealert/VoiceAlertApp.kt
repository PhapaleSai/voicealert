package com.phapalesai.voicealert

import android.app.Application
import com.phapalesai.voicealert.data.PreferencesRepository

class VoiceAlertApp : Application() {

    lateinit var preferencesRepository: PreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesRepository = PreferencesRepository(applicationContext)
    }

    companion object {
        lateinit var instance: VoiceAlertApp
            private set
    }
}
