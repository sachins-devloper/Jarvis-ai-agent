package com.personalai.jarvis.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.personalai.jarvis.agent.AgentTool
import com.personalai.jarvis.ui.theme.JarvisBackground
import com.personalai.jarvis.ui.theme.JarvisBorder
import com.personalai.jarvis.ui.theme.JarvisCyan
import com.personalai.jarvis.ui.theme.JarvisGreen
import com.personalai.jarvis.ui.theme.JarvisOrange
import com.personalai.jarvis.ui.theme.JarvisSurface
import com.personalai.jarvis.ui.theme.JarvisSurfaceVariant
import com.personalai.jarvis.ui.theme.TextMuted
import com.personalai.jarvis.ui.theme.TextPrimary
import com.personalai.jarvis.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var toolsExpanded by remember { mutableStateOf(false) }
    var showAboutFeatures by remember { mutableStateOf(false) }

    if (showAboutFeatures) {
        AboutFeaturesScreen(
            onBack = { showAboutFeatures = false },
            modifier = modifier
        )
        return
    }

    // OpenAI state
    var apiKeyInput by remember(uiState.openAiKey) { mutableStateOf(uiState.openAiKey) }
    var selectedModel by remember(uiState.openAiModel) { mutableStateOf(uiState.openAiModel) }
    var showPassword by remember { mutableStateOf(false) }
    var apiKeySavedFeedback by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val modelsList = listOf("gpt-4o-mini", "gpt-4o", "gpt-4-turbo", "gpt-3.5-turbo")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(JarvisSurface)
                    .border(1.dp, JarvisBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = JarvisCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "SETTINGS",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "System Configuration & Controls",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // --- Settings Body ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ==========================================
            // 1. AI & LLM ENGINE CONFIGURATION
            // ==========================================
            SettingsSectionCard(title = "AI & LLM Configuration", icon = Icons.Default.Psychology) {
                // Engine Mode Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isOpenAiEnabled) "OpenAI Cloud Engine" else "Local On-Device Engine",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (uiState.isOpenAiEnabled)
                                "Using OpenAI API for deep reasoning & broad world knowledge"
                            else
                                "Runs 100% on-device without internet server dependencies",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = uiState.isOpenAiEnabled,
                        onCheckedChange = { viewModel.toggleOpenAi(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF10A37F),
                            checkedTrackColor = Color(0xFF10A37F).copy(alpha = 0.3f),
                            uncheckedThumbColor = JarvisCyan,
                            uncheckedTrackColor = JarvisSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = uiState.isOpenAiEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // API Key Input
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it
                                apiKeySavedFeedback = false
                            },
                            label = { Text("OpenAI API Key", color = TextSecondary, fontSize = 12.sp) },
                            placeholder = { Text("sk-proj-...", color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        val clipText = clipboardManager.getText()?.text ?: ""
                                        if (clipText.isNotBlank()) {
                                            apiKeyInput = clipText
                                            apiKeySavedFeedback = false
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Paste",
                                            tint = JarvisCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Visibility",
                                            tint = TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10A37F),
                                unfocusedBorderColor = JarvisBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = JarvisBackground,
                                unfocusedContainerColor = JarvisBackground
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Model Selection Chips
                        Text(
                            text = "Model",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            modelsList.take(3).forEach { model ->
                                val isSelected = selectedModel.equals(model, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF10A37F).copy(alpha = 0.2f) else JarvisSurfaceVariant)
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF10A37F) else JarvisBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedModel = model
                                            apiKeySavedFeedback = false
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = model.replace("gpt-", ""),
                                        color = if (isSelected) Color(0xFF10A37F) else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Save OpenAI Config Button
                        Button(
                            onClick = {
                                viewModel.saveOpenAiConfig(apiKeyInput.trim(), selectedModel, true)
                                apiKeySavedFeedback = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10A37F)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (apiKeySavedFeedback) Icons.Default.Check else Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (apiKeySavedFeedback) "Saved & Active" else "Save OpenAI Configuration",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // If Local AI is active
                AnimatedVisibility(
                    visible = !uiState.isOpenAiEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(JarvisBackground)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Active Engine: ${uiState.engineName}",
                                color = JarvisCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Direct zero-latency execution. Controls device settings, alarms, calls, messages, and missed calls offline.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // ==========================================
            // 2. AUDIO & VOICE CONTROLS
            // ==========================================
            SettingsSectionCard(title = "Audio & Voice Controls", icon = Icons.Default.VolumeUp) {
                // Auto-Speak Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Speak Replies",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Jarvis reads every AI response aloud via Text-to-Speech",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = uiState.autoSpeak,
                        onCheckedChange = { viewModel.setAutoSpeak(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JarvisCyan,
                            checkedTrackColor = JarvisCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = JarvisSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Speech Speed Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Speech Rate",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${String.format("%.2f", uiState.speechRate)}x",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = uiState.speechRate,
                        onValueChange = { viewModel.setSpeechRate(it) },
                        valueRange = 0.75f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisCyan,
                            activeTrackColor = JarvisCyan,
                            inactiveTrackColor = JarvisSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pitch Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Voice Pitch",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${String.format("%.2f", uiState.ttsPitch)}x",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = uiState.ttsPitch,
                        onValueChange = { viewModel.setTtsPitch(it) },
                        valueRange = 0.75f..1.3f,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisCyan,
                            activeTrackColor = JarvisCyan,
                            inactiveTrackColor = JarvisSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Test Voice Button
                OutlinedButton(
                    onClick = { viewModel.testVoice() },
                    border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = JarvisCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Test Jarvis Voice",
                        color = JarvisCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ==========================================
            // 3. SYSTEM SERVICES & PERMISSIONS
            // ==========================================
            SettingsSectionCard(title = "System Services & Permissions", icon = Icons.Default.Security) {
                // Accessibility Service
                ServiceStatusRow(
                    title = "Accessibility Automation",
                    subtitle = "Required for UI screen inspection & screenshots",
                    isActive = uiState.isAccessibilityActive,
                    onOpenSettings = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Notification Listener Service
                ServiceStatusRow(
                    title = "Notification Listener",
                    subtitle = "Required for reading incoming phone notifications",
                    isActive = uiState.isNotificationListenerActive,
                    onOpenSettings = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Runtime Permissions Snapshot
                val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                val hasCall = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                val hasContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                val hasCallLog = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
                val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                val hasStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
                } else {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                }

                PermissionItem(name = "Microphone (Voice)", isGranted = hasAudio)
                PermissionItem(name = "Camera (Vision AI)", isGranted = hasCamera)
                PermissionItem(name = "Direct Phone Call", isGranted = hasCall)
                PermissionItem(name = "Contacts Lookup", isGranted = hasContacts)
                PermissionItem(name = "Call Log & Missed Calls", isGranted = hasCallLog)
                PermissionItem(name = "Photos, Music & Media Storage", isGranted = hasStorage)

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    border = BorderStroke(1.dp, JarvisBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Manage App Permissions in Android",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // ==========================================
            // 4. ACTIVE AGENT TOOLS
            // ==========================================
            val tools = remember { viewModel.getAllTools() }
            SettingsSectionCard(title = "Registered Agent Tools (${tools.size})", icon = Icons.Default.Tune) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { toolsExpanded = !toolsExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (toolsExpanded) "Hide active tools" else "View all ${tools.size} registered tools",
                        color = JarvisCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = if (toolsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = JarvisCyan
                    )
                }

                AnimatedVisibility(visible = toolsExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tools.forEach { tool ->
                            ToolItemRow(tool = tool)
                        }
                    }
                }
            }

            // ==========================================
            // 5. DATA MANAGEMENT & CLEAR HISTORY
            // ==========================================
            SettingsSectionCard(title = "Data & Conversation History", icon = Icons.Default.Storage) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clear Chat History",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Deletes all local message logs and tool execution records",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = { showClearConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ==========================================
            // 6. ABOUT JARVIS & FEATURES
            // ==========================================
            SettingsSectionCard(title = "About Jarvis & Feature Guide", icon = Icons.Default.Info) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "App Version", color = TextSecondary, fontSize = 12.sp)
                    Text(text = "1.0.0 (Neural Mobile)", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Architecture", color = TextSecondary, fontSize = 12.sp)
                    Text(text = "Local-First Tool Agent", color = JarvisCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showAboutFeatures = true },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = JarvisCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "View App & Feature Guide",
                        color = JarvisCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // --- Confirmation Dialog for Clear History ---
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Clear Conversation History?",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will erase all past conversation messages and tool execution logs from local memory. This action cannot be undone.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete Everything", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = JarvisSurface,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface),
        border = BorderStroke(1.dp, JarvisBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisCyan.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = JarvisCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun ServiceStatusRow(
    title: String,
    subtitle: String,
    isActive: Boolean,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(JarvisBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isActive) JarvisGreen else Color(0xFFEF4444))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isActive) JarvisGreen.copy(alpha = 0.15f) else JarvisOrange.copy(alpha = 0.15f))
                .border(
                    1.dp,
                    if (isActive) JarvisGreen.copy(alpha = 0.4f) else JarvisOrange.copy(alpha = 0.4f),
                    RoundedCornerShape(6.dp)
                )
                .clickable(onClick = onOpenSettings)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isActive) "Active" else "Enable",
                color = if (isActive) JarvisGreen else JarvisOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PermissionItem(
    name: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, color = TextSecondary, fontSize = 12.sp)
        Text(
            text = if (isGranted) "Granted ✓" else "Not Granted ✕",
            color = if (isGranted) JarvisGreen else Color(0xFFEF4444),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ToolItemRow(tool: AgentTool) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(JarvisBackground)
            .border(1.dp, JarvisBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = tool.name,
                color = JarvisCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${tool.definition.parameters.size} params",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tool.description,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}
