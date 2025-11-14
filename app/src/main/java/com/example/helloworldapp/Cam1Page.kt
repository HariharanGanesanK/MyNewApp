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

class Cam1Page : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Cam1Screen(context = this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Cam1Screen(context: Context) {
    val prefs = context.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)

    val name = prefs.getString(AppConfig.KEY_NAME, "N/A") ?: "N/A"
    val role = prefs.getString(AppConfig.KEY_ROLE, "N/A") ?: "N/A"
    val userId = prefs.getString(AppConfig.KEY_USER_ID, "N/A") ?: "N/A"
    val deviceUniqueId = prefs.getString(AppConfig.KEY_DEVICE_ID, "N/A") ?: "N/A"

    val storedSessionId = prefs.getString(AppConfig.KEY_SESSION_ID, null)
    val storedTransactionId = prefs.getString(AppConfig.KEY_TRANSACTION_CAM1, "")

    var sessionId by remember { mutableStateOf(storedSessionId ?: "") }
    var currentTransactionId by remember { mutableStateOf(storedTransactionId ?: "") }

    var vehicleNumber by remember { mutableStateOf(TextFieldValue("")) }
    var isDetecting by remember { mutableStateOf(currentTransactionId.isNotEmpty()) }

    var counts by remember { mutableStateOf(mapOf("box" to 0, "bale" to 0, "trolley" to 0)) }

    val coroutineScope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }

    var showInvalidConfirmDialog by remember { mutableStateOf(false) }

    val vehicleNumberRegex = remember { Regex("^[A-Z]{2}\\s?\\d{1,2}\\s?[A-Z]{1,3}\\s?\\d{1,4}$") }

    val mqttHelper = remember { MqttHelper(AppConfig.CAM1_SERVER_IP) }

    LaunchedEffect(isDetecting) {
        if (isDetecting) {
            val topic = "jlmill/sessions/$sessionId/$currentTransactionId/counts"
            Log.d("MQTT", "Subscribing: $topic")

            mqttHelper.connect {
                mqttHelper.subscribe(topic) { msg ->
                    try {
                        val json = JSONObject(msg)
                        val c = json.getJSONObject("counts")
                        counts = mapOf(
                            "box" to c.optInt("box"),
                            "bale" to c.optInt("bale"),
                            "trolley" to c.optInt("trolley")
                        )
                    } catch (_: Exception) {}
                }
            }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(AppConfig.CAM1_TITLE, fontSize = 23.sp)
                Spacer(Modifier.height(12.dp))
                Text("${AppConfig.CAM1_SUPERVISOR_LABEL}: $name", fontSize = 16.sp)

                if (sessionId.isNotEmpty()) Text("${AppConfig.CAM1_SESSION_LABEL}: $sessionId", fontSize = 12.sp)
                if (currentTransactionId.isNotEmpty()) Text("${AppConfig.CAM1_TRANSACTION_LABEL}: $currentTransactionId", fontSize = 12.sp)

                Spacer(Modifier.height(24.dp))
                OutlinedTextField(value = vehicleNumber, onValueChange = { vehicleNumber = it.copy(text = it.text.uppercase()) }, label = { Text(AppConfig.CAM1_VEHICLE_LABEL) }, modifier = Modifier.fillMaxWidth(0.9f))
                Spacer(Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = {
                        if (sessionId.isEmpty()) { Toast.makeText(context, AppConfig.CAM1_TOAST_NO_SESSION, Toast.LENGTH_SHORT).show(); return@Button }
                        if (!vehicleNumberRegex.matches(vehicleNumber.text.trim())) { showInvalidConfirmDialog = true; return@Button }

                        coroutineScope.launch {
                            val transactionId = UUID.randomUUID().toString()
                            currentTransactionId = transactionId
                            prefs.edit().putString(AppConfig.KEY_TRANSACTION_CAM1, transactionId).apply()

                            startDetectionCam1(context, client, AppConfig.CAM1_START_URL, name, role, userId, deviceUniqueId, vehicleNumber.text, sessionId, transactionId) { ok ->
                                if (ok) isDetecting = true
                            }
                        }
                    }, enabled = !isDetecting, modifier = Modifier.width(150.dp)) { Text(AppConfig.CAM1_BUTTON_START) }

                    Button(onClick = {
                        coroutineScope.launch {
                            stopDetectionCam1(context, client, AppConfig.CAM1_STOP_URL, sessionId, currentTransactionId) {}
                            isDetecting = false
                            counts = mapOf("box" to 0, "bale" to 0, "trolley" to 0)
                            vehicleNumber = TextFieldValue("")
                            currentTransactionId = ""
                            prefs.edit().remove(AppConfig.KEY_TRANSACTION_CAM1).apply()
                            Toast.makeText(context, AppConfig.CAM1_TOAST_DETECTION_STOPPED, Toast.LENGTH_SHORT).show()
                        }
                    }, enabled = isDetecting, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.width(150.dp)) {
                        Text(AppConfig.CAM1_BUTTON_STOP)
                    }
                }

                Spacer(Modifier.height(30.dp))

                if (isDetecting) LiveCountsPanelCam1(counts)
            }

            if (showInvalidConfirmDialog) {
                AlertDialog(onDismissRequest = { showInvalidConfirmDialog = false }, title = { Text(AppConfig.CAM1_INVALID_TITLE) }, text = { Text(AppConfig.CAM1_INVALID_MESSAGE) }, confirmButton = {
                    TextButton(onClick = {
                        showInvalidConfirmDialog = false
                        coroutineScope.launch {
                            val transactionId = UUID.randomUUID().toString()
                            currentTransactionId = transactionId
                            prefs.edit().putString(AppConfig.KEY_TRANSACTION_CAM1, transactionId).apply()

                            startDetectionCam1(context, client, AppConfig.CAM1_START_URL, name, role, userId, deviceUniqueId, vehicleNumber.text, sessionId, transactionId) { isDetecting = true }
                        }
                    }) { Text(AppConfig.CAM1_INVALID_YES) }
                }, dismissButton = { TextButton(onClick = { showInvalidConfirmDialog = false }) { Text(AppConfig.CAM1_INVALID_CANCEL) } })
            }
        }
    }
}

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

        val req = Request.Builder().url(url).post(payload.toString().toRequestBody("application/json".toMediaType())).build()
        val res = client.newCall(req).execute()
        val ok = res.isSuccessful
        res.close()

        withContext(Dispatchers.Main) {
            if (ok) Toast.makeText(context, AppConfig.CAM1_TOAST_STARTED, Toast.LENGTH_SHORT).show()
        }

        onResult(ok)
    } catch (e: Exception) {
        Log.e("CAM1_START", "Error: ${e.message}")
        onResult(false)
    }
}

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

        val req = Request.Builder().url(url).post(payload.toString().toRequestBody("application/json".toMediaType())).build()
        val res = client.newCall(req).execute()
        val ok = res.isSuccessful
        res.close()

        withContext(Dispatchers.Main) {
            if (ok) Toast.makeText(context, AppConfig.CAM1_TOAST_STOPPED, Toast.LENGTH_SHORT).show()
        }

        onResult(ok)
    } catch (e: Exception) {
        Log.e("CAM1_STOP", "Error: ${e.message}")
        onResult(false)
    }
}

@Composable
fun LiveCountsPanelCam1(counts: Map<String, Int>) {
    Card(modifier = Modifier.fillMaxWidth(0.9f).padding(top = 16.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📊 Live Object Counts", fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text("📦 Box: ${counts["box"]}")
            Text("🧵 Bale: ${counts["bale"]}")
            Text("🛒 Trolley: ${counts["trolley"]}")
        }
    }
}
