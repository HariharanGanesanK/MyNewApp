package com.example.helloworldapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class CamSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CamSelectionScreen(context = this)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CamSelectionScreen(context: Context) {
    val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

    // 🧾 User info
    val name = prefs.getString("name", "N/A")
    val role = prefs.getString("role", "N/A")
    val userId = prefs.getString("userId", "N/A")
    val sessionId = prefs.getString("session_id", "N/A")

    var showProfileDialog by remember { mutableStateOf(false) }
    var showMenuDialog by remember { mutableStateOf(false) }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // 🔝 Top Bar Icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // ☰ Menu Icon (Left)
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable { showMenuDialog = true }
                )

                // 👤 Profile Icon (Right)
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { showProfileDialog = true }
                )
            }

            // 🎥 Camera Buttons (Center)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    "🎥 Camera Selection",
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val intent = Intent(context, Cam1Page::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Text("Cam 1")
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val intent = Intent(context, Cam2Page::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Text("Cam 2")
                }
            }

            // 👤 Profile Popup Dialog
            if (showProfileDialog) {
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
                            Text("🔑 Session ID: $sessionId")
                        }
                    }
                )
            }

            // ☰ Menu Dialog
            if (showMenuDialog) {
                AlertDialog(
                    onDismissRequest = { showMenuDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showMenuDialog = false }) {
                            Text("Close")
                        }
                    },
                    title = { Text("📋 Menu") },
                    text = {
                        Column {
                            Text(
                                "🏠 Dashboard",
                                modifier = Modifier
                                    .clickable {
                                        val intent = Intent(context, DashboardActivity::class.java)
                                        context.startActivity(intent)
                                        showMenuDialog = false
                                    }
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}
