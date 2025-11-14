package com.example.helloworldapp

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.helloworldapp.config.AppConfig

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Check Wi-Fi connection before proceeding
        if (!isWifiConnected()) {
            Toast.makeText(
                this,
                AppConfig.WIFI_ERROR_MESSAGE,
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val prefs = getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)

        val name = prefs.getString(AppConfig.KEY_NAME, null)
        val role = prefs.getString(AppConfig.KEY_ROLE, null)
        val email = prefs.getString(AppConfig.KEY_EMAIL, null)
        val userId = prefs.getString(AppConfig.KEY_USER_ID, null)
        val protectionStatus = prefs.getString(
            AppConfig.KEY_PROTECTION,
            AppConfig.DEFAULT_PROTECTION
        )

        // If user not registered → RegistrationActivity
        if (name == null || role == null || email == null || userId == null) {
            startActivity(Intent(this, RegistrationActivity::class.java))
            finish()
            return
        }

        // If registered → check protection
        if (protectionStatus == AppConfig.PROTECTION_ENABLED) {
            startActivity(Intent(this, AuthenticationActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        finish()
    }

    private fun isWifiConnected(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
