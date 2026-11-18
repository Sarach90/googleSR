package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AiCallLog
import com.example.data.model.AiSettings
import com.example.data.model.GeminiModelCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.aiSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_studio_settings")

class AiSettingsStore(private val context: Context) {

    private object Keys {
        val PRIMARY_MODEL_ID = stringPreferencesKey("primary_model_id")
        val FALLBACK_MODEL_ID = stringPreferencesKey("fallback_model_id")
        val ECONOMIC_MODE = booleanPreferencesKey("economic_mode_enabled")
        val CALL_COUNT = intPreferencesKey("ai_call_count")
        val CALL_LOGS_JSON = stringPreferencesKey("ai_call_logs_json")
    }

    val settingsFlow: Flow<AiSettings> = context.aiSettingsDataStore.data.map { prefs ->
        val primary = prefs[Keys.PRIMARY_MODEL_ID] ?: GeminiModelCatalog.DEFAULT_PRIMARY_ID
        val fallbackRaw = prefs[Keys.FALLBACK_MODEL_ID]
        val fallback = when {
            fallbackRaw == null -> GeminiModelCatalog.DEFAULT_FALLBACK_ID
            fallbackRaw.isBlank() -> null
            else -> fallbackRaw
        }
        val economic = prefs[Keys.ECONOMIC_MODE] ?: true
        val count = prefs[Keys.CALL_COUNT] ?: 0
        val logs = runCatching {
            val raw = prefs[Keys.CALL_LOGS_JSON] ?: "[]"
            val arr = JSONArray(raw)
            (0 until arr.length()).map { AiCallLog.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())

        AiSettings(
            primaryModelId = primary,
            fallbackModelId = fallback,
            economicModeEnabled = economic,
            sessionCallCount = count,
            callLogs = logs
        )
    }

    suspend fun setPrimaryModel(modelId: String) {
        context.aiSettingsDataStore.edit { prefs ->
            prefs[Keys.PRIMARY_MODEL_ID] = modelId
        }
    }

    suspend fun setFallbackModel(modelId: String?) {
        context.aiSettingsDataStore.edit { prefs ->
            prefs[Keys.FALLBACK_MODEL_ID] = modelId ?: ""
        }
    }

    suspend fun setEconomicMode(enabled: Boolean) {
        context.aiSettingsDataStore.edit { prefs ->
            prefs[Keys.ECONOMIC_MODE] = enabled
        }
    }

    suspend fun recordCall(log: AiCallLog) {
        context.aiSettingsDataStore.edit { prefs ->
            val currentCount = prefs[Keys.CALL_COUNT] ?: 0
            prefs[Keys.CALL_COUNT] = currentCount + 1

            val existing = runCatching {
                val arr = JSONArray(prefs[Keys.CALL_LOGS_JSON] ?: "[]")
                (0 until arr.length()).map { AiCallLog.fromJson(arr.getJSONObject(it)) }
            }.getOrDefault(emptyList())

            val updated = (listOf(log) + existing).take(15)
            val arr = JSONArray().apply { updated.forEach { put(it.toJson()) } }
            prefs[Keys.CALL_LOGS_JSON] = arr.toString()
        }
    }

    suspend fun clearCallLogs() {
        context.aiSettingsDataStore.edit { prefs ->
            prefs[Keys.CALL_LOGS_JSON] = "[]"
            prefs[Keys.CALL_COUNT] = 0
        }
    }
}
