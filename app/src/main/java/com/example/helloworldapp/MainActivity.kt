package com.example.helloworldapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Read stored user data
        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val name = prefs.getString("name", null)
        val role = prefs.getString("role", null)
        val email = prefs.getString("email", null)
        val userId = prefs.getString("userId", null)
        val protectionStatus = prefs.getString("protection", "disabled") // 👈 biometric protection status

        // ✅ 1️⃣ Check user registration
        if (name == null || role == null || email == null || userId == null) {
            // ❌ No registration → go to RegistrationActivity
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // ✅ 2️⃣ User already registered → check protection status
        if (protectionStatus == "enabled") {
            // 🔐 Protection ON → go to AuthenticationActivity (biometric / device PIN unlock)
            val intent = Intent(this, AuthenticationActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // 🚀 Protection OFF → directly go to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
