package com.anhem.translator.model

import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AppStorage(private val appContext: android.content.Context) {
    private val prefs: SharedPreferences = appContext.getSharedPreferences("anhem", android.content.Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun loadProfile(): Profile = loadOr("profile", Profile())
    fun saveProfile(p: Profile) = save("profile", p)

    fun loadSettings(): Settings = loadOr("settings", Settings())
    fun saveSettings(s: Settings) = save("settings", s)

    fun loadActivePersona(): String = prefs.getString("activePersona", "girlfriend") ?: "girlfriend"
    fun saveActivePersona(id: String) = prefs.edit().putString("activePersona", id).apply()

    fun loadContext(personaId: String): Context = loadOr("ctx_$personaId", Context())
    fun saveContext(personaId: String, ctx: Context) = save("ctx_$personaId", ctx)

    fun loadTone(): String = prefs.getString("tone", "") ?: ""
    fun saveTone(tone: String) = prefs.edit().putString("tone", tone).apply()

    fun loadUsageStats(): UsageStats = loadOr("usageStats", UsageStats())
    fun saveUsageStats(s: UsageStats) = save("usageStats", s)

    private inline fun <reified T> loadOr(key: String, default: T): T {
        val raw = prefs.getString(key, null) ?: return default
        return try { json.decodeFromString<T>(raw) } catch (_: Exception) { default }
    }
    private inline fun <reified T> save(key: String, value: T) {
        prefs.edit().putString(key, json.encodeToString(value)).apply()
    }
}
