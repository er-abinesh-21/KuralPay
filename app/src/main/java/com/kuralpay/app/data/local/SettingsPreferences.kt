package com.kuralpay.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kuralpay.app.domain.model.SummarySlot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        val NEVER_SYNCED = setOf("groq_api_key", "synced_uid")
    }

    private object Keys {
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val VOICE_PROFILE = stringPreferencesKey("voice_profile")
        val AUDIO_ROUTE = stringPreferencesKey("audio_route")
        val SUMMARY_9AM = booleanPreferencesKey("summary_9am")
        val SUMMARY_1PM = booleanPreferencesKey("summary_1pm")
        val SUMMARY_6PM = booleanPreferencesKey("summary_6pm")
        val SUMMARY_CLOSING = booleanPreferencesKey("summary_closing")
        val SUMMARY_DETAILS = booleanPreferencesKey("summary_details")
        val PREMIUM_ENABLED = booleanPreferencesKey("premium_enabled")
        val OVERLAY_DURATION = intPreferencesKey("overlay_duration")
        val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        val MAX_VOLUME = booleanPreferencesKey("max_volume")
        val REPLAY_COUNT = intPreferencesKey("replay_count")
    }

    /** How long payment history is kept, in days. 0 means forever. */
    val retentionDays: Flow<Int> = context.settingsDataStore.data.map { it[intPreferencesKey("retention_days")] ?: 0 }

    suspend fun setRetentionDays(days: Int) {
        context.settingsDataStore.updateData { it.toMutablePreferences().apply { this[intPreferencesKey("retention_days")] = days.coerceAtLeast(0) } }
    }

    /** The account whose data is on this phone; a different account signing in wipes the old one's local copy. */
    suspend fun syncedUid(): String? = context.settingsDataStore.data.first()[stringPreferencesKey("synced_uid")]

    suspend fun setSyncedUid(uid: String) {
        context.settingsDataStore.updateData { it.toMutablePreferences().apply { this[stringPreferencesKey("synced_uid")] = uid } }
    }

    /** Everything worth backing up. The Groq key and sync bookkeeping never leave the phone. */
    val syncable: Flow<Map<String, Any>> = context.settingsDataStore.data.map { prefs ->
        prefs.asMap().entries
            .filter { it.key.name !in NEVER_SYNCED }
            .associate { it.key.name to it.value }
    }

    suspend fun applySynced(values: Map<String, Any?>) {
        context.settingsDataStore.updateData { current ->
            current.toMutablePreferences().apply {
                values.forEach { (name, value) ->
                    if (name in NEVER_SYNCED) return@forEach
                    when (value) {
                        is Boolean -> this[booleanPreferencesKey(name)] = value
                        is Number -> this[intPreferencesKey(name)] = value.toInt()
                        is String -> this[stringPreferencesKey(name)] = value
                    }
                }
            }
        }
    }

    fun summaryTime(slot: SummarySlot): Flow<Int> =
        context.settingsDataStore.data.map { it[intPreferencesKey("${slot.tag}_minutes")] ?: slot.defaultMinutes }

    suspend fun setSummaryTime(slot: SummarySlot, minutes: Int) {
        context.settingsDataStore.updateData {
            it.toMutablePreferences().apply { this[intPreferencesKey("${slot.tag}_minutes")] = minutes.coerceIn(0, 24 * 60 - 1) }
        }
    }

    val forwardingEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[booleanPreferencesKey("forwarding_enabled")] ?: true }
    val acceptForwarded: Flow<Boolean> = context.settingsDataStore.data.map { it[booleanPreferencesKey("accept_forwarded")] ?: true }

    suspend fun setForwardingEnabled(enabled: Boolean) {
        context.settingsDataStore.updateData { it.toMutablePreferences().apply { this[booleanPreferencesKey("forwarding_enabled")] = enabled } }
    }

    suspend fun setAcceptForwarded(enabled: Boolean) {
        context.settingsDataStore.updateData { it.toMutablePreferences().apply { this[booleanPreferencesKey("accept_forwarded")] = enabled } }
    }

    val replayCount: Flow<Int> = context.settingsDataStore.data.map { (it[Keys.REPLAY_COUNT] ?: 1).coerceIn(1, 10) }

    suspend fun setReplayCount(count: Int) {
        context.settingsDataStore.updateData { it.toMutablePreferences().apply { this[Keys.REPLAY_COUNT] = count.coerceIn(1, 10) } }
    }

    val maxVolume: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.MAX_VOLUME] ?: false }

    suspend fun setMaxVolume(enabled: Boolean) {
        context.settingsDataStore.updateData { it.toMutablePreferences().apply { this[Keys.MAX_VOLUME] = enabled } }
    }

    val overlayEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.OVERLAY_ENABLED] ?: true }
    val voiceEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.VOICE_ENABLED] ?: true }
    val voiceProfile: Flow<String> = context.settingsDataStore.data.map { it[Keys.VOICE_PROFILE] ?: "SHOP" }
    val audioRoute: Flow<String> = context.settingsDataStore.data.map { it[Keys.AUDIO_ROUTE] ?: "AUTO_DETECT" }
    val summary9am: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.SUMMARY_9AM] ?: true }
    val summary1pm: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.SUMMARY_1PM] ?: true }
    val summary6pm: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.SUMMARY_6PM] ?: true }
    val summaryClosing: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.SUMMARY_CLOSING] ?: true }
    val summaryDetails: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.SUMMARY_DETAILS] ?: false }
    val premiumEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.PREMIUM_ENABLED] ?: false }
    val overlayDuration: Flow<Int> = context.settingsDataStore.data.map { it[Keys.OVERLAY_DURATION] ?: 4000 }
    val groqApiKey: Flow<String> = context.settingsDataStore.data.map { it[Keys.GROQ_API_KEY] ?: "" }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.settingsDataStore.updateData { it.toMutablePreferences().apply { this[Keys.OVERLAY_ENABLED] = enabled } }
    }

    suspend fun setVoiceEnabled(enabled: Boolean) {
        context.settingsDataStore.updateData { it.toMutablePreferences().apply { this[Keys.VOICE_ENABLED] = enabled } }
    }

    suspend fun setVoiceProfile(profile: String) {
        context.settingsDataStore.updateData { it.toMutablePreferences().apply { this[Keys.VOICE_PROFILE] = profile } }
    }

    suspend fun setAudioRoute(route: String) {
        context.settingsDataStore.updateData { it.toMutablePreferences().apply { this[Keys.AUDIO_ROUTE] = route } }
    }

    suspend fun setSummary9am(enabled: Boolean) {
        context.settingsDataStore.updateData { it.toMutablePreferences().apply { this[Keys.SUMMARY_9AM] = enabled } }
    }

    suspend fun setSummary1pm(enabled: Boolean) {
        context.settingsDataStore.updateData { it.toMutablePreferences().apply { this[Keys.SUMMARY_1PM] = enabled } }
    }

    suspend fun setSummary6pm(enabled: Boolean) {
        context.settingsDataStore.updateData { it.toMutablePreferences().apply { this[Keys.SUMMARY_6PM] = enabled } }
    }

    suspend fun setSummaryClosing(enabled: Boolean) {
        context.settingsDataStore.updateData { it.toMutablePreferences().apply { this[Keys.SUMMARY_CLOSING] = enabled } }
    }

    suspend fun setSummaryDetails(enabled: Boolean) {
        context.settingsDataStore.updateData { it.toMutablePreferences().apply { this[Keys.SUMMARY_DETAILS] = enabled } }
    }

    suspend fun setOverlayDuration(duration: Int) {
        context.settingsDataStore.updateData { it.toMutablePreferences().apply { this[Keys.OVERLAY_DURATION] = duration } }
    }

    suspend fun setGroqApiKey(key: String) {
        context.settingsDataStore.updateData { it.toMutablePreferences().apply { this[Keys.GROQ_API_KEY] = key } }
    }
}

