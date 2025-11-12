package com.example.helloworldapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : ComponentActivity() {
    private val client = OkHttpClient()
    private val backendUrl = "http://192.168.1.7:9010"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginHomeScreen(context = this, client = client, backendUrl = backendUrl)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginHomeScreen(context: Context, client: OkHttpClient, backendUrl: String) {
    val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    val userId = prefs.getString("userId", "N/A")
    val deviceId = prefs.getString("deviceId", "N/A")
    var sessionId by remember { mutableStateOf("Checking session...") }
    var showProfileDialog by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    // 🧠 Check for both Cam1 and Cam2 transaction IDs
    LaunchedEffect(Unit) {
        val cam1Transaction = prefs.getString("transaction_id_cam1", null)
        val cam2Transaction = prefs.getString("transaction_id_cam2", null)

        if (cam1Transaction != null || cam2Transaction != null) {
            // ✅ Active detection on either Cam1 or Cam2 — reuse old session
            val savedSession = prefs.getString("session_id", "N/A")
            sessionId = savedSession ?: "N/A"
            Toast.makeText(context, "Reusing existing session (active detection running)", Toast.LENGTH_SHORT).show()
        } else if (userId != "N/A" && deviceId != "N/A") {
            // 🆕 No detection active — create new session
            loading = true
            val session = createSession(client, backendUrl, userId!!, deviceId!!, context)
            loading = false
            sessionId = session ?: "Failed to generate session"
            if (session != null) prefs.edit().putString("session_id", session).apply()
        } else {
            sessionId = "Missing user/device info"
        }
    }

    // 🌈 UI Layout
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // 👤 Profile Icon (Top Right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable { showProfileDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            }

            // 🧭 Main Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text("Welcome!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = {
                        val intent = Intent(context, DashboardActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go to Dashboard")
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val intent = Intent(context, CamSelectionActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go to Detection")
                }

                Spacer(modifier = Modifier.height(30.dp))
                Text("Session ID: $sessionId", style = MaterialTheme.typography.bodyMedium)
                if (loading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CircularProgressIndicator()
                }
            }

            // 💬 Profile popup
            if (showProfileDialog) {
                val name = prefs.getString("name", "N/A")
                val role = prefs.getString("role", "N/A")

                AlertDialog(
                    onDismissRequest = { showProfileDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showProfileDialog = false }) {
                            Text("Close")
                        }
                    },
                    title = { Text("User Info") },
                    text = {
                        Column {
                            Text("👤 Name: $name")
                            Text("🧩 Role: $role")
                            Text("🆔 User ID: $userId")
                        }
                    }
                )
            }
        }
    }
}

// -----------------------------------------------------------
// 🔌 Backend Communication - Create Session
// -----------------------------------------------------------
suspend fun createSession(
    client: OkHttpClient,
    backendUrl: String,
    userId: String,
    deviceId: String,
    context: Context
): String? = withContext(Dispatchers.IO) {
    val json = JSONObject().apply {
        put("user_id", userId)
        put("device_id", deviceId)
    }

    val request = Request.Builder()
        .url("$backendUrl/api/auth/login")
        .post(json.toString().toRequestBody("application/json".toMediaType()))
        .build()

    try {
        val response = client.newCall(request).execute()
        val body = response.body?.string()
        val jsonResponse = JSONObject(body ?: "{}")
        response.close()

        return@withContext if (jsonResponse.optBoolean("success", false)) {
            val session = jsonResponse.getJSONObject("session").getString("session_id")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Session created: $session", Toast.LENGTH_SHORT).show()
            }
            session
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Login failed: ${jsonResponse.optString("message")}", Toast.LENGTH_LONG).show()
            }
            null
        }
    } catch (e: IOException) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
        }
        null
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    LoginHomeScreen(
        context = androidx.compose.ui.platform.LocalContext.current,
        client = OkHttpClient(),
        backendUrl = "http://192.168.1.7:9010"
    )
}
