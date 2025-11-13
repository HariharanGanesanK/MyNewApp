package com.example.helloworldapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// -------------------------------------------------------------
// MODEL
// -------------------------------------------------------------
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

// -------------------------------------------------------------
// API
// -------------------------------------------------------------
interface ApiService {
    @GET("api/transactions/today")
    suspend fun getTodayTransactions(): List<Transaction>

    @GET("api/transactions/grouped")
    suspend fun getGroupedTransactions(): Map<String, List<Transaction>>
}

// -------------------------------------------------------------
// MAIN ACTIVITY
// -------------------------------------------------------------
class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                DashboardScreen()
            }
        }
    }
}

// -------------------------------------------------------------
// DASHBOARD SCREEN
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {

    val api = remember {
        Retrofit.Builder()
            .baseUrl("http://192.168.1.7:9020/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val scope = rememberCoroutineScope()

    var todayData by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var groupedHistory by remember { mutableStateOf<Map<String, List<Transaction>>>(emptyMap()) }

    val expandedDates = remember { mutableStateMapOf<String, Boolean>() }

    var expandedCardId by remember { mutableStateOf<String?>(null) }        // TODAY
    var expandedHistoryCardId by remember { mutableStateOf<String?>(null) } // LAST 7 DAYS  (OPTION C)

    var isRefreshing by remember { mutableStateOf(false) }

    // AUTO REFRESH 10s
    LaunchedEffect(Unit) {
        while (true) {
            try {
                todayData = api.getTodayTransactions()
                groupedHistory = api.getGroupedTransactions()
                val firstActive = todayData.firstOrNull { it.endTime == null }
                if (expandedCardId == null && firstActive != null)
                    expandedCardId = firstActive.transaction_id

            } catch (_: Exception) {}
            delay(10000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "📊 7-Day Loading Dashboard",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D47A1),
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            isRefreshing = true
                            try {
                                todayData = api.getTodayTransactions()
                                groupedHistory = api.getGroupedTransactions()
                            } catch (_: Exception) {}
                            isRefreshing = false
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = if (isRefreshing) Color.Gray else Color(0xFF1565C0)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFB))
                .padding(innerPadding)
        ) {

            if (groupedHistory.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("⏳ Fetching loadings...", color = Color(0xFF607D8B))
                }
            } else {

                val today = LocalDate.now()
                val isoFormatter = DateTimeFormatter.ISO_DATE
                val prettyFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {

                    // -------------------------------------------------------------
                    // TODAY'S ACTIVE LOADING
                    // -------------------------------------------------------------
                    item {
                        Text(
                            "📅 Today’s Active Loadings",
                            color = Color(0xFF00796B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    if (todayData.isNotEmpty()) {
                        val sortedToday = todayData.sortedByDescending { it.startTime ?: "" }

                        itemsIndexed(sortedToday) { _, item ->
                            val isExpanded = expandedCardId == item.transaction_id

                            StylishCard(
                                item = item,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    expandedCardId =
                                        if (isExpanded) null else item.transaction_id
                                }
                            )
                        }
                    }

                    // -------------------------------------------------------------
                    // LAST 7 DAYS LOADING
                    // -------------------------------------------------------------
                    item {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "🗓 Last 7 Days Loadings",
                            color = Color(0xFF1E3A8A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    groupedHistory.keys.sortedDescending().forEach { dateKey ->

                        val records = groupedHistory[dateKey] ?: emptyList()
                        val expanded = expandedDates[dateKey] ?: false

                        val prettyDate = try {
                            LocalDate.parse(dateKey, isoFormatter).format(prettyFormatter)
                        } catch (_: Exception) { dateKey }

                        val label =
                            if (dateKey == today.format(isoFormatter))
                                "Today (${records.size})"
                            else "$prettyDate (${records.size})"

                        // DATE HEADER
                        item {
                            Card(
                                onClick = { expandedDates[dateKey] = !expanded },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .animateContentSize(),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor =
                                        if (expanded) Color(0xFFE3F2FD) else Color(0xFFF9FAFB)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = Color(0xFF1565C0)
                                    )
                                    Spacer(Modifier.width(8.dp))

                                    Text(
                                        text = label,
                                        color = Color(0xFF0D47A1),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Surface(
                                        color = if (records.isNotEmpty()) Color(0xFF2196F3)
                                        else Color.LightGray,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            "${records.size}",
                                            modifier = Modifier.padding(10.dp),
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Spacer(Modifier.width(6.dp))

                                    Icon(
                                        imageVector = if (expanded) Icons.Default.ExpandLess
                                        else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color(0xFF1565C0)
                                    )
                                }
                            }
                        }

                        // INNER RECORDS (NOW EXPANDABLE OPTION C)
                        item {

                            AnimatedVisibility(
                                visible = expanded && records.isNotEmpty(),
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut()
                            ) {

                                Column(Modifier.padding(horizontal = 8.dp)) {

                                    records.sortedByDescending { it.startTime ?: "" }
                                        .forEach { rec ->

                                            val recordWithDate =
                                                if (rec.date.isNullOrBlank())
                                                    rec.copy(date = dateKey)
                                                else rec

                                            val isExpanded =
                                                expandedHistoryCardId == recordWithDate.transaction_id

                                            HistoryCard(
                                                item = recordWithDate,
                                                isExpanded = isExpanded,
                                                onToggleExpand = {
                                                    expandedHistoryCardId =
                                                        if (isExpanded) null
                                                        else recordWithDate.transaction_id
                                                }
                                            )
                                        }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TODAY'S CARD
// -------------------------------------------------------------
@Composable
fun StylishCard(item: Transaction, isExpanded: Boolean, onToggleExpand: () -> Unit) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(3.dp, RoundedCornerShape(16.dp))
            .animateContentSize(tween(300))
            .clickable { onToggleExpand() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {

        Column(Modifier.padding(14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "🚗 ${item.vehicleNumber ?: "Unknown"}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E88E5),
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    if (item.endTime == null) "🟢 LIVE" else "✅ DONE",
                    color = if (item.endTime == null)
                        Color(0xFF388E3C) else Color(0xFF455A64),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(4.dp))

            Text("👤 ${item.name ?: "N/A"} — ${item.role ?: "Role"}",
                color = Color(0xFF424242))

            Text("📹 ${item.camera ?: "Unknown Camera"}",
                color = Color(0xFF546E7A))

            Text("📅 ${item.date ?: "N/A"} | ⏰ ${item.startTime} → ${item.endTime ?: "..."}",
                color = Color.Gray)

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {

                Column {

                    Spacer(Modifier.height(8.dp))
                    ConditionalStats(item)
                    Spacer(Modifier.height(10.dp))

                    if (item.imageUrl.isNullOrBlank()) {
                        Text("❌ No image available", color = Color.Red)
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(item.imageUrl),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// EXPANDABLE HISTORY CARD (OPTION C)
// -------------------------------------------------------------
@Composable
fun HistoryCard(item: Transaction, isExpanded: Boolean, onToggleExpand: () -> Unit) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .clickable { onToggleExpand() }
            .animateContentSize(tween(300)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(12.dp)
    ) {

        Column(Modifier.padding(12.dp)) {

            Text("📅 ${item.date ?: "Unknown Date"}",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF37474F))

            Text("⏰ ${item.startTime} → ${item.endTime ?: "🟢 Active"}",
                color = Color(0xFF607D8B))

            Text("👤 ${item.name ?: ""} (${item.role ?: ""})",
                color = Color(0xFF424242))

            Text("🚗 ${item.vehicleNumber}", color = Color(0xFF1E88E5))

            Text("📹 ${item.camera}", color = Color(0xFF757575))

            Spacer(Modifier.height(6.dp))

            ConditionalStats(item)

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {

                Column {

                    Spacer(Modifier.height(8.dp))

                    if (item.imageUrl.isNullOrBlank()) {
                        Text("❌ No image available", color = Color.Red)
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(item.imageUrl),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// COMMON STATS BASED ON CAMERA
// -------------------------------------------------------------
@Composable
fun ConditionalStats(item: Transaction) {
    Row {
        when (item.camera) {
            "cam_1" -> {
                Text("📦 ${item.box ?: 0} box", color = Color(0xFF00695C))
                Spacer(Modifier.width(12.dp))
                Text("🧵 ${item.bale ?: 0} bale", color = Color(0xFF00695C))
                Spacer(Modifier.width(12.dp))
                Text("🛒 ${item.trolley ?: 0} trolley", color = Color(0xFF00695C))
            }
            "cam_2" -> {
                Text("🎒 ${item.bag ?: 0} bag", color = Color(0xFF00695C))
                Spacer(Modifier.width(12.dp))
                Text("🛒 ${item.trolley ?: 0} trolley", color = Color(0xFF00695C))
            }
            else -> {
                Text("⚙️ No data", color = Color.Gray)
            }
        }
    }
}
