package com.example.helloworldapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*

class Cam1Page : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Cam1Screen(context = this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Cam1Screen(context: Context) {
    val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

    // 🔧 Load user metadata
    val name = prefs.getString("name", "N/A") ?: "N/A"
    val role = prefs.getString("role", "N/A") ?: "N/A"
    val userId = prefs.getString("userId", "N/A") ?: "N/A"
    val deviceUniqueId = prefs.getString("deviceId", "N/A") ?: "N/A"

    // ✅ Get backend-generated session_id (from LoginActivity)
    val storedSessionId = prefs.getString("session_id", null)
    val storedTransactionId = prefs.getString("transaction_id_cam1", "")

    var sessionId by remember { mutableStateOf(storedSessionId ?: "") }
    var currentTransactionId by remember { mutableStateOf(storedTransactionId ?: "") }

    // ⚙️ UI State
    var vehicleNumber by remember { mutableStateOf(TextFieldValue("")) }
    var isDetecting by remember { mutableStateOf(currentTransactionId.isNotEmpty()) }
    var counts by remember { mutableStateOf(mapOf("box" to 0, "bale" to 0, "trolley" to 0)) }

    val coroutineScope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }

    // 🌐 Backend API endpoints
    val SERVER_IP = "192.168.1.7"
    val START_URL = "http://$SERVER_IP:8000/start"
    val STOP_URL = "http://$SERVER_IP:8000/stop"
    val COUNT_URL = "http://$SERVER_IP:8000/poll_counts"

    // 🔁 Polling loop for live counts
    val detectingState = rememberUpdatedState(isDetecting)

    LaunchedEffect(sessionId, currentTransactionId) {
        if (sessionId.isEmpty()) {
            Log.e("Cam1Page", "No valid session ID found — please log in first.")
            return@LaunchedEffect
        }

        var stopPolling = false
        while (!stopPolling) {
            if (detectingState.value && currentTransactionId.isNotEmpty()) {
                try {
                    withContext(Dispatchers.IO) {
                        val request = Request.Builder()
                            .url("$COUNT_URL/$sessionId/$currentTransactionId")
                            .get()
                            .build()

                        val response = client.newCall(request).execute()
                        val body = response.body?.string()

                        if (response.isSuccessful && body != null) {
                            val json = JSONObject(body)
                            val jsonCounts = json.optJSONObject("counts")

                            if (jsonCounts != null) {
                                val newCounts = mapOf(
                                    "box" to jsonCounts.optInt("box", 0),
                                    "bale" to jsonCounts.optInt("bale", 0),
                                    "trolley" to jsonCounts.optInt("trolley", 0)
                                )

                                if (newCounts != counts) {
                                    withContext(Dispatchers.Main) {
                                        counts = newCounts
                                        Log.d("Polling", "Counts updated: $counts")
                                    }
                                }
                            }
                        } else if (response.code == 404) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Session not found or ended.", Toast.LENGTH_SHORT).show()
                            }
                            stopPolling = true
                        }

                        response.close()
                    }
                } catch (e: Exception) {
                    Log.e("Polling", "Error: ${e.message}")
                }
            }
            if (stopPolling) break
            delay(1000) // 1-second delay between polls
        }
    }

    // 🚀 UI Layout
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "YOLOv5 Detection Control - Cam 1",
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Supervisor: $name ($role)", fontSize = 16.sp)

                if (sessionId.isNotEmpty())
                    Text("Session ID: $sessionId", fontSize = 14.sp)
                else
                    Text("⚠️ No session ID found — please log in first", fontSize = 14.sp)

                if (currentTransactionId.isNotEmpty())
                    Text("Transaction ID: $currentTransactionId", fontSize = 14.sp)

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = vehicleNumber,
                    onValueChange = { vehicleNumber = it },
                    label = { Text("Vehicle Number") },
                    modifier = Modifier.fillMaxWidth(0.9f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // ▶️ START Button
                    Button(
                        onClick = {
                            if (sessionId.isEmpty()) {
                                Toast.makeText(context, "No session ID found. Please log in again.", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (vehicleNumber.text.isBlank()) {
                                Toast.makeText(context, "Enter vehicle number first!", Toast.LENGTH_SHORT).show()
                            } else {
                                coroutineScope.launch {
                                    val transactionId = UUID.randomUUID().toString()
                                    currentTransactionId = transactionId

                                    prefs.edit()
                                        .putString("transaction_id_cam1", transactionId)
                                        .apply()

                                    startDetectionCam1(
                                        context, client, START_URL,
                                        name, role, userId, deviceUniqueId,
                                        vehicleNumber.text, sessionId, transactionId
                                    ) { success ->
                                        if (success) isDetecting = true
                                    }
                                }
                            }
                        },
                        enabled = !isDetecting,
                        modifier = Modifier.width(150.dp)
                    ) {
                        Text("Start")
                    }

                    // ⏹️ STOP Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                stopDetectionCam1(context, client, STOP_URL, sessionId, currentTransactionId) { success ->
                                    if (success) {
                                        isDetecting = false
                                        counts = mapOf("box" to 0, "bale" to 0, "trolley" to 0)
                                        vehicleNumber = TextFieldValue("")
                                        currentTransactionId = ""
                                        prefs.edit().remove("transaction_id_cam1").apply()
                                    }
                                }
                            }
                        },
                        enabled = isDetecting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.width(150.dp)
                    ) {
                        Text("Stop")
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                if (isDetecting) {
                    LiveCountsPanelCam1(counts)
                }
            }
        }
    }
}

// ✅ Start Detection API Call (Cam 1)
suspend fun startDetectionCam1(
    context: Context,
    client: OkHttpClient,
    url: String,
    name: String,
    role: String,
    userId: String,
    deviceUniqueId: String,
    vehicleNumber: String,
    sessionId: String,
    transactionId: String,
    onResult: (Boolean) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val payload = JSONObject().apply {
            put("name", name)
            put("role", role)
            put("user_id", userId)
            put("device_unique_id", deviceUniqueId)
            put("vehicle_number", vehicleNumber)
            put("video_url", "cam_1")
            put("session_id", sessionId)
            put("transaction_id", transactionId)
        }

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string()
        response.close()

        if (response.isSuccessful) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Detection started (Cam 1)!", Toast.LENGTH_SHORT).show()
            }
            onResult(true)
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Start failed: $body", Toast.LENGTH_LONG).show()
            }
            onResult(false)
        }
    } catch (e: IOException) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
        }
        onResult(false)
    }
}

// ✅ Stop Detection API Call (Cam 1) — now includes transaction_id
suspend fun stopDetectionCam1(
    context: Context,
    client: OkHttpClient,
    url: String,
    sessionId: String,
    transactionId: String,
    onResult: (Boolean) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val payload = JSONObject().apply {
            put("session_id", sessionId)
            put("transaction_id", transactionId)
        }

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string()
        response.close()

        if (response.isSuccessful) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Detection stopped (Cam 1)!", Toast.LENGTH_SHORT).show()
            }
            onResult(true)
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Stop failed: $body", Toast.LENGTH_LONG).show()
            }
            onResult(false)
        }
    } catch (e: IOException) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
        }
        onResult(false)
    }
}

// ✅ Live Counts UI (Cam 1)
@Composable
fun LiveCountsPanelCam1(counts: Map<String, Int>) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(top = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📊 Live Object Counts", fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("📦 Box: ${counts["box"]}")
            Text("🧵 Bale: ${counts["bale"]}")
            Text("🛒 Trolley: ${counts["trolley"]}")
        }
    }
}
