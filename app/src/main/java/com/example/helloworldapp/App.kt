package com.example.helloworldapp

import android.app.Application
import com.example.helloworldapp.config.ConfigManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ConfigManager.load(this)
    }
}
