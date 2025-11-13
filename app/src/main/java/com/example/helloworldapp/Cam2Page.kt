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

class Cam2Page : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Cam2Screen(context = this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Cam2Screen(context: Context) {
    val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

    val name = prefs.getString("name", "N/A") ?: "N/A"
    val role = prefs.getString("role", "N/A") ?: "N/A"
    val userId = prefs.getString("userId", "N/A") ?: "N/A"
    val deviceUniqueId = prefs.getString("deviceId", "N/A") ?: "N/A"

    val storedSessionId = prefs.getString("session_id", null)
    val storedTransactionId = prefs.getString("transaction_id_cam2", "")

    var sessionId by remember { mutableStateOf(storedSessionId ?: "") }
    var currentTransactionId by remember { mutableStateOf(storedTransactionId ?: "") }

    var vehicleNumber by remember { mutableStateOf(TextFieldValue("")) }
    var isDetecting by remember { mutableStateOf(currentTransactionId.isNotEmpty()) }

    // LIVE COUNTS STATE
    var counts by remember { mutableStateOf(mapOf("bag" to 0, "trolley" to 0)) }

    var pollingJob by remember { mutableStateOf<Job?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }

    // Confirmation dialog for invalid vehicle
    var showInvalidConfirmDialog by remember { mutableStateOf(false) }

    // Vehicle regex
    val vehicleNumberRegex = remember {
        Regex("^[A-Z]{2}\\s?\\d{1,2}\\s?[A-Z]{1,3}\\s?\\d{1,4}$")
    }

    // Backend URLs (Cam2 uses PORT 8001)
    val SERVER_IP = "192.168.1.7"
    val START_URL = "http://$SERVER_IP:8001/start"
    val STOP_URL = "http://$SERVER_IP:8001/stop"
    val COUNT_URL = "http://$SERVER_IP:8001/poll_counts"

    // ------------ POLLING FUNCTION ------------
    fun startPollingCounts(sessionId: String, transactionId: String) {
        pollingJob?.cancel()

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
                        "bag" to c.optInt("bag", 0),
                        "trolley" to c.optInt("trolley", 0)
                    )

                    withContext(Dispatchers.Main) { counts = updatedCounts }

                } catch (e: Exception) {
                    Log.e("POLL_CAM2", "Polling error: ${e.message}")
                }

                delay(1000) // poll every second
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
                Text("Detection Control - Cam 2", fontSize = 23.sp)

                Spacer(modifier = Modifier.height(12.dp))
                Text("Supervisor: $name", fontSize = 16.sp)

                if (sessionId.isNotEmpty()) Text("Session ID: $sessionId", fontSize = 12.sp)
                if (currentTransactionId.isNotEmpty()) Text("Transaction ID: $currentTransactionId", fontSize = 12.sp)

                Spacer(modifier = Modifier.height(24.dp))



                OutlinedTextField(
                    value = vehicleNumber,
                    onValueChange = { newVal ->
                        vehicleNumber = newVal.copy(text = newVal.text.uppercase())
                    },
                    label = { Text("Vehicle Number") },
                    modifier = Modifier.fillMaxWidth(0.9f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // -------------------------------------------------------
                // BUTTONS
                // -------------------------------------------------------
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    // START BUTTON
                    Button(
                        onClick = {
                            if (sessionId.isEmpty()) {
                                Toast.makeText(context, "No session ID!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (!vehicleNumberRegex.matches(vehicleNumber.text.trim())) {
                                showInvalidConfirmDialog = true
                                return@Button
                            }

                            coroutineScope.launch {
                                val txn = UUID.randomUUID().toString()
                                currentTransactionId = txn

                                prefs.edit().putString("transaction_id_cam2", txn).apply()

                                startDetectionCam2(
                                    context, client, START_URL,
                                    name, role, userId, deviceUniqueId,
                                    vehicleNumber.text, sessionId, txn
                                ) { ok ->
                                    if (ok) {
                                        isDetecting = true
                                        startPollingCounts(sessionId, txn)
                                    }
                                }
                            }
                        },
                        enabled = !isDetecting,
                        modifier = Modifier.width(150.dp)
                    ) { Text("Start") }

                    // STOP BUTTON
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                stopDetectionCam2(context, client, STOP_URL, sessionId, currentTransactionId) {}

                                pollingJob?.cancel()
                                pollingJob = null

                                delay(1000)

                                isDetecting = false
                                counts = mapOf("bag" to 0, "trolley" to 0)
                                vehicleNumber = TextFieldValue("")
                                currentTransactionId = ""
                                prefs.edit().remove("transaction_id_cam2").apply()

                                Toast.makeText(context, "Detection stopped", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = isDetecting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.width(150.dp)
                    ) { Text("Stop") }
                }

                if (isDetecting) {
                    Spacer(modifier = Modifier.height(30.dp))
                    LiveCountsPanelCam2(counts)
                }
            }

            // CONFIRMATION DIALOG
            if (showInvalidConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showInvalidConfirmDialog = false },
                    title = { Text("Invalid Vehicle Format") },
                    text = { Text("Vehicle number looks incorrect. Continue anyway?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showInvalidConfirmDialog = false

                            coroutineScope.launch {
                                val txn = UUID.randomUUID().toString()
                                currentTransactionId = txn
                                prefs.edit().putString("transaction_id_cam2", txn).apply()

                                startDetectionCam2(
                                    context, client, START_URL,
                                    name, role, userId, deviceUniqueId,
                                    vehicleNumber.text, sessionId, txn
                                ) { ok ->
                                    if (ok) {
                                        isDetecting = true
                                        startPollingCounts(sessionId, txn)
                                    }
                                }
                            }
                        }) { Text("Yes") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showInvalidConfirmDialog = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------
// START DETECTION API (CAM 2)
// -------------------------------------------------------
suspend fun startDetectionCam2(
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
            put("video_url", "cam_2")
            put("session_id", sessionId)
            put("transaction_id", transactionId)
        }

        val response = client.newCall(
            Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
        ).execute()

        val ok = response.isSuccessful
        response.close()

        withContext(Dispatchers.Main) {
            if (ok) Toast.makeText(context, "Detection started (Cam 2)", Toast.LENGTH_SHORT).show()
        }

        onResult(ok)

    } catch (e: Exception) {
        Log.e("Cam2Start", "Error: ${e.message}")
        onResult(false)
    }
}

// -------------------------------------------------------
// STOP DETECTION API (CAM 2)
// -------------------------------------------------------
suspend fun stopDetectionCam2(
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

        val response = client.newCall(
            Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
        ).execute()

        val ok = response.isSuccessful
        response.close()

        withContext(Dispatchers.Main) {
            if (ok) Toast.makeText(context, "Detection stopped (Cam 2)", Toast.LENGTH_SHORT).show()
        }

        onResult(ok)

    } catch (e: Exception) {
        Log.e("Cam2Stop", "Error: ${e.message}")
        onResult(false)
    }
}

// -------------------------------------------------------
// LIVE COUNTS UI PANEL
// -------------------------------------------------------
@Composable
fun LiveCountsPanelCam2(counts: Map<String, Int>) {
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

            Text("👜 Bag: ${counts["bag"]}")
            Text("🛒 Trolley: ${counts["trolley"]}")
        }
    }
}
