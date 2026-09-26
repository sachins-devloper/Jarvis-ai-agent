package com.personalai.jarvis.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.personalai.jarvis.ui.components.SystemStatusBar
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
    var inputText by remember { mutableStateOf("") }

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

    // Auto-scroll to bottom on new messages
    LaunchedEffect(uiState.messages.size, uiState.activeEvent) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
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
            // 1. Top Header Bar
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(JarvisCyan.copy(alpha = 0.15f))
                            .border(1.dp, JarvisCyan.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Jarvis Logo",
                            tint = JarvisCyan,
                            modifier = Modifier.size(20.dp)
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(JarvisCyan.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LOCAL AI",
                                    color = JarvisCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Autonomous Android Agent",
                            color = TextSecondary,
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
                                tint = JarvisCyan
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear History",
                            tint = TextMuted
                        )
                    }
                }
            }

            // 2. System Status Bar (Engine, Tools, Services)
            SystemStatusBar(
                engineName = uiState.engineName,
                toolCount = uiState.toolCount,
                isAccessibilityActive = uiState.isAccessibilityActive,
                isNotificationListenerActive = uiState.isNotificationListenerActive,
                onOpenAccessibility = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                onOpenNotifications = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )

            // 3. Active Agent State Banner (Thinking / Tool Execution)
            AnimatedVisibility(
                visible = uiState.activeEvent !is AgentEvent.Idle && uiState.activeEvent !is AgentEvent.Completed,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisSurfaceVariant)
                        .border(1.dp, JarvisCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
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
                            is AgentEvent.ToolExecuting -> "Agent executing: ${ev.toolName}..."
                            is AgentEvent.ToolExecuted -> "Tool finished: ${ev.toolName}"
                            is AgentEvent.TokenStream -> "Generating response..."
                            is AgentEvent.Error -> "Error: ${ev.error}"
                            else -> "Processing..."
                        }
                        Text(
                            text = statusText,
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 4. Conversation Stream & Tool Execution Cards
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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

                // If currently executing a tool right now, show live ToolExecutionCard
                if (uiState.activeEvent is AgentEvent.ToolExecuting) {
                    val ev = uiState.activeEvent as AgentEvent.ToolExecuting
                    item {
                        ToolExecutionCard(
                            toolName = ev.toolName,
                            arguments = ev.arguments,
                            isExecuting = true
                        )
                    }
                }
            }

            // 5. Quick Suggestion Chips
            val quickChips = listOf(
                "Open YouTube",
                "Set alarm at 7:00 AM",
                "Turn on flashlight",
                "Search Python DSA",
                "Battery status",
                "What is Python?"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickChips.forEach { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(JarvisSurfaceVariant)
                            .border(1.dp, JarvisBorder, RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.submitQuery(chip)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = chip,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 6. Bottom Input & Glowing Microphone Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (uiState.isListening) "Listening to your voice..." else "Ask Jarvis to run a phone task...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorder,
                        focusedContainerColor = JarvisSurface,
                        unfocusedContainerColor = JarvisSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 3,
                    trailingIcon = {
                        if (inputText.isNotBlank()) {
                            IconButton(onClick = {
                                val query = inputText
                                inputText = ""
                                viewModel.submitQuery(query)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = JarvisCyan
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Glowing animated Voice Microphone Button
                GlowingMicButton(
                    isListening = uiState.isListening,
                    rmsLevel = uiState.rmsLevel,
                    onClick = {
                        viewModel.toggleVoiceInput()
                    }
                )
            }
        }
    }
}
