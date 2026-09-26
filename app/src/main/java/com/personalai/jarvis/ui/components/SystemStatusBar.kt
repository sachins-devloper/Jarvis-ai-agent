package com.personalai.jarvis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personalai.jarvis.ui.theme.JarvisBorder
import com.personalai.jarvis.ui.theme.JarvisCyan
import com.personalai.jarvis.ui.theme.JarvisGreen
import com.personalai.jarvis.ui.theme.JarvisOrange
import com.personalai.jarvis.ui.theme.JarvisSurface
import com.personalai.jarvis.ui.theme.TextPrimary
import com.personalai.jarvis.ui.theme.TextSecondary

@Composable
fun SystemStatusBar(
    engineName: String,
    toolCount: Int,
    isAccessibilityActive: Boolean,
    isNotificationListenerActive: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // AI Engine Status
        StatusPill(
            label = "AI Engine",
            value = if (engineName.contains("GGUF")) "GGUF llama.cpp" else "Zero-Shot Mobile",
            indicatorColor = JarvisCyan
        )

        // Tools count
        StatusPill(
            label = "Tools",
            value = "$toolCount Registered",
            indicatorColor = JarvisGreen
        )

        // Accessibility Service
        StatusPill(
            label = "Accessibility",
            value = if (isAccessibilityActive) "Active" else "Enable",
            indicatorColor = if (isAccessibilityActive) JarvisGreen else JarvisOrange,
            onClick = if (!isAccessibilityActive) onOpenAccessibility else null
        )

        // Notification Listener
        StatusPill(
            label = "Notifications",
            value = if (isNotificationListenerActive) "Active" else "Enable",
            indicatorColor = if (isNotificationListenerActive) JarvisGreen else JarvisOrange,
            onClick = if (!isNotificationListenerActive) onOpenNotifications else null
        )
    }
}

@Composable
private fun StatusPill(
    label: String,
    value: String,
    indicatorColor: Color,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(JarvisSurface)
            .border(1.dp, JarvisBorder, RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$label: ",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
