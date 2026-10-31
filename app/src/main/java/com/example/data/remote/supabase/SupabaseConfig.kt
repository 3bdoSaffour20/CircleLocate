package com.example.data.remote.supabase

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class SupabaseConfig(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("supabase_config_prefs", Context.MODE_PRIVATE)

    var customUrl: String?
        get() = prefs.getString("custom_supabase_url", null)
        set(value) = prefs.edit().putString("custom_supabase_url", value?.trim()).apply()

    var customAnonKey: String?
        get() = prefs.getString("custom_supabase_anon_key", null)
        set(value) = prefs.edit().putString("custom_supabase_anon_key", value?.trim()).apply()

    val effectiveUrl: String
        get() {
            val custom = customUrl
            if (!custom.isNullOrBlank()) return custom
            return try {
                BuildConfig.SUPABASE_URL
            } catch (_: Throwable) {
                "https://your-project.supabase.co"
            }
        }

    val effectiveAnonKey: String
        get() {
            val custom = customAnonKey
            if (!custom.isNullOrBlank()) return custom
            return try {
                BuildConfig.SUPABASE_ANON_KEY
            } catch (_: Throwable) {
                "YOUR_SUPABASE_ANON_KEY"
            }
        }

    fun isConfigured(): Boolean {
        val url = effectiveUrl
        val key = effectiveAnonKey
        return url.isNotBlank() &&
                !url.contains("your-project", ignoreCase = true) &&
                url.startsWith("http") &&
                key.isNotBlank() &&
                !key.contains("YOUR_SUPABASE", ignoreCase = true)
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
