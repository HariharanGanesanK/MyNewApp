package com.example.helloworldapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class RegistrationActivity : ComponentActivity() {
    private val client = OkHttpClient()
    private val backendUrl = "http://192.168.1.7:9000" // 👈 Replace with your FastAPI backend URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RegistrationScreen(context = this, client = client, backendUrl = backendUrl)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(context: Context, client: OkHttpClient, backendUrl: String) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var otpStage by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }

    val roles = listOf("MD", "JMD", "GM", "AGM", "IT HEAD", "SUPERVISOR")
    var expanded by remember { mutableStateOf(false) }

    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    val scope = rememberCoroutineScope()

    // ✅ Password validation
    fun isValidPassword(pwd: String): Boolean {
        val passwordRegex =
            Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$")
        return passwordRegex.matches(pwd)
    }

    // ✅ Email validation
    fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
        return emailRegex.matches(email)
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (!otpStage) "User Registration" else "Enter OTP",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (!otpStage) {
                    // --- Name ---
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- Role Dropdown ---
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = role,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Role") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            roles.forEach { selectedRole ->
                                DropdownMenuItem(
                                    text = { Text(selectedRole) },
                                    onClick = {
                                        role = selectedRole
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- Email (optional, but validated if entered) ---
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = if (email.isNotEmpty() && !isValidEmail(it)) {
                                "Invalid email format"
                            } else {
                                ""
                            }
                        },
                        label = { Text("Email (optional)") },
                        isError = emailError.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (emailError.isNotEmpty()) {
                        Text(
                            text = emailError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- Password ---
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = if (it.isEmpty()) {
                                ""
                            } else if (!isValidPassword(it)) {
                                "Password must be 8+ chars, with upper, lower, number & special char"
                            } else {
                                ""
                            }
                        },
                        label = { Text("Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val icon =
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        isError = passwordError.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (passwordError.isNotEmpty()) {
                        Text(
                            text = passwordError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(15.dp))

                    Text(text = "Device ID: $deviceId", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Register Button ---
                    Button(
                        onClick = {
                            when {
                                name.isEmpty() || role.isEmpty() || password.isEmpty() -> {
                                    Toast.makeText(
                                        context,
                                        "Please fill all required fields",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                email.isNotEmpty() && !isValidEmail(email) -> {
                                    Toast.makeText(
                                        context,
                                        "❌ Invalid email format",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                !isValidPassword(password) -> {
                                    Toast.makeText(
                                        context,
                                        "❌ Weak password! Use 8+ chars with upper, lower, number & special symbol",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                else -> {
                                    loading = true
                                    scope.launch {
                                        val success = registerUser(
                                            context,
                                            client,
                                            backendUrl,
                                            name,
                                            role,
                                            password,
                                            email,
                                            deviceId
                                        )
                                        loading = false
                                        if (success) otpStage = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
                    ) {
                        Text(if (loading) "Registering..." else "Register")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- Test Backend Connection ---
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val req = Request.Builder().url("$backendUrl/hello").build()
                                    val res =
                                        withContext(Dispatchers.IO) { client.newCall(req).execute() }
                                    val msg = res.body?.string() ?: "No response"
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Backend says: $msg",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Error: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Backend Connection")
                    }
                } else {
                    // --- OTP Input ---
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { otp = it },
                        label = { Text("Enter OTP") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Verify OTP Button ---
                    Button(
                        onClick = {
                            if (otp.isEmpty()) {
                                Toast.makeText(context, "Please enter OTP", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                loading = true
                                scope.launch {
                                    val verified = verifyOtp(
                                        context,
                                        client,
                                        backendUrl,
                                        otp,
                                        name,
                                        role,
                                        password,
                                        email,
                                        deviceId
                                    )
                                    loading = false
                                    if (verified) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "✅ Registration successful! Set up app protection next.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            val intent = Intent(
                                                context,
                                                ProtectionSetupActivity::class.java
                                            )
                                            context.startActivity(intent)
                                            if (context is ComponentActivity) {
                                                context.overridePendingTransition(
                                                    android.R.anim.fade_in,
                                                    android.R.anim.fade_out
                                                )
                                                context.finish()
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
                    ) {
                        Text(if (loading) "Verifying..." else "Verify OTP")
                    }
                }
            }
        }
    }
}

// --------------------------- BACKEND CALLS -----------------------------

suspend fun registerUser(
    context: Context,
    client: OkHttpClient,
    backendUrl: String,
    name: String,
    role: String,
    password: String,
    email: String,
    deviceId: String
): Boolean = withContext(Dispatchers.IO) {
    val json = JSONObject().apply {
        put("name", name)
        put("role", role)
        put("password", password)
        put("device_unique_id", deviceId)
        put("company_name", "JLMILLS")
        put("branch", "Rajapalayam")
        put("sub_branch", "None")
        put("mail", if (email.isEmpty()) "none" else email)
    }

    val request = Request.Builder()
        .url("$backendUrl/register")
        .post(json.toString().toRequestBody("application/json".toMediaType()))
        .build()

    try {
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        val jsonResponse = JSONObject(body)
        response.close()

        if (response.isSuccessful && jsonResponse.optString("status") == "pending") {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "OTP Sent to approver!", Toast.LENGTH_SHORT).show()
            }
            true
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Registration failed: ${jsonResponse.optString("message")}",
                    Toast.LENGTH_LONG
                ).show()
            }
            false
        }
    } catch (e: IOException) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
        }
        false
    }
}

suspend fun verifyOtp(
    context: Context,
    client: OkHttpClient,
    backendUrl: String,
    otp: String,
    name: String,
    role: String,
    password: String,
    email: String,
    deviceId: String
): Boolean = withContext(Dispatchers.IO) {
    val registrationData = JSONObject().apply {
        put("name", name)
        put("role", role)
        put("password", password)
        put("device_unique_id", deviceId)
        put("company_name", "JLMILLS")
        put("branch", "Rajapalayam")
        put("sub_branch", "None")
        put("mail", if (email.isEmpty()) "none" else email)
    }

    val payload = JSONObject().apply {
        put("otp", otp)
        put("registration_data", registrationData)
    }

    val request = Request.Builder()
        .url("$backendUrl/verify_otp")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()

    try {
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        val jsonResponse = JSONObject(body)
        response.close()

        if (response.isSuccessful && jsonResponse.optString("message") == "User registered successfully") {
            val userId = jsonResponse.optString("user_id", "unknown")

            // ✅ Save to SharedPreferences
            val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("name", name)
                putString("role", role)
                putString("email", email)
                putString("deviceId", deviceId)
                putString("userId", userId)
                apply()
            }

            true
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Invalid OTP or verification failed", Toast.LENGTH_LONG)
                    .show()
            }
            false
        }
    } catch (e: IOException) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
        }
        false
    }
}
