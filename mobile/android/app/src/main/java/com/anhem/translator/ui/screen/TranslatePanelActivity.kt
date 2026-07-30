package com.anhem.translator.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.anhem.translator.AnhemApp
import com.anhem.translator.engine.TranslationEngine
import com.anhem.translator.model.*
import com.anhem.translator.ui.theme.*
import kotlinx.coroutines.launch

class TranslatePanelActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initText = intent.getStringExtra("initial_text") ?: ""
        val initDir = intent.getStringExtra("direction") ?: "auto"

        setContent {
            AnhemTheme(darkTheme = true) {
                TranslatePanel(
                    initialText = initText,
                    initialDir = initDir,
                    onCopy = { text -> copyToClipboard(text) },
                    onTranslate = { text, persona, profile, settings, tone, dir ->
                        translate(text, persona, profile, settings, tone, dir)
                    }
                )
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("translation", text))
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
    }

    private suspend fun translate(
        text: String, persona: Persona, profile: Profile,
        settings: Settings, tone: String, dir: String
    ): String {
        val result = TranslationEngine.translate(text, dir, persona, profile, settings, tone)
        return result.text
    }
}

@Composable
fun TranslatePanel(
    initialText: String,
    initialDir: String,
    onCopy: (String) -> Unit,
    onTranslate: suspend (String, Persona, Profile, Settings, String, String) -> String
) {
    val context = LocalContext.current
    val storage = remember { (context.applicationContext as AnhemApp).storage }
    val settings by remember { mutableStateOf(storage.loadSettings()) }
    val profile by remember { mutableStateOf(storage.loadProfile()) }
    var activePersona by remember { mutableStateOf(Persona.ALL.first { it.id == storage.loadActivePersona() }) }
    var tone by remember { mutableStateOf(storage.loadTone()) }

    var inputText by remember { mutableStateOf(initialText) }
    var resultText by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(initialDir) }
    var isTranslating by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ColorBlack
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.statusBarsPadding())
            Spacer(Modifier.height(8.dp))

            // Persona selector
            PersonaSelector(activePersona) { p ->
                activePersona = p
                storage.saveActivePersona(p.id)
                storage.saveTone(p.tone)
                tone = p.tone
            }

            Spacer(Modifier.height(12.dp))

            // Input area
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it; direction = if (it.isNotEmpty()) detectDirection(it) else initialDir },
                placeholder = { Text("输入要翻译的文字…", color = ColorSecondary) },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ColorWhite,
                    unfocusedTextColor = ColorWhite,
                    focusedBorderColor = ColorAccent,
                    unfocusedBorderColor = ColorSeparator,
                    cursorColor = ColorAccent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (inputText.isNotBlank() && !isTranslating) {
                        val dir = if (direction == "auto") detectDirection(inputText) else direction
                        direction = dir
                        isTranslating = true
                        scope.launch {
                            resultText = onTranslate(inputText, activePersona, profile, settings, tone, dir)
                            isTranslating = false
                        }
                    }
                }),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Direction badge + translate button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (direction != "auto") {
                    Badge(direction)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        if (inputText.isNotBlank() && !isTranslating) {
                            val dir = if (direction == "auto") detectDirection(inputText) else direction
                            direction = dir
                            isTranslating = true
                            scope.launch {
                                resultText = onTranslate(inputText, activePersona, profile, settings, tone, dir)
                                isTranslating = false
                            }
                        }
                    },
                    enabled = inputText.isNotBlank() && !isTranslating,
                    colors = ButtonDefaults.buttonColors(containerColor = ColorAccent),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(if (isTranslating) "翻译中…" else "翻译")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Result card
            if (resultText.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ColorSurface)
                        .padding(16.dp)
                ) {
                    Text("原文", color = ColorSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(inputText, color = ColorWhite, fontSize = 16.sp, lineHeight = 24.sp)
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = ColorSeparator)
                    Spacer(Modifier.height(12.dp))
                    Text("译文", color = ColorAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(resultText, color = ColorWhite, fontSize = 16.sp, lineHeight = 24.sp)
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { onCopy(resultText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorAccent),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("📋 复制译文") }
            }

            if (isTranslating && resultText.isEmpty()) {
                Spacer(Modifier.height(40.dp))
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = ColorAccent
                )
            }
        }
    }
}

@Composable
fun PersonaSelector(active: Persona, onSelect: (Persona) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Persona.ALL) { persona ->
            val isSelected = persona.id == active.id
            val bgColor by animateColorAsState(
                if (isSelected) ColorAccent else ColorSurface,
                label = "personaBg"
            )
            val textColor = if (isSelected) ColorWhite else ColorSecondary

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable { onSelect(persona) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(persona.emoji, fontSize = 20.sp)
                Text(persona.label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun Badge(dir: String) {
    val label = if (dir == "zh2vi") "中→越" else "越→中"
    val bgColor = if (dir == "zh2vi") ColorAccent.copy(alpha = 0.15f) else ColorGreen.copy(alpha = 0.15f)
    val fgColor = if (dir == "zh2vi") ColorAccent else ColorGreen

    Text(
        text = label,
        color = fgColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
