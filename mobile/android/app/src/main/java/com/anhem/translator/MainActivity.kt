package com.anhem.translator

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anhem.translator.service.ClipboardMonitorService
import com.anhem.translator.ui.screen.SettingsScreen
import com.anhem.translator.ui.screen.TranslatePanelActivity
import com.anhem.translator.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val storage = (application as AnhemApp).storage
        val settings = storage.loadSettings()

        setContent {
            AnhemTheme(darkTheme = true) {
                var showSettings by remember { mutableStateOf(!settings.hasValidKey()) }
                var monitoring by remember { mutableStateOf(settings.clipboardMonitorEnabled) }

                if (showSettings) {
                    SettingsScreen(
                        storage = storage,
                        modifier = Modifier.fillMaxSize(),
                        onComplete = { showSettings = false }
                    )
                } else {
                    // Home dashboard
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("💬 anhem", fontSize = 28.sp, color = ColorWhite)
                        Text("中越互译 · v4-flash 驱动", color = ColorSecondary, fontSize = 14.sp)
                        Spacer(Modifier.height(32.dp))

                        // Clipboard monitor toggle
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ColorSurface)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("剪贴板监听", color = ColorWhite, fontSize = 16.sp)
                                        Text("复制文本自动翻译", color = ColorSecondary, fontSize = 13.sp)
                                    }
                                    Switch(
                                        checked = monitoring,
                                        onCheckedChange = { enabled ->
                                            monitoring = enabled
                                            storage.saveSettings(settings.copy(clipboardMonitorEnabled = enabled))
                                            if (enabled) {
                                                startService(Intent(this@MainActivity, ClipboardMonitorService::class.java))
                                            } else {
                                                stopService(Intent(this@MainActivity, ClipboardMonitorService::class.java))
                                            }
                                        },
                                        colors = SwitchDefaults.colors(checkedTrackColor = ColorAccent)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Input panel button
                        Button(
                            onClick = {
                                startActivity(Intent(this@MainActivity, TranslatePanelActivity::class.java))
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorAccent),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("✏ 输入翻译", fontSize = 16.sp) }

                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { showSettings = true }) {
                            Text("⚙ 设置", color = ColorSecondary)
                        }
                    }
                }
            }
        }
    }
}

private fun com.anhem.translator.model.Settings.hasValidKey(): Boolean {
    return apiKey.startsWith("sk-") && apiKey.length > 20
}
