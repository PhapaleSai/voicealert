package com.phapalesai.voicealert.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "voice_alert_prefs")

class PreferencesRepository(private val context: Context) {

    companion object {
        val KEY_TRAVEL_MODE = booleanPreferencesKey("travel_mode_active")
        val KEY_PRIVACY_MODE = booleanPreferencesKey("privacy_mode_active")
        val KEY_PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language") // "auto", "en", "hi", "mr"
        val KEY_SPEECH_SPEED = stringPreferencesKey("speech_speed") // "1.0"
        val KEY_OTP_PROTECTION = booleanPreferencesKey("otp_protection")
        val KEY_BANK_MASKING = booleanPreferencesKey("bank_masking")
    }

    val travelModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_TRAVEL_MODE] ?: true
    }

    val privacyModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PRIVACY_MODE] ?: false
    }

    val preferredLanguageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PREFERRED_LANGUAGE] ?: "auto"
    }

    val otpProtectionFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_OTP_PROTECTION] ?: true
    }

    val bankMaskingFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BANK_MASKING] ?: true
    }

    suspend fun setTravelMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TRAVEL_MODE] = enabled
        }
    }

    suspend fun setPrivacyMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PRIVACY_MODE] = enabled
        }
    }

    suspend fun setPreferredLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PREFERRED_LANGUAGE] = language
        }
    }

    suspend fun setOtpProtection(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OTP_PROTECTION] = enabled
        }
    }

    suspend fun setBankMasking(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BANK_MASKING] = enabled
        }
    }
}
