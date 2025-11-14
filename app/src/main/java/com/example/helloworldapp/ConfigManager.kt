package com.example.helloworldapp.config

import android.content.Context
import java.util.Properties

object ConfigManager {

    private val properties = Properties()
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        try {
            val inputStream = context.assets.open("app-config.properties")
            properties.load(inputStream)
            loaded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * SAFE GETTER:
     * Returns the value or null if missing.
     */
    fun get(key: String): String? {
        return properties.getProperty(key)
    }

    /**
     * PARSE INT (Safe mode)
     * If parsing fails, return fallback
     */
    fun getInt(key: String, default: Int = 0): Int {
        return properties.getProperty(key)?.toIntOrNull() ?: default
    }
}
