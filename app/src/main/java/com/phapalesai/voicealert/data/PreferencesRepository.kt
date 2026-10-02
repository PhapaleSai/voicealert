package com.phapalesai.voicealert.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
        val KEY_APP_RULES = stringPreferencesKey("app_speak_rules") // "pkg1=SPEAK;pkg2=DISABLED"
        val KEY_QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val KEY_QUIET_HOURS_START = intPreferencesKey("quiet_hours_start_hour") // 0-23
        val KEY_QUIET_HOURS_END = intPreferencesKey("quiet_hours_end_hour") // 0-23
        val KEY_RESPECT_DND = booleanPreferencesKey("respect_system_dnd")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_ANNOUNCE_CALLER = booleanPreferencesKey("announce_caller_enabled")
        val KEY_DEVICE_PROFILES = stringPreferencesKey("device_profile_overrides") // "AA:BB=CAR;CC:DD=SHARED"
        val KEY_VIP_CONTACTS = stringPreferencesKey("vip_contact_names") // "Mom;Dad;Boss"
        val KEY_MISSED_CALLS = stringPreferencesKey("pending_missed_calls") // "name|number|timestamp;..."
        val KEY_LOW_BATTERY_ALERT = booleanPreferencesKey("low_battery_alert_enabled")
        val KEY_DAILY_DIGEST_ENABLED = booleanPreferencesKey("daily_digest_enabled")
        val KEY_DIGEST_LAST_DATE = stringPreferencesKey("digest_last_date") // "yyyy-MM-dd"
        val KEY_DIGEST_SPOKEN_COUNT = intPreferencesKey("digest_spoken_count")
        val KEY_DIGEST_MISSED_CALL_COUNT = intPreferencesKey("digest_missed_call_count")
        val KEY_SHAKE_TO_STOP = booleanPreferencesKey("shake_to_stop_enabled")
    }

    /** Map of packageName -> SpeakMode name, for apps the user has explicitly configured. */
    val appRulesFlow: Flow<Map<String, String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_APP_RULES] ?: return@map emptyMap()
        raw.split(";")
            .filter { it.isNotBlank() && it.contains("=") }
            .associate {
                val (pkg, mode) = it.split("=", limit = 2)
                pkg to mode
            }
    }

    suspend fun setAppMode(packageName: String, mode: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_APP_RULES] ?: ""
            val rules = current.split(";")
                .filter { it.isNotBlank() && it.contains("=") }
                .associate {
                    val (pkg, m) = it.split("=", limit = 2)
                    pkg to m
                }
                .toMutableMap()
            rules[packageName] = mode
            prefs[KEY_APP_RULES] = rules.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }

    /** Applies [mode] to every package in [packageNames] in a single write, for bulk actions. */
    suspend fun setAppModes(packageNames: Collection<String>, mode: String) {
        if (packageNames.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_APP_RULES] ?: ""
            val rules = current.split(";")
                .filter { it.isNotBlank() && it.contains("=") }
                .associate {
                    val (pkg, m) = it.split("=", limit = 2)
                    pkg to m
                }
                .toMutableMap()
            packageNames.forEach { rules[it] = mode }
            prefs[KEY_APP_RULES] = rules.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
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

    /** Whether Quiet Hours is on (e.g. auto-mute overnight or during a meeting window). */
    val quietHoursEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_QUIET_HOURS_ENABLED] ?: false
    }

    /** Quiet Hours start hour, 0-23, local time. Default 22 (10 PM). */
    val quietHoursStartFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_QUIET_HOURS_START] ?: 22
    }

    /** Quiet Hours end hour, 0-23, local time. Default 7 (7 AM). */
    val quietHoursEndFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_QUIET_HOURS_END] ?: 7
    }

    /** Whether to stay silent (except Speak Over apps) when the phone's system Do Not Disturb is active. */
    val respectDndFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_RESPECT_DND] ?: true
    }

    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_QUIET_HOURS_ENABLED] = enabled }
    }

    suspend fun setQuietHoursRange(startHour: Int, endHour: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_QUIET_HOURS_START] = startHour
            prefs[KEY_QUIET_HOURS_END] = endHour
        }
    }

    suspend fun setRespectDnd(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_RESPECT_DND] = enabled }
    }

    /** The user's own name, spoken in the caller announcement (e.g. "Hey Sai, Mom is calling"). */
    val userNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME] ?: ""
    }

    /** Whether incoming calls get announced by caller name/number at all. */
    val announceCallerFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ANNOUNCE_CALLER] ?: true
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { prefs -> prefs[KEY_USER_NAME] = name }
    }

    suspend fun setAnnounceCaller(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_ANNOUNCE_CALLER] = enabled }
    }

    /** Map of Bluetooth address -> DeviceType name, remembered permanently so reconnecting to the
     *  same car/speaker/headphones doesn't require reselecting its profile every time. */
    val deviceProfilesFlow: Flow<Map<String, String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_DEVICE_PROFILES] ?: return@map emptyMap()
        raw.split(";")
            .filter { it.isNotBlank() && it.contains("=") }
            .associate {
                val (address, type) = it.split("=", limit = 2)
                address to type
            }
    }

    suspend fun setDeviceProfile(address: String, type: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_DEVICE_PROFILES] ?: ""
            val profiles = current.split(";")
                .filter { it.isNotBlank() && it.contains("=") }
                .associate {
                    val (addr, t) = it.split("=", limit = 2)
                    addr to t
                }
                .toMutableMap()
            profiles[address] = type
            prefs[KEY_DEVICE_PROFILES] = profiles.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }

    /** Contact display names that always break through — Speak Over, bypassing Quiet Hours/DND/app rules. */
    val vipContactsFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_VIP_CONTACTS] ?: return@map emptyList()
        raw.split(";").filter { it.isNotBlank() }
    }

    suspend fun addVipContact(name: String) {
        if (name.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_VIP_CONTACTS] ?: ""
            val names = current.split(";").filter { it.isNotBlank() }.toMutableList()
            if (names.none { it.equals(name, ignoreCase = true) }) {
                names.add(name)
            }
            prefs[KEY_VIP_CONTACTS] = names.joinToString(";")
        }
    }

    suspend fun removeVipContact(name: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_VIP_CONTACTS] ?: ""
            val names = current.split(";").filter { it.isNotBlank() && !it.equals(name, ignoreCase = true) }
            prefs[KEY_VIP_CONTACTS] = names.joinToString(";")
        }
    }

    /** Calls that rang and were never answered, waiting to be announced next time the user unlocks the phone. */
    val missedCallsFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_MISSED_CALLS] ?: return@map emptyList()
        raw.split(";").filter { it.isNotBlank() }
    }

    suspend fun addMissedCall(entry: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_MISSED_CALLS] ?: ""
            val entries = current.split(";").filter { it.isNotBlank() }.toMutableList()
            entries.add(entry)
            prefs[KEY_MISSED_CALLS] = entries.joinToString(";")
        }
    }

    suspend fun clearMissedCalls() {
        context.dataStore.edit { prefs -> prefs[KEY_MISSED_CALLS] = "" }
    }

    /** Whether to proactively announce when a connected device's battery gets low. */
    val lowBatteryAlertFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOW_BATTERY_ALERT] ?: true
    }

    suspend fun setLowBatteryAlert(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_LOW_BATTERY_ALERT] = enabled }
    }

    /** Whether a short spoken summary of the previous day's activity plays once a new day starts. */
    val dailyDigestEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DAILY_DIGEST_ENABLED] ?: false
    }

    suspend fun setDailyDigestEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_DAILY_DIGEST_ENABLED] = enabled }
    }

    val digestLastDateFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DIGEST_LAST_DATE] ?: ""
    }

    val digestSpokenCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DIGEST_SPOKEN_COUNT] ?: 0
    }

    val digestMissedCallCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DIGEST_MISSED_CALL_COUNT] ?: 0
    }

    suspend fun incrementDigestSpokenCount() {
        context.dataStore.edit { prefs -> prefs[KEY_DIGEST_SPOKEN_COUNT] = (prefs[KEY_DIGEST_SPOKEN_COUNT] ?: 0) + 1 }
    }

    suspend fun incrementDigestMissedCallCount() {
        context.dataStore.edit { prefs -> prefs[KEY_DIGEST_MISSED_CALL_COUNT] = (prefs[KEY_DIGEST_MISSED_CALL_COUNT] ?: 0) + 1 }
    }

    /** Resets the daily counters to zero for the new day, after the digest for the old day was spoken. */
    suspend fun resetDigestForNewDay(today: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DIGEST_LAST_DATE] = today
            prefs[KEY_DIGEST_SPOKEN_COUNT] = 0
            prefs[KEY_DIGEST_MISSED_CALL_COUNT] = 0
        }
    }

    /** Whether shaking the phone immediately silences whatever VoiceAlert is currently speaking. */
    val shakeToStopFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHAKE_TO_STOP] ?: true
    }

    suspend fun setShakeToStop(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_SHAKE_TO_STOP] = enabled }
    }
}
