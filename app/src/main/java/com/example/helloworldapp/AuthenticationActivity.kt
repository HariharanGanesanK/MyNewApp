package com.example.helloworldapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

class AuthenticationActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val protection = prefs.getString("protection", "disabled")

        if (protection == "enabled") {
            authenticateUser()
        } else {
            // 🔓 If protection disabled → directly go to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun authenticateUser() {
        val executor: Executor = ContextCompat.getMainExecutor(this)
        val biometricManager = BiometricManager.from(this)

        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(
                this,
                "⚠️ No biometric or device lock set up. Redirecting...",
                Toast.LENGTH_SHORT
            ).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // ✅ Create BiometricPrompt instance
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(
                    applicationContext,
                    "✅ Authentication Successful!",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(Intent(this@AuthenticationActivity, LoginActivity::class.java))
                finish()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(
                    applicationContext,
                    "❌ Error: $errString",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(
                    applicationContext,
                    "⚠️ Authentication failed! Try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        // ✅ Build prompt info (new method for Android 11+)
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock App")
            .setSubtitle("Authenticate using your fingerprint, face, or device PIN")

        // ⚙️ Use new API if supported, else fallback to deprecated method
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // ✅ Android 11+ (API 30+)
            promptInfoBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            // ⚙️ Android 10 and below
            @Suppress("DEPRECATION")
            promptInfoBuilder.setDeviceCredentialAllowed(true)
        }

        val promptInfo = promptInfoBuilder.build()

        // ✅ Show authentication dialog
        prompt.authenticate(promptInfo)
    }
}
