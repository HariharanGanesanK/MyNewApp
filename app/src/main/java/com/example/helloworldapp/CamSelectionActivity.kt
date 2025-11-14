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
import com.example.helloworldapp.config.AppConfig

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

    val prefs = context.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)

    val name = prefs.getString(AppConfig.KEY_NAME, "N/A")
    val role = prefs.getString(AppConfig.KEY_ROLE, "N/A")
    val userId = prefs.getString(AppConfig.KEY_USER_ID, "N/A")
    val sessionId = prefs.getString(AppConfig.KEY_SESSION_ID, "N/A")

    var showProfileDialog by remember { mutableStateOf(false) }
    var showMenuDialog by remember { mutableStateOf(false) }

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", modifier = Modifier.size(42.dp).clip(CircleShape).clickable { showMenuDialog = true })
                Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Profile", modifier = Modifier.size(48.dp).clip(CircleShape).clickable { showProfileDialog = true })
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.align(Alignment.Center)) {
                Text(AppConfig.TITLE_CAMERA_SELECTION, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(24.dp))

                Button(onClick = { context.startActivity(Intent(context, Cam1Page::class.java)) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
                    Text(AppConfig.BUTTON_CAM1)
                }

                Spacer(Modifier.height(20.dp))

                Button(onClick = { context.startActivity(Intent(context, Cam2Page::class.java)) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
                    Text(AppConfig.BUTTON_CAM2)
                }
            }

            if (showProfileDialog) {
                AlertDialog(
                    onDismissRequest = { showProfileDialog = false },
                    confirmButton = { TextButton(onClick = { showProfileDialog = false }) { Text(AppConfig.BUTTON_CLOSE) } },
                    title = { Text(AppConfig.DIALOG_USER_INFO_TITLE) },
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

            if (showMenuDialog) {
                AlertDialog(
                    onDismissRequest = { showMenuDialog = false },
                    confirmButton = { TextButton(onClick = { showMenuDialog = false }) { Text(AppConfig.BUTTON_CLOSE) } },
                    title = { Text(AppConfig.MENU_TITLE) },
                    text = {
                        Column {
                            Text(AppConfig.MENU_DASHBOARD, modifier = Modifier.clickable {
                                context.startActivity(Intent(context, DashboardActivity::class.java))
                                showMenuDialog = false
                            }.padding(vertical = 8.dp))
                        }
                    }
                )
            }
        }
    }
}
