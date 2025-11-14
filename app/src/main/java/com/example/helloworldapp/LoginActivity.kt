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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.helloworldapp.config.AppConfig
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
    private val backendUrl = AppConfig.LOGIN_BACKEND_URL

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
    val prefs = context.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)

    val userId = prefs.getString(AppConfig.KEY_USER_ID, "N/A")
    val deviceId = prefs.getString(AppConfig.KEY_DEVICE_ID, "N/A")

    var sessionId by remember { mutableStateOf("Checking session...") }
    var showProfileDialog by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val cam1Transaction = prefs.getString(AppConfig.KEY_TRANSACTION_CAM1, null)
        val cam2Transaction = prefs.getString(AppConfig.KEY_TRANSACTION_CAM2, null)

        if (cam1Transaction != null || cam2Transaction != null) {
            sessionId = prefs.getString(AppConfig.KEY_SESSION_ID, "N/A") ?: "N/A"
            Toast.makeText(context, AppConfig.TOAST_REUSE_SESSION, Toast.LENGTH_SHORT).show()
        } else if (userId != "N/A" && deviceId != "N/A") {
            loading = true
            val session = createSession(client, backendUrl, userId!!, deviceId!!, context)
            loading = false
            sessionId = session ?: AppConfig.TOAST_SESSION_FAILED
            if (session != null) prefs.edit().putString(AppConfig.KEY_SESSION_ID, session).apply()
        } else {
            sessionId = "Missing user/device info"
        }
    }

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Box(modifier = Modifier.align(Alignment.TopEnd).clickable { showProfileDialog = true }) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.align(Alignment.Center)) {
                Text("Welcome!", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(30.dp))

                Button(onClick = { context.startActivity(Intent(context, DashboardActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Go to Dashboard")
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(onClick = { context.startActivity(Intent(context, CamSelectionActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Go to Detection")
                }

                Spacer(modifier = Modifier.height(30.dp))
                Text("Session ID: $sessionId")
                if (loading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CircularProgressIndicator()
                }
            }

            if (showProfileDialog) {
                val name = prefs.getString(AppConfig.KEY_NAME, "N/A")
                val role = prefs.getString(AppConfig.KEY_ROLE, "N/A")

                AlertDialog(
                    onDismissRequest = { showProfileDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showProfileDialog = false }) { Text("Close") }
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
        .url(backendUrl + AppConfig.ENDPOINT_LOGIN)
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
        backendUrl = AppConfig.LOGIN_BACKEND_URL
    )
}
