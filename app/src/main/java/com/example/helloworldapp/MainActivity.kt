package com.example.helloworldapp

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Check Wi-Fi connection before proceeding
        if (!isWifiConnected()) {
            Toast.makeText(
                this,
                "Please connect to a Wi-Fi network to continue.",
                Toast.LENGTH_LONG
            ).show()
            return // Stop further navigation
        }

        // ✅ Read stored user data
        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val name = prefs.getString("name", null)
        val role = prefs.getString("role", null)
        val email = prefs.getString("email", null)
        val userId = prefs.getString("userId", null)
        val protectionStatus = prefs.getString("protection", "disabled")

        // ✅ 1️⃣ Check user registration
        if (name == null || role == null || email == null || userId == null) {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // ✅ 2️⃣ Registered → check protection status
        if (protectionStatus == "enabled") {
            val intent = Intent(this, AuthenticationActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // ✅ Function to check Wi-Fi connection
    private fun isWifiConnected(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
