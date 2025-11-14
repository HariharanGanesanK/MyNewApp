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
import com.example.helloworldapp.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class RegistrationActivity : ComponentActivity() {
    private val client = OkHttpClient()
    private val backendUrl = AppConfig.BACKEND_URL

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

    val roles = AppConfig.ROLES
    var expanded by remember { mutableStateOf(false) }

    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    val scope = rememberCoroutineScope()

    fun isValidPassword(pwd: String): Boolean = AppConfig.PASSWORD_REGEX.matches(pwd)
    fun isValidEmail(email: String): Boolean = AppConfig.EMAIL_REGEX.matches(email)

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(text = if (!otpStage) "User Registration" else "Enter OTP", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(20.dp))

                if (!otpStage) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))

                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(value = role, onValueChange = {}, readOnly = true, label = { Text("Select Role") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            roles.forEach { selectedRole ->
                                DropdownMenuItem(text = { Text(selectedRole) }, onClick = { role = selectedRole; expanded = false })
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(value = email, onValueChange = {
                        email = it
                        emailError = if (email.isNotEmpty() && !isValidEmail(it)) "Invalid email format" else ""
                    }, label = { Text("Email (optional)") }, isError = emailError.isNotEmpty(), modifier = Modifier.fillMaxWidth())

                    if (emailError.isNotEmpty()) Text(text = emailError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(value = password, onValueChange = {
                        password = it
                        passwordError = if (it.isNotEmpty() && !isValidPassword(it)) "Password must be 8+ chars, upper, lower, number & symbol" else ""
                    }, label = { Text("Password") }, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                        }
                    }, isError = passwordError.isNotEmpty(), modifier = Modifier.fillMaxWidth())

                    if (passwordError.isNotEmpty()) Text(text = passwordError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(15.dp))
                    Text("Device ID: $deviceId", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(20.dp))

                    Button(onClick = {
                        when {
                            name.isEmpty() || role.isEmpty() || password.isEmpty() -> Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                            email.isNotEmpty() && !isValidEmail(email) -> Toast.makeText(context, "Invalid email format", Toast.LENGTH_SHORT).show()
                            !isValidPassword(password) -> Toast.makeText(context, "Weak password!", Toast.LENGTH_LONG).show()
                            else -> {
                                loading = true
                                scope.launch {
                                    val success = registerUser(context, client, backendUrl, name, role, password, email, deviceId)
                                    loading = false
                                    if (success) otpStage = true
                                }
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth(), enabled = !loading) {
                        Text(if (loading) "Registering..." else "Register")
                    }

                    Spacer(Modifier.height(10.dp))

                    Button(onClick = {
                        scope.launch {
                            try {
                                val req = Request.Builder().url(backendUrl + AppConfig.ENDPOINT_HELLO).build()
                                val res = withContext(Dispatchers.IO) { client.newCall(req).execute() }
                                val msg = res.body?.string() ?: "No response"
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Test Backend Connection")
                    }

                } else {
                    OutlinedTextField(value = otp, onValueChange = { otp = it }, label = { Text("Enter OTP") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = {
                        if (otp.isEmpty()) {
                            Toast.makeText(context, "Enter OTP", Toast.LENGTH_SHORT).show()
                        } else {
                            loading = true
                            scope.launch {
                                val verified = verifyOtp(context, client, backendUrl, otp, name, role, password, email, deviceId)
                                loading = false
                                if (verified) {
                                    context.startActivity(Intent(context, ProtectionSetupActivity::class.java))
                                    if (context is ComponentActivity) context.finish()
                                }
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth(), enabled = !loading) {
                        Text(if (loading) "Verifying..." else "Verify OTP")
                    }
                }
            }
        }
    }
}

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
        put("company_name", AppConfig.COMPANY_NAME)
        put("branch", AppConfig.BRANCH)
        put("sub_branch", AppConfig.SUB_BRANCH)
        put("mail", if (email.isEmpty()) "none" else email)
    }

    val request = Request.Builder()
        .url(backendUrl + AppConfig.ENDPOINT_REGISTER)
        .post(json.toString().toRequestBody("application/json".toMediaType()))
        .build()

    try {
        val response = client.newCall(request).execute()
        val jsonResponse = JSONObject(response.body?.string() ?: "")
        response.close()

        if (response.isSuccessful && jsonResponse.optString("status") == "pending") {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "OTP Sent to approver!", Toast.LENGTH_SHORT).show()
            }
            return@withContext true
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, jsonResponse.optString("message"), Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    false
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
        put("company_name", AppConfig.COMPANY_NAME)
        put("branch", AppConfig.BRANCH)
        put("sub_branch", AppConfig.SUB_BRANCH)
        put("mail", if (email.isEmpty()) "none" else email)
    }

    val payload = JSONObject().apply {
        put("otp", otp)
        put("registration_data", registrationData)
    }

    val request = Request.Builder()
        .url(backendUrl + AppConfig.ENDPOINT_VERIFY_OTP)
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()

    try {
        val response = client.newCall(request).execute()
        val jsonResponse = JSONObject(response.body?.string() ?: "")
        response.close()

        if (response.isSuccessful && jsonResponse.optString("message") == "User registered successfully") {
            val userId = jsonResponse.optString("user_id", "unknown")
            val prefs = context.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString(AppConfig.KEY_NAME, name)
                putString(AppConfig.KEY_ROLE, role)
                putString(AppConfig.KEY_EMAIL, email)
                putString(AppConfig.KEY_DEVICE_ID, deviceId)
                putString(AppConfig.KEY_USER_ID, userId)
                apply()
            }
            return@withContext true
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    false
}
