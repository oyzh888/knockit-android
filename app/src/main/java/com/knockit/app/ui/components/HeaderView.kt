package com.knockit.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knockit.app.ui.theme.Warning

@Composable
fun HeaderView(
    notificationEnabled: Boolean,
    onEnableClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // App Title Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Knockit",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "🔔", // 🔔
                fontSize = 28.sp,
            )
        }

        // Notification Permission Banner
        AnimatedVisibility(
            visible = !notificationEnabled,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            NotificationBanner(onEnableClick = onEnableClick)
        }
    }
}

@Composable
private fun NotificationBanner(
    onEnableClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Warning.copy(alpha = 0.15f))
            .clickable(onClick = onEnableClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.NotificationsOff,
            contentDescription = null,
            tint = Warning,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Notifications are disabled",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Warning,
                ),
            )
            Text(
                text = "Tap to enable so you don't miss reminders",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Warning.copy(alpha = 0.8f),
                ),
            )
        }
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = "Enable notifications",
            tint = Warning,
            modifier = Modifier.size(20.dp),
        )
    }
}
