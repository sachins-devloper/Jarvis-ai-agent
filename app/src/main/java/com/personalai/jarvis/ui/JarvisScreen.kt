package com.personalai.jarvis.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteOutline
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.FileProvider
import com.personalai.jarvis.utils.ImageHelper
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import com.personalai.jarvis.ui.components.OpenAiKeyDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.personalai.jarvis.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.personalai.jarvis.agent.AgentEvent
import com.personalai.jarvis.agent.ToolResult
import com.personalai.jarvis.ui.components.ChatMessageBubble
import com.personalai.jarvis.ui.components.GlowingMicButton
import com.personalai.jarvis.ui.components.ToolExecutionCard
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
fun JarvisScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var stagedImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            stagedImageUri = uri
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraTempUri != null) {
            stagedImageUri = cameraTempUri
        }
    }

    if (showSettings) {
        SettingsScreen(
            viewModel = viewModel,
            onBack = { showSettings = false },
            modifier = modifier
        )
        return
    }

    val showScrollToBottom by remember {
        derivedStateOf {
            val totalItems = uiState.messages.size
            if (totalItems <= 1) false
            else {
                val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleIndex < totalItems - 1
            }
        }
    }

    // Lifecycle observer to refresh Accessibility & Notification listener active state
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshServicesStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-scroll to bottom on new messages or active loading state
    LaunchedEffect(uiState.messages.size, uiState.activeEvent) {
        val hasActiveEvent = uiState.activeEvent !is AgentEvent.Idle && uiState.activeEvent !is AgentEvent.Completed
        val totalCount = uiState.messages.size + if (hasActiveEvent) 1 else 0
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground),
        color = JarvisBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // 1. Top Header Bar (Clean, Minimal, Modern)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(JarvisCyan.copy(alpha = 0.15f))
                            .border(1.dp, JarvisCyan.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ai_logo),
                            contentDescription = "Jarvis Logo",
                            tint = JarvisCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "JARVIS",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                            if (uiState.isOpenAiEnabled) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF10A37F).copy(alpha = 0.16f))
                                        .border(1.dp, Color(0xFF10A37F).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .clickable { showSettings = true }
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10A37F))
                                    )
                                    Text(
                                        text = "OpenAI",
                                        color = Color(0xFF10A37F),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (uiState.isOpenAiEnabled) "OpenAI Cloud • ${uiState.openAiModel}" else "Neural Personal Assistant",
                            color = if (uiState.isOpenAiEnabled) Color(0xFF10A37F) else TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.isSpeaking) {
                        IconButton(onClick = { viewModel.stopSpeaking() }) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Mute",
                                tint = JarvisCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    // Clean Settings Gear Button
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(JarvisSurface)
                            .border(1.dp, JarvisBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = JarvisCyan,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // 2. Conversation Stream & Tool Execution Cards with Scroll-to-Bottom Button

            // 3. Conversation Stream & Tool Execution Cards with Scroll-to-Bottom Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.messages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(JarvisSurfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Empty",
                                            tint = JarvisCyan.copy(alpha = 0.6f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Ready to assist you on-device.",
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap the glowing mic or type an action below.",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    items(uiState.messages, key = { it.id }) { msg ->
                        Column {
                            ChatMessageBubble(message = msg)

                            // If this message has suggestions (e.g. disambiguation options for multiple contacts)
                            if (!msg.suggestions.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    msg.suggestions.forEach { suggestion ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(JarvisCyan.copy(alpha = 0.15f))
                                                .border(1.dp, JarvisCyan, RoundedCornerShape(16.dp))
                                                .clickable { viewModel.submitQuery(suggestion) }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = suggestion,
                                                color = JarvisCyan,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            // If this message executed a tool, render the ToolExecutionCard
                            if (msg.toolCall != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                ToolExecutionCard(
                                    toolName = msg.toolCall,
                                    arguments = emptyMap(),
                                    result = ToolResult(success = true, output = msg.toolResult ?: "Completed")
                                )
                            }
                        }
                    }

                    // Active Agent Loading / Thinking / Tool Execution Box below the last message
                    if (uiState.activeEvent !is AgentEvent.Idle && uiState.activeEvent !is AgentEvent.Completed) {
                        if (uiState.activeEvent is AgentEvent.ToolExecuting) {
                            val ev = uiState.activeEvent as AgentEvent.ToolExecuting
                            item {
                                ToolExecutionCard(
                                    toolName = ev.toolName,
                                    arguments = ev.arguments,
                                    isExecuting = true
                                )
                            }
                        } else {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(JarvisSurfaceVariant)
                                        .border(1.dp, JarvisCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = JarvisCyan,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        val statusText = when (val ev = uiState.activeEvent) {
                                            is AgentEvent.Thinking -> ev.message
                                            is AgentEvent.ToolExecuted -> "Tool completed: ${ev.toolName}"
                                            is AgentEvent.TokenStream -> "Generating response..."
                                            is AgentEvent.Error -> "Error: ${ev.error}"
                                            else -> "Processing..."
                                        }
                                        Text(
                                            text = statusText,
                                            color = JarvisCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Scroll-to-Bottom button when scrolled up
                val scrollBtnAlpha by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (showScrollToBottom) 1f else 0f,
                    label = "scrollToBottomAlpha"
                )
                if (scrollBtnAlpha > 0.01f) {
                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                if (uiState.messages.isNotEmpty()) {
                                    listState.animateScrollToItem(uiState.messages.size - 1)
                                }
                            }
                        },
                        shape = CircleShape,
                        color = JarvisSurfaceVariant.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.8f)),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = 12.dp)
                            .size(42.dp)
                            .graphicsLayer {
                                alpha = scrollBtnAlpha
                                scaleX = 0.7f + (0.3f * scrollBtnAlpha)
                                scaleY = 0.7f + (0.3f * scrollBtnAlpha)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll to bottom",
                                tint = JarvisCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Attached Image Preview Strip (if user picked an image from camera or gallery)
            AnimatedVisibility(
                visible = stagedImageUri != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                stagedImageUri?.let { uri ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = JarvisSurfaceVariant,
                            border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val thumbBmp = remember(uri) {
                                    try {
                                        val stream = context.contentResolver.openInputStream(uri)
                                        val bmp = BitmapFactory.decodeStream(stream)
                                        stream?.close()
                                        bmp?.asImageBitmap()
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (thumbBmp != null) {
                                    Image(
                                        bitmap = thumbBmp,
                                        contentDescription = "Attached photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Photo Attached",
                                        color = JarvisCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "GPT-4o Vision Ready",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                IconButton(
                                    onClick = { stagedImageUri = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove photo",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Input & Glowing Microphone / Send Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasContent = inputText.isNotBlank() || stagedImageUri != null

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (stagedImageUri != null) "Ask about this photo..." else if (uiState.isListening) "Listening to your voice..." else "Ask Jarvis to run a phone task...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            // Gallery Picker Button
                            IconButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Pick Image from Gallery",
                                    tint = if (stagedImageUri != null) JarvisCyan else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Camera Snapshot Button
                            IconButton(
                                onClick = {
                                    val photoFile = ImageHelper.createTempCameraFile(context)
                                    val photoUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        photoFile
                                    )
                                    cameraTempUri = photoUri
                                    cameraLauncher.launch(photoUri)
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Take Photo with Camera",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorder,
                        focusedContainerColor = JarvisSurface,
                        unfocusedContainerColor = JarvisSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = JarvisCyan
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (hasContent) ImeAction.Send else ImeAction.Default
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (hasContent) {
                                val query = inputText.trim()
                                val imageToSubmit = stagedImageUri
                                inputText = ""
                                stagedImageUri = null
                                viewModel.submitQuery(query, imageToSubmit)
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Glowing animated Voice / Send Button
                GlowingMicButton(
                    isListening = uiState.isListening,
                    rmsLevel = uiState.rmsLevel,
                    hasText = hasContent,
                    onClick = {
                        if (hasContent) {
                            val query = inputText.trim()
                            val imageToSubmit = stagedImageUri
                            inputText = ""
                            stagedImageUri = null
                            viewModel.submitQuery(query, imageToSubmit)
                        } else {
                            viewModel.toggleVoiceInput()
                        }
                    }
                )
            }
        }

        // 6. OpenAI Configuration & API Key Dialog
        if (uiState.showApiKeyDialog) {
            OpenAiKeyDialog(
                initialApiKey = uiState.openAiKey,
                initialModel = uiState.openAiModel,
                initialEnabled = uiState.isOpenAiEnabled,
                onDismiss = { viewModel.dismissApiKeyDialog() },
                onSave = { apiKey, model, enabled ->
                    viewModel.saveOpenAiConfig(apiKey, model, enabled)
                }
            )
        }
    }
}
