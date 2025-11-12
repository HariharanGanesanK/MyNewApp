package com.example.helloworldapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ✅ Model class
data class Transaction(
    val transaction_id: String?,
    val name: String?,
    val role: String?,
    val camera: String?,
    val vehicleNumber: String?,
    val date: String?,
    val startTime: String?,
    val endTime: String?,
    val box: Int?,
    val bale: Int?,
    val bag: Int?,
    val trolley: Int?,
    val imageUrl: String?
)

// ✅ API service
interface ApiService {
    @GET("api/transactions/today")
    suspend fun getTodayTransactions(): List<Transaction>

    @GET("api/transactions/grouped")
    suspend fun getGroupedTransactions(): Map<String, List<Transaction>>
}

// ✅ Helper for clickable text (expand/collapse)
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    return pointerInput(Unit) { detectTapGestures { onClick() } }
}

// ✅ Activity
class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DashboardScreen() }
    }
}

@Composable
fun DashboardScreen() {
    // 🚀 Backend base URL
    val api = remember {
        Retrofit.Builder()
            .baseUrl("http://192.168.1.7:9020/") // ✅ your working backend IP
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    var todayData by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var groupedHistory by remember { mutableStateOf<Map<String, List<Transaction>>>(emptyMap()) }
    val expandedDates = remember { mutableStateMapOf<String, Boolean>() }

    // ✅ Fetch today's data every 5s
    LaunchedEffect(Unit) {
        while (true) {
            try {
                todayData = api.getTodayTransactions()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(5000)
        }
    }

    // ✅ Fetch grouped history every 10s
    LaunchedEffect(Unit) {
        while (true) {
            try {
                groupedHistory = api.getGroupedTransactions()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(10000)
        }
    }

    // ✅ UI
    if (todayData.isEmpty() && groupedHistory.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("🔄 Loading data...", color = Color(0xFF38bdf8))
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0f172a))
                .padding(12.dp)
        ) {
            item {
                Text(
                    "📊 Loading Dashboard",
                    color = Color(0xFF38f865),
                    fontSize = 22.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            // ✅ Today's loadings
            if (todayData.isNotEmpty()) {
                itemsIndexed(todayData) { _, item -> TodayBox(item) }
            } else {
                item {
                    Text(
                        "📅 No loadings today",
                        color = Color(0xFFa1a1aa),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // ✅ Grouped history (excluding today)
            val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            groupedHistory.keys.sortedDescending().forEach { date ->
                val records = groupedHistory[date] ?: emptyList()
                if (date != today) {
                    item {
                        val expanded = expandedDates[date] ?: false
                        Text(
                            text = if (expanded) "📂 $date (${records.size})" else "📁 $date (${records.size})",
                            color = Color(0xFFE7EDEE),
                            fontSize = 18.sp,
                            modifier = Modifier
                                .padding(vertical = 6.dp)
                                .clickableNoRipple { expandedDates[date] = !expanded }
                        )
                    }
                    if (expandedDates[date] == true) {
                        itemsIndexed(records) { _, rec -> HistoryBox(rec) }
                    }
                }
            }
        }
    }
}

@Composable
fun TodayBox(item: Transaction) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (item.endTime == null) 2.dp else 0.dp,
                color = if (item.endTime == null) Color(0xFF4ade80) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .background(Color(0xFF1e293b))
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("📅 ${item.date ?: "N/A"}", color = Color(0xFFeceff0), fontSize = 16.sp, modifier = Modifier.weight(1f))
            if (item.endTime == null)
                Text("🟢 LIVE ⏱ ${item.startTime}", color = Color(0xFF4ade80))
            else
                Text("⏰ ${item.startTime} 🔚 ${item.endTime}", color = Color(0xFFbbbbbb))
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("👤 ${item.name}", color = Color(0xFFe2e8f0))
                Text("🧑‍💼 ${item.role}", color = Color(0xFFe2e8f0))
                Text("📦 Box: ${item.box}", color = Color(0xFFe2e8f0))
                Text("🧵 Bale: ${item.bale}", color = Color(0xFFe2e8f0))
                Text("🎒 Bag: ${item.bag}", color = Color(0xFFe2e8f0))
                Text("🛒 Trolley: ${item.trolley}", color = Color(0xFFe2e8f0))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("🚗 ${item.vehicleNumber}", color = Color(0xFFe2e8f0))
                Text("📹 ${item.camera}", color = Color(0xFFe2e8f0))
                Spacer(Modifier.height(6.dp))
                if (!item.imageUrl.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(item.imageUrl),
                        contentDescription = "Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Text("No Image", color = Color(0xFF94a3b8))
                }
            }
        }
    }
}

@Composable
fun HistoryBox(item: Transaction) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1e293b))
            .padding(10.dp)
    ) {
        Text("📅 ${item.date}", color = Color(0xFFe2e8f0))
        Text("⏰ ${item.startTime} ${if (item.endTime != null) "🔚 ${item.endTime}" else "🟢 LIVE"}", color = Color(0xFFbfc6cc))
        Text("👤 ${item.name} (${item.role})", color = Color(0xFFe2e8f0))
        Text("🚗 ${item.vehicleNumber}", color = Color(0xFFe2e8f0))
        Text("📹 ${item.camera}", color = Color(0xFFe2e8f0))
    }
}
