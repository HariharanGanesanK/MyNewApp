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

    val name = prefs.getString("name", "N/A") ?: "N/A"
    val role = prefs.getString("role", "N/A") ?: "N/A"
    val userId = prefs.getString("userId", "N/A") ?: "N/A"
    val deviceUniqueId = prefs.getString("deviceId", "N/A") ?: "N/A"

    val storedSessionId = prefs.getString("session_id", null)
    val storedTransactionId = prefs.getString("transaction_id_cam1", "")

    var sessionId by remember { mutableStateOf(storedSessionId ?: "") }
    var currentTransactionId by remember { mutableStateOf(storedTransactionId ?: "") }

    var vehicleNumber by remember { mutableStateOf(TextFieldValue("")) }
    var isDetecting by remember { mutableStateOf(currentTransactionId.isNotEmpty()) }

    // LIVE COUNTS
    var counts by remember { mutableStateOf(mapOf("box" to 0, "bale" to 0, "trolley" to 0)) }

    var pollingJob by remember { mutableStateOf<Job?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }

    // Confirmation dialog trigger
    var showInvalidConfirmDialog by remember { mutableStateOf(false) }

    // Regex for vehicle number verification
    val vehicleNumberRegex = remember {
        Regex(
            "^[A-Z]{2}\\s?\\d{1,2}\\s?[A-Z]{1,3}\\s?\\d{1,4}$"
        )
    }

    // Backend URLs
    val SERVER_IP = "192.168.1.7"
    val START_URL = "http://$SERVER_IP:8000/start"
    val STOP_URL = "http://$SERVER_IP:8000/stop"
    val COUNT_URL = "http://$SERVER_IP:8000/poll_counts"

    // ------------ POLLING FUNCTION ------------
    fun startPollingCounts(sessionId: String, transactionId: String) {
        pollingJob?.cancel() // stop old one if any

        pollingJob = coroutineScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val request = Request.Builder()
                        .url("$COUNT_URL/$sessionId/$transactionId")
                        .get()
                        .build()

                    val response = client.newCall(request).execute()
                    val bodyString = response.body?.string()
                    response.close()

                    if (!response.isSuccessful || bodyString.isNullOrEmpty()) {
                        delay(1000)
                        continue
                    }

                    val json = JSONObject(bodyString)
                    val c = json.getJSONObject("counts")

                    val updatedCounts = mapOf(
                        "box" to c.optInt("box", 0),
                        "bale" to c.optInt("bale", 0),
                        "trolley" to c.optInt("trolley", 0)
                    )

                    withContext(Dispatchers.Main) {
                        counts = updatedCounts
                    }

                } catch (e: Exception) {
                    Log.e("POLL", "Polling failed: ${e.message}")
                }

                delay(1000)
            }
        }
    }
    // ------------ END POLLING FUNCTION ------------

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text("Detection Control - Cam 1", fontSize = 23.sp)

                Spacer(modifier = Modifier.height(12.dp))
                Text("Supervisor: $name", fontSize = 16.sp)

                if (sessionId.isNotEmpty()) Text("Session ID: $sessionId", fontSize = 12.sp)
                if (currentTransactionId.isNotEmpty()) Text("Transaction ID: $currentTransactionId", fontSize = 12.sp)

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = vehicleNumber,
                    onValueChange = {
                        vehicleNumber = it.copy(text = it.text.uppercase())
                    },
                    label = { Text("Vehicle Number") },
                    modifier = Modifier.fillMaxWidth(0.9f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ---------------- Buttons ----------------
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    // START
                    Button(
                        onClick = {
                            if (sessionId.isEmpty()) {
                                Toast.makeText(context, "No session ID found!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (!vehicleNumberRegex.matches(vehicleNumber.text.trim())) {
                                showInvalidConfirmDialog = true
                                return@Button
                            }

                            coroutineScope.launch {
                                val transactionId = UUID.randomUUID().toString()
                                currentTransactionId = transactionId
                                prefs.edit().putString("transaction_id_cam1", transactionId).apply()

                                startDetectionCam1(
                                    context, client, START_URL,
                                    name, role, userId, deviceUniqueId,
                                    vehicleNumber.text, sessionId, transactionId
                                ) { success ->
                                    if (success) {
                                        isDetecting = true
                                        startPollingCounts(sessionId, transactionId)
                                    }
                                }
                            }
                        },
                        enabled = !isDetecting,
                        modifier = Modifier.width(150.dp)
                    ) { Text("Start") }

                    // STOP
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                stopDetectionCam1(context, client, STOP_URL, sessionId, currentTransactionId) {}

                                pollingJob?.cancel()
                                pollingJob = null

                                isDetecting = false
                                counts = mapOf("box" to 0, "bale" to 0, "trolley" to 0)
                                vehicleNumber = TextFieldValue("")
                                currentTransactionId = ""
                                prefs.edit().remove("transaction_id_cam1").apply()

                                Toast.makeText(context, "Detection stopped", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = isDetecting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.width(150.dp)
                    ) { Text("Stop") }
                }
                // -----------------------------------------

                Spacer(modifier = Modifier.height(30.dp))

                if (isDetecting) LiveCountsPanelCam1(counts)
            }

            // INVALID NUMBER CONFIRMATION POPUP
            if (showInvalidConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showInvalidConfirmDialog = false },
                    title = { Text("Invalid Vehicle Format") },
                    text = { Text("This number doesn't match official format. Continue?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showInvalidConfirmDialog = false

                            coroutineScope.launch {
                                val transactionId = UUID.randomUUID().toString()
                                currentTransactionId = transactionId
                                prefs.edit().putString("transaction_id_cam1", transactionId).apply()

                                startDetectionCam1(
                                    context, client, START_URL,
                                    name, role, userId, deviceUniqueId,
                                    vehicleNumber.text, sessionId, transactionId
                                ) { success ->
                                    if (success) {
                                        isDetecting = true
                                        startPollingCounts(sessionId, transactionId)
                                    }
                                }
                            }
                        }) { Text("Yes") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showInvalidConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

// ------------------------------------------
// START CAMERA API CALL
// ------------------------------------------
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
        val ok = response.isSuccessful
        response.close()

        withContext(Dispatchers.Main) {
            if (ok) Toast.makeText(context, "Detection Started!", Toast.LENGTH_SHORT).show()
        }

        onResult(ok)

    } catch (e: Exception) {
        Log.e("START", "Error: ${e.message}")
        onResult(false)
    }
}

// ------------------------------------------
// STOP CAMERA API CALL
// ------------------------------------------
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
        val ok = response.isSuccessful
        response.close()

        withContext(Dispatchers.Main) {
            if (ok) Toast.makeText(context, "Detection Stopped!", Toast.LENGTH_SHORT).show()
        }

        onResult(ok)

    } catch (e: Exception) {
        Log.e("STOP", "Error: ${e.message}")
        onResult(false)
    }
}

// ------------------------------------------
// LIVE COUNTS UI
// ------------------------------------------
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
