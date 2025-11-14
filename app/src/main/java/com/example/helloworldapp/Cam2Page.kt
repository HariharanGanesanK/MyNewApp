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
import com.example.helloworldapp.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
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
    val prefs = context.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)

    val name = prefs.getString(AppConfig.KEY_NAME, "N/A") ?: "N/A"
    val role = prefs.getString(AppConfig.KEY_ROLE, "N/A") ?: "N/A"
    val userId = prefs.getString(AppConfig.KEY_USER_ID, "N/A") ?: "N/A"
    val deviceUniqueId = prefs.getString(AppConfig.KEY_DEVICE_ID, "N/A") ?: "N/A"

    val storedSessionId = prefs.getString(AppConfig.KEY_SESSION_ID, null)
    val storedTransactionId = prefs.getString(AppConfig.KEY_TRANSACTION_CAM2, "")

    var sessionId by remember { mutableStateOf(storedSessionId ?: "") }
    var currentTransactionId by remember { mutableStateOf(storedTransactionId ?: "") }

    var vehicleNumber by remember { mutableStateOf(TextFieldValue("")) }
    var isDetecting by remember { mutableStateOf(currentTransactionId.isNotEmpty()) }

    var counts by remember { mutableStateOf(mapOf("bag" to 0, "trolley" to 0)) }

    val coroutineScope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }

    var showInvalidConfirmDialog by remember { mutableStateOf(false) }

    val vehicleNumberRegex = remember { Regex("^[A-Z]{2}\\s?\\d{1,2}\\s?[A-Z]{1,3}\\s?\\d{1,4}$") }

    val SERVER_IP = AppConfig.CAM2_SERVER_IP
    val START_URL = AppConfig.CAM2_START_URL
    val STOP_URL = AppConfig.CAM2_STOP_URL

    val mqttHelper = remember { MqttHelper(SERVER_IP) }

    LaunchedEffect(isDetecting) {
        if (isDetecting) {
            val topic = "jlmill/sessions/$sessionId/$currentTransactionId/counts_cam2"
            Log.d("MQTT", "Subscribing to: $topic")

            mqttHelper.connect {
                mqttHelper.subscribe(topic) { message ->
                    try {
                        val json = JSONObject(message)
                        val c = json.getJSONObject("counts")
                        counts = mapOf("bag" to c.optInt("bag", 0), "trolley" to c.optInt("trolley", 0))
                    } catch (e: Exception) {
                        Log.e("MQTT", "CAM2 parse error: ${e.message}")
                    }
                }
            }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(AppConfig.CAM2_TITLE, fontSize = 23.sp)
                Spacer(Modifier.height(12.dp))
                Text("${AppConfig.CAM2_SUPERVISOR_LABEL}: $name", fontSize = 16.sp)

                if (sessionId.isNotEmpty()) Text("${AppConfig.CAM2_SESSION_LABEL}: $sessionId", fontSize = 12.sp)
                if (currentTransactionId.isNotEmpty()) Text("${AppConfig.CAM2_TRANSACTION_LABEL}: $currentTransactionId", fontSize = 12.sp)

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(value = vehicleNumber, onValueChange = { vehicleNumber = it.copy(text = it.text.uppercase()) }, label = { Text(AppConfig.CAM2_VEHICLE_LABEL) }, modifier = Modifier.fillMaxWidth(0.9f))

                Spacer(Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = {
                        if (sessionId.isEmpty()) { Toast.makeText(context, AppConfig.CAM2_TOAST_NO_SESSION, Toast.LENGTH_SHORT).show(); return@Button }
                        if (!vehicleNumberRegex.matches(vehicleNumber.text.trim())) { showInvalidConfirmDialog = true; return@Button }

                        coroutineScope.launch {
                            val transactionId = UUID.randomUUID().toString()
                            currentTransactionId = transactionId
                            prefs.edit().putString(AppConfig.KEY_TRANSACTION_CAM2, transactionId).apply()

                            startDetectionCam2(context, client, START_URL, name, role, userId, deviceUniqueId, vehicleNumber.text, sessionId, transactionId) { success ->
                                if (success) isDetecting = true
                            }
                        }
                    }, enabled = !isDetecting, modifier = Modifier.width(150.dp)) { Text(AppConfig.CAM2_BUTTON_START) }

                    Button(onClick = {
                        coroutineScope.launch {
                            stopDetectionCam2(context, client, STOP_URL, sessionId, currentTransactionId) {}
                            isDetecting = false
                            counts = mapOf("bag" to 0, "trolley" to 0)
                            vehicleNumber = TextFieldValue("")
                            currentTransactionId = ""
                            prefs.edit().remove(AppConfig.KEY_TRANSACTION_CAM2).apply()
                            Toast.makeText(context, AppConfig.CAM2_TOAST_DETECTION_STOPPED, Toast.LENGTH_SHORT).show()
                        }
                    }, enabled = isDetecting, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.width(150.dp)) {
                        Text(AppConfig.CAM2_BUTTON_STOP)
                    }
                }

                Spacer(Modifier.height(30.dp))

                if (isDetecting) LiveCountsPanelCam2(counts)
            }

            if (showInvalidConfirmDialog) {
                AlertDialog(onDismissRequest = { showInvalidConfirmDialog = false }, title = { Text(AppConfig.CAM2_INVALID_TITLE) }, text = { Text(AppConfig.CAM2_INVALID_MESSAGE) }, confirmButton = {
                    TextButton(onClick = {
                        showInvalidConfirmDialog = false
                        coroutineScope.launch {
                            val transactionId = UUID.randomUUID().toString()
                            currentTransactionId = transactionId
                            prefs.edit().putString(AppConfig.KEY_TRANSACTION_CAM2, transactionId).apply()

                            startDetectionCam2(context, client, START_URL, name, role, userId, deviceUniqueId, vehicleNumber.text, sessionId, transactionId) { isDetecting = true }
                        }
                    }) { Text(AppConfig.CAM2_INVALID_YES) }
                }, dismissButton = { TextButton(onClick = { showInvalidConfirmDialog = false }) { Text(AppConfig.CAM2_INVALID_CANCEL) } })
            }
        }
    }
}

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

        val request = Request.Builder().url(url).post(payload.toString().toRequestBody("application/json".toMediaType())).build()
        val response = client.newCall(request).execute()
        val ok = response.isSuccessful
        response.close()

        withContext(Dispatchers.Main) {
            if (ok) Toast.makeText(context, AppConfig.CAM2_TOAST_STARTED, Toast.LENGTH_SHORT).show()
        }

        onResult(ok)
    } catch (e: Exception) {
        Log.e("CAM2_START", "Error: ${e.message}")
        onResult(false)
    }
}

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

        val request = Request.Builder().url(url).post(payload.toString().toRequestBody("application/json".toMediaType())).build()
        val response = client.newCall(request).execute()
        val ok = response.isSuccessful
        response.close()

        withContext(Dispatchers.Main) {
            if (ok) Toast.makeText(context, AppConfig.CAM2_TOAST_STOPPED, Toast.LENGTH_SHORT).show()
        }

        onResult(ok)
    } catch (e: Exception) {
        Log.e("CAM2_STOP", "Error: ${e.message}")
        onResult(false)
    }
}

@Composable
fun LiveCountsPanelCam2(counts: Map<String, Int>) {
    Card(modifier = Modifier.fillMaxWidth(0.9f).padding(top = 16.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📊 Live Object Counts", fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text("🛍 Bag: ${counts["bag"]}")
            Text("🛒 Trolley: ${counts["trolley"]}")
        }
    }
}
