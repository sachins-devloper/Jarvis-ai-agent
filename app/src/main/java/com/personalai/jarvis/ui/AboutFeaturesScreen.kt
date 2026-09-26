package com.personalai.jarvis.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personalai.jarvis.ui.theme.JarvisBackground
import com.personalai.jarvis.ui.theme.JarvisBorder
import com.personalai.jarvis.ui.theme.JarvisCyan
import com.personalai.jarvis.ui.theme.JarvisSurface
import com.personalai.jarvis.ui.theme.TextMuted
import com.personalai.jarvis.ui.theme.TextPrimary
import com.personalai.jarvis.ui.theme.TextSecondary

@Composable
fun AboutFeaturesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val scrollState = rememberScrollState()

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
                    text = "ABOUT & FEATURES",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Jarvis Capabilities & Command Guide",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // --- Body ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero App Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurface),
                border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(JarvisCyan.copy(alpha = 0.15f))
                                .border(1.dp, JarvisCyan.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "JARVIS PERSONAL AI",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Version 1.0.0 • Local-First Android Agent",
                                color = JarvisCyan,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Jarvis is an autonomous AI assistant built specifically for Android. It operates with a tool-use agent architecture, meaning when you talk to Jarvis, it analyzes your intent and calls real phone tools to execute tasks directly on your device without opening manual apps.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            // Section Header
            Text(
                text = "COMPLETE FEATURE BREAKDOWN",
                color = JarvisCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 6.dp)
            )

            // 1. Phone Calls & Missed Calls
            FeatureDetailCard(
                icon = Icons.Default.Call,
                title = "Phone Calls & Call Logs",
                toolNames = listOf("make_call", "lookup_contact", "get_call_log"),
                description = "Places direct phone calls without opening the dialer UI, resolves contacts from your phonebook, and queries missed or incoming calls.",
                examples = listOf(
                    "Call Mom",
                    "Do I have any missed calls?",
                    "Who called me?",
                    "Show my call history",
                    "Dial 9876543210"
                )
            )

            // 2. Messaging (SMS & WhatsApp)
            FeatureDetailCard(
                icon = Icons.Default.Chat,
                title = "Messaging (SMS & WhatsApp)",
                toolNames = listOf("send_message"),
                description = "Sends instant text messages or WhatsApp chats with automated recipient lookup and multi-turn contact disambiguation.",
                examples = listOf(
                    "Send message to John saying I'll be there in 10 minutes",
                    "WhatsApp Alex: Are you free for lunch?",
                    "Text Mom I reached safely"
                )
            )

            // 3. Device Controls & Hardware
            FeatureDetailCard(
                icon = Icons.Default.PhoneAndroid,
                title = "Device Controls & Hardware",
                toolNames = listOf("device_control"),
                description = "Controls device hardware including flashlight, music volume, battery monitoring, internal storage calculation, and screenshots.",
                examples = listOf(
                    "Turn on flashlight / Turn off torch",
                    "Volume up / Volume down",
                    "Battery status",
                    "Check storage space",
                    "Take a screenshot"
                )
            )

            // 4. Mobile Device Diagnostics & Specs
            FeatureDetailCard(
                icon = Icons.Default.Security,
                title = "Device Specs & Diagnostics",
                toolNames = listOf("get_device_info"),
                description = "Inspects live device information including model name, brand, Android OS release, RAM capacity, internal storage, network connectivity, and uptime.",
                examples = listOf(
                    "What is my mobile device about?",
                    "Tell me about my phone",
                    "Show phone specs",
                    "How much RAM does my phone have?"
                )
            )

            // 5. Alarms & Reminders
            FeatureDetailCard(
                icon = Icons.Default.Alarm,
                title = "Alarms & Timers",
                toolNames = listOf("set_alarm"),
                description = "Schedules system alarms directly in Android Clock with natural language time parsing.",
                examples = listOf(
                    "Set alarm for 7:00 AM",
                    "Alarm at 6:30 PM",
                    "Wake me up at 8 AM"
                )
            )

            // 6. Web Search & Browsing
            FeatureDetailCard(
                icon = Icons.Default.Language,
                title = "Web Search & Browsing",
                toolNames = listOf("open_browser"),
                description = "Searches the web via Google or opens specific URLs in your preferred mobile browser.",
                examples = listOf(
                    "Search latest technology news",
                    "Google quantum physics",
                    "Open wikipedia.org"
                )
            )

            // 7. Notes & Memory
            FeatureDetailCard(
                icon = Icons.Default.NoteAdd,
                title = "Notes & Long-Term Memory",
                toolNames = listOf("create_note"),
                description = "Stores persistent notes, reminders, and facts into Jarvis's local Room database.",
                examples = listOf(
                    "Take a note: Buy groceries tonight",
                    "Remember that my flight ticket number is XYZ123",
                    "Add note about the meeting"
                )
            )

            // 8. YouTube Music & Video Playback
            FeatureDetailCard(
                icon = Icons.Default.PlayCircle,
                title = "YouTube Video & Music Playback",
                toolNames = listOf("play_youtube"),
                description = "Searches and plays music tracks, trailers, and educational videos directly inside the YouTube app.",
                examples = listOf(
                    "Play Interstellar soundtrack on YouTube",
                    "Watch Kotlin Jetpack Compose tutorial",
                    "Play relaxing music"
                )
            )

            // 8.5 Local File & Media Search
            FeatureDetailCard(
                icon = Icons.Default.Storage,
                title = "File & Media Search (Images, Music, Video, Docs)",
                toolNames = listOf("search_files"),
                description = "Searches on-device photos/images, songs/music, videos, and documents with instant interactive file cards. Tapping any result opens the file in your media player/gallery or opens its containing folder location.",
                examples = listOf(
                    "Search files whatsapp",
                    "Search images",
                    "Find photos screenshot",
                    "Search music",
                    "Find video",
                    "Search doc pdf"
                )
            )

            // 9. Application Launcher
            FeatureDetailCard(
                icon = Icons.Default.Apps,
                title = "App Launcher",
                toolNames = listOf("open_app"),
                description = "Launches any installed application on your phone by spoken or typed name.",
                examples = listOf(
                    "Open WhatsApp",
                    "Launch Camera",
                    "Open Spotify",
                    "Start Settings"
                )
            )

            // 10. AI Reasoning & Local Engine
            FeatureDetailCard(
                icon = Icons.Default.Psychology,
                title = "Dual AI Intelligence Architecture",
                toolNames = listOf("OpenAI Cloud", "Zero-Shot Mobile Engine", "GGUF llama.cpp"),
                description = "Seamlessly combines OpenAI cloud intelligence (GPT-4o, GPT-4o-mini) for deep conversational reasoning with an on-device rule engine and llama.cpp native runner for 100% offline capability.",
                examples = listOf(
                    "What is gravity?",
                    "Explain photosynthesis in simple terms",
                    "Solve 25 * 14"
                )
            )

            // 11. Audio & TTS System
            FeatureDetailCard(
                icon = Icons.Default.GraphicEq,
                title = "Speech & Audio Engine",
                toolNames = listOf("SpeechRecognizer", "TextToSpeech", "RMS Visualizer"),
                description = "Built-in speech-to-text with live audio wave animation, Text-to-Speech synthesis with configurable speech speed (0.75x - 1.50x), vocal pitch, and Auto-Speak response toggle.",
                examples = listOf(
                    "Tap mic button to speak naturally",
                    "Toggle Auto-Speak on/off in Settings",
                    "Customize speech rate & pitch in Settings"
                )
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FeatureDetailCard(
    icon: ImageVector,
    title: String,
    toolNames: List<String>,
    description: String,
    examples: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface),
        border = BorderStroke(1.dp, JarvisBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisCyan.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = JarvisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = toolNames.joinToString(" • "),
                        color = JarvisCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Examples
            Text(
                text = "EXAMPLE COMMANDS:",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                examples.forEach { example ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "›",
                            color = JarvisCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "\"$example\"",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
