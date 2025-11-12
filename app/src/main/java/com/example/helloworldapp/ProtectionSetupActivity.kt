package com.example.helloworldapp

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

class ProtectionSetupActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Use ComposeView manually for Compose inside FragmentActivity
        val composeView = ComposeView(this)
        composeView.setContent {
            ProtectionSetupScreen(this)
        }

        setContentView(composeView)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectionSetupScreen(context: Context) {
    val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    var protectionStatus by remember { mutableStateOf(prefs.getString("protection", "disabled")) }

    val executor: Executor = ContextCompat.getMainExecutor(context)
    val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    val biometricManager = BiometricManager.from(context)

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Device Protection Settings",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "Current status: ${protectionStatus?.uppercase()}")

                Spacer(modifier = Modifier.height(40.dp))

                // ✅ Enable Protection Button
                Button(
                    onClick = {
                        // Check if device has any security setup
                        if (!keyguardManager.isDeviceSecure) {
                            Toast.makeText(
                                context,
                                "Please set a device PIN, pattern, or password first!",
                                Toast.LENGTH_LONG
                            ).show()
                            context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                            return@Button
                        }

                        val canAuth = biometricManager.canAuthenticate(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        )

                        when (canAuth) {
                            BiometricManager.BIOMETRIC_SUCCESS -> {
                                val prompt = BiometricPrompt(
                                    context as FragmentActivity,
                                    executor,
                                    object : BiometricPrompt.AuthenticationCallback() {
                                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                            super.onAuthenticationSucceeded(result)

                                            // ✅ Save protection enabled
                                            prefs.edit().putString("protection", "enabled").apply()
                                            protectionStatus = "enabled"

                                            Toast.makeText(
                                                context,
                                                "✅ Protection enabled successfully!",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            // ✅ Add small delay (1 second) before navigating
                                            Handler(Looper.getMainLooper()).postDelayed({
                                                val intent = Intent(context, MainActivity::class.java)
                                                context.startActivity(intent)
                                                (context as FragmentActivity).finish()
                                            }, 1000)
                                        }

                                        override fun onAuthenticationError(
                                            errorCode: Int,
                                            errString: CharSequence
                                        ) {
                                            super.onAuthenticationError(errorCode, errString)
                                            Toast.makeText(
                                                context,
                                                "Error: $errString",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )

                                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("Enable App Protection")
                                    .setSubtitle("Confirm using your fingerprint, face, or PIN")
                                    // ✅ Use modern method for Android 11+
                                    .setAllowedAuthenticators(
                                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                    )
                                    .build()

                                prompt.authenticate(promptInfo)
                            }

                            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                                Toast.makeText(
                                    context,
                                    "❌ Your device doesn’t support biometrics or PIN protection. Kindly press Disable.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                                Toast.makeText(
                                    context,
                                    "⚠️ No biometric or PIN registered. Please set up device security in Settings.",
                                    Toast.LENGTH_LONG
                                ).show()
                                context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                            }

                            else -> {
                                Toast.makeText(
                                    context,
                                    "⚠️ Your phone doesn’t support biometric authentication. Kindly press Disable.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable Protection")
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ❌ Disable Protection Button
                Button(
                    onClick = {
                        prefs.edit().putString("protection", "disabled").apply()
                        protectionStatus = "disabled"
                        Toast.makeText(context, "Protection disabled!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                        (context as FragmentActivity).finish()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Disable Protection")
                }
            }
        }
    }
}
