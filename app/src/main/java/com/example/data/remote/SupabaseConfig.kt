package com.example.data.remote

import android.content.Context
import androidx.core.content.edit

/**
 * Supabase connection configuration.
 *
 * Supports dynamic configuration via SharedPreferences so the admin
 * can configure their own Supabase project URL and anon key directly from the app.
 */
object SupabaseConfig {
    private const val PREFS = "supabase_config_prefs"
    private const val KEY_CUSTOM_URL = "custom_supabase_url"
    private const val KEY_CUSTOM_ANON = "custom_supabase_anon_key"

    private const val DEFAULT_PROJECT_URL = "https://dszzgenzrmxxcnrzvdwi.supabase.co"
    private const val DEFAULT_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRzenpnZW56cm14eGNucnp2ZHdpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk0MjIxNTMsImV4cCI6MjEwNDk5ODE1M30.CD64fa5bvh9BKFQrW24ROJieROWz1GKH6-TxILUjWuE"

    @Volatile
    private var customUrl: String? = null

    @Volatile
    private var customKey: String? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        customUrl = prefs.getString(KEY_CUSTOM_URL, null)
        customKey = prefs.getString(KEY_CUSTOM_ANON, null)
    }

    fun cleanUrl(rawUrl: String): String {
        var u = rawUrl.trim().trimEnd('/')
        if (u.endsWith("/rest/v1")) {
            u = u.removeSuffix("/rest/v1")
        }
        if (u.endsWith("/auth/v1")) {
            u = u.removeSuffix("/auth/v1")
        }
        return u.trimEnd('/')
    }

    fun saveConfig(context: Context, url: String, anonKey: String) {
        val cleanUrl = cleanUrl(url)
        val cleanKey = anonKey.trim()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_CUSTOM_URL, cleanUrl)
            putString(KEY_CUSTOM_ANON, cleanKey)
        }
        customUrl = cleanUrl
        customKey = cleanKey
    }

    fun getProjectUrl(): String {
        return customUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_PROJECT_URL
    }

    fun getAnonKey(): String {
        return customKey?.takeIf { it.isNotBlank() } ?: DEFAULT_KEY
    }

    fun isConfigured(): Boolean {
        val u = getProjectUrl()
        return u.isNotBlank() && (u.startsWith("https://") || u.startsWith("http://"))
    }

    val BASE_URL: String
        get() {
            val u = customUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_PROJECT_URL
            return u.trimEnd('/') + "/"
        }

    val ANON_KEY: String
        get() = customKey?.takeIf { it.isNotBlank() } ?: DEFAULT_KEY

    val REST_BASE: String
        get() = BASE_URL + "rest/v1/"

    val AUTH_BASE: String
        get() = BASE_URL + "auth/v1/"
}

