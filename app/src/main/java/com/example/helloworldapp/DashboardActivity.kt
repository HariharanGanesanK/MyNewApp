package com.example.helloworldapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.helloworldapp.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.clickable


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

interface ApiService {
    @GET(AppConfig.ENDPOINT_TODAY_TRANSACTIONS)
    suspend fun getTodayTransactions(): List<Transaction>

    @GET(AppConfig.ENDPOINT_GROUPED_TRANSACTIONS)
    suspend fun getGroupedTransactions(): Map<String, List<Transaction>>
}

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DashboardScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {

    val api = remember {
        Retrofit.Builder()
            .baseUrl(AppConfig.DASHBOARD_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val scope = rememberCoroutineScope()

    var todayData by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var groupedHistory by remember { mutableStateOf<Map<String, List<Transaction>>>(emptyMap()) }

    val expandedDates = remember { mutableStateMapOf<String, Boolean>() }

    var expandedCardId by remember { mutableStateOf<String?>(null) }
    var expandedHistoryCardId by remember { mutableStateOf<String?>(null) }

    var isRefreshing by remember { mutableStateOf(false) }

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
                title = { Text(AppConfig.DASHBOARD_TITLE, fontSize = 20.sp, color = Color(0xFF0D47A1)) },
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
                            contentDescription = AppConfig.REFRESH_CONTENT_DESCRIPTION,
                            tint = if (isRefreshing) Color.Gray else Color(0xFF1565C0)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            Modifier.fillMaxSize().background(Color(0xFFF8FAFB)).padding(innerPadding)
        ) {
            if (groupedHistory.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(AppConfig.DASHBOARD_LOADING_MESSAGE)
                }
            } else {
                val today = LocalDate.now()
                val isoFormat = DateTimeFormatter.ISO_DATE
                val pretty = DateTimeFormatter.ofPattern("MMM dd, yyyy")

                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    item {
                        Text(AppConfig.TODAY_SECTION_TITLE, color = Color(0xFF00796B), fontSize = 18.sp)
                    }

                    if (todayData.isNotEmpty()) {
                        val sorted = todayData.sortedByDescending { it.startTime ?: "" }

                        itemsIndexed(sorted) { _, item ->
                            val open = expandedCardId == item.transaction_id

                            StylishCard(item = item, isExpanded = open, onToggleExpand = {
                                expandedCardId = if (open) null else item.transaction_id
                            })
                        }
                    }

                    item { Spacer(Modifier.height(12.dp)) }

                    item {
                        Text(AppConfig.LAST_7_DAYS_TITLE, color = Color(0xFF1E3A8A), fontSize = 18.sp)
                    }

                    groupedHistory.keys.sortedDescending().forEach { dateKey ->
                        val list = groupedHistory[dateKey] ?: emptyList()
                        val expanded = expandedDates[dateKey] ?: false

                        val prettyDate = try {
                            LocalDate.parse(dateKey, isoFormat).format(pretty)
                        } catch (_: Exception) { dateKey }

                        val label = if (dateKey == today.format(isoFormat))
                            "${AppConfig.LABEL_TODAY} (${list.size})"
                        else "$prettyDate (${list.size})"

                        item {
                            Card(
                                onClick = { expandedDates[dateKey] = !expanded },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (expanded) Color(0xFFE3F2FD) else Color(0xFFF9FAFB)
                                )
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF1565C0))
                                    Spacer(Modifier.width(8.dp))
                                    Text(label, modifier = Modifier.weight(1f), color = Color(0xFF0D47A1))
                                    Surface(
                                        color = if (list.isNotEmpty()) Color(0xFF2196F3) else Color.LightGray,
                                        shape = CircleShape
                                    ) {
                                        Text(list.size.toString(), modifier = Modifier.padding(10.dp), color = Color.White)
                                    }
                                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = Color(0xFF1565C0))
                                }
                            }
                        }

                        item {
                            AnimatedVisibility(visible = expanded && list.isNotEmpty(), enter = fadeIn() + expandVertically(), exit = fadeOut()) {
                                Column(Modifier.padding(8.dp)) {
                                    list.sortedByDescending { it.startTime ?: "" }.forEach { rec ->
                                        val withDate = if (rec.date.isNullOrBlank()) rec.copy(date = dateKey) else rec
                                        val open = expandedHistoryCardId == withDate.transaction_id
                                        HistoryCard(item = withDate, isExpanded = open, onToggleExpand = {
                                            expandedHistoryCardId = if (open) null else withDate.transaction_id
                                        })
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

@Composable
fun StylishCard(item: Transaction, isExpanded: Boolean, onToggleExpand: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp).shadow(3.dp, RoundedCornerShape(16.dp)).animateContentSize(tween(AppConfig.CARD_ANIMATION_DURATION)).clickable { onToggleExpand() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🚗 ${item.vehicleNumber ?: "Unknown"}", fontSize = 17.sp, modifier = Modifier.weight(1f), color = Color(0xFF1E88E5))
                Text(if (item.endTime == null) AppConfig.STATUS_LIVE else AppConfig.STATUS_DONE, color = if (item.endTime == null) Color(0xFF388E3C) else Color(0xFF455A64))
            }

            Text("👤 ${item.name} — ${item.role}", color = Color.DarkGray)
            Text("📹 ${item.camera}", color = Color.Gray)
            Text("📅 ${item.date} | ⏰ ${item.startTime} → ${item.endTime ?: "..."}")

            AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    ConditionalStats(item)
                    Spacer(Modifier.height(12.dp))

                    if (item.imageUrl.isNullOrEmpty()) {
                        Text(AppConfig.NO_IMAGE_AVAILABLE, color = Color.Red)
                    } else {
                        Image(painter = rememberAsyncImagePainter(item.imageUrl), contentDescription = null, modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)))
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(item: Transaction, isExpanded: Boolean, onToggleExpand: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(6.dp).shadow(1.dp, RoundedCornerShape(12.dp)).clickable { onToggleExpand() }.animateContentSize(tween(AppConfig.CARD_ANIMATION_DURATION)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("📅 ${item.date}", color = Color(0xFF37474F))
            Text("⏰ ${item.startTime} → ${item.endTime ?: AppConfig.STATUS_LIVE}")
            Text("👤 ${item.name} (${item.role})")
            Text("🚗 ${item.vehicleNumber}")
            Text("📹 ${item.camera}")

            Spacer(Modifier.height(6.dp))
            ConditionalStats(item)

            AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    if (item.imageUrl.isNullOrEmpty()) {
                        Text(AppConfig.NO_IMAGE_AVAILABLE, color = Color.Red)
                    } else {
                        Image(painter = rememberAsyncImagePainter(item.imageUrl), contentDescription = null, modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)))
                    }
                }
            }
        }
    }
}

@Composable
fun ConditionalStats(item: Transaction) {
    Row {
        when (item.camera) {
            "cam_1" -> {
                Text("${AppConfig.LABEL_BOX} ${item.box ?: 0} box")
                Spacer(Modifier.width(12.dp))
                Text("${AppConfig.LABEL_BALE} ${item.bale ?: 0} bale")
                Spacer(Modifier.width(12.dp))
                Text("${AppConfig.LABEL_TROLLEY} ${item.trolley ?: 0} trolley")
            }
            "cam_2" -> {
                Text("${AppConfig.LABEL_BAG} ${item.bag ?: 0} bag")
                Spacer(Modifier.width(12.dp))
                Text("${AppConfig.LABEL_TROLLEY} ${item.trolley ?: 0} trolley")
            }
            else -> Text("⚙️ No data")
        }
    }
}
