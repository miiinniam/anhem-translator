package com.anhem.translator.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anhem.translator.model.AppStorage
import com.anhem.translator.model.Settings
import com.anhem.translator.ui.theme.*

@Composable
fun SettingsScreen(
    storage: AppStorage,
    modifier: Modifier = Modifier,
    onComplete: () -> Unit = {}
) {
    var settings by remember { mutableStateOf(storage.loadSettings()) }
    var keyStatus by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("👋 欢迎使用 anhem", style = MaterialTheme.typography.headlineMedium, color = ColorWhite)
        Spacer(Modifier.height(4.dp))
        Text("由 DeepSeek v4-flash 驱动", style = MaterialTheme.typography.bodyMedium, color = ColorSecondary)
        Spacer(Modifier.height(24.dp))

        // API Key
        OutlinedTextField(
            value = settings.apiKey,
            onValueChange = { settings = settings.copy(apiKey = it); keyStatus = "" },
            label = { Text("API Key (sk-...)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ColorWhite, unfocusedTextColor = ColorWhite,
                focusedBorderColor = ColorAccent, unfocusedBorderColor = ColorSeparator,
                cursorColor = ColorAccent
            )
        )
        if (keyStatus.isNotEmpty()) {
            Text(keyStatus, color = if (keyStatus.contains("✅")) ColorGreen else ColorRed)
        }
        Spacer(Modifier.height(8.dp))

        // Dialect
        Text("越南语方言", color = ColorSecondary, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("north" to "北方·河内", "south" to "南方·胡志明").forEach { (d, label) ->
                FilterChip(
                    selected = settings.dialect == d,
                    onClick = { settings = settings.copy(dialect = d) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ColorAccent,
                        selectedLabelColor = ColorWhite
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Clipboard monitor toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("剪贴板监听", color = ColorWhite)
                Text("复制文本自动翻译", color = ColorSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = settings.clipboardMonitorEnabled,
                onCheckedChange = { settings = settings.copy(clipboardMonitorEnabled = it) },
                colors = SwitchDefaults.colors(checkedTrackColor = ColorAccent)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Advanced: Glossary
        if (showAdvanced) {
            Text("术语表（一行一条）", color = ColorSecondary)
            OutlinedTextField(
                value = settings.glossary,
                onValueChange = { settings = settings.copy(glossary = it) },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ColorWhite, unfocusedTextColor = ColorWhite,
                    focusedBorderColor = ColorAccent, unfocusedBorderColor = ColorSeparator,
                    cursorColor = ColorAccent
                )
            )
        }
        TextButton(onClick = { showAdvanced = !showAdvanced }) {
            Text(if (showAdvanced) "收起" else "高级设置…", color = ColorAccent)
        }

        Spacer(Modifier.height(24.dp))

        // Save & Start
        Button(
            onClick = {
                storage.saveSettings(settings)
                onComplete()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = settings.apiKey.startsWith("sk-") && settings.apiKey.length > 20,
            colors = ButtonDefaults.buttonColors(containerColor = ColorAccent),
            shape = RoundedCornerShape(12.dp)
        ) { Text("开始使用", fontSize = 16.sp) }
    }
}

