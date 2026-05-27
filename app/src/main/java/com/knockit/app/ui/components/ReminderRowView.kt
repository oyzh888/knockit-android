package com.knockit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knockit.app.data.model.Reminder
import com.knockit.app.data.model.ReminderType
import com.knockit.app.data.model.RepeatRule
import com.knockit.app.ui.theme.Error
import com.knockit.app.ui.theme.ScenarioExercise
import com.knockit.app.ui.theme.ScenarioFeeding
import com.knockit.app.ui.theme.ScenarioMedicine
import com.knockit.app.ui.theme.ScenarioPrayer
import com.knockit.app.ui.theme.ScenarioSleep
import com.knockit.app.ui.theme.ScenarioWater
import com.knockit.app.ui.theme.ScenarioCustom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReminderRowView(
    reminder: Reminder,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = remember(reminder.type) { ReminderType.fromRawValue(reminder.type) }
    val iconColor = typeColor(type)
    val icon = typeIcon(type)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onEdit)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Type icon circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = type.displayName,
                tint = iconColor,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Middle: title + time + repeat chip
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (reminder.isActive)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                ),
                maxLines = 1,
            )
            Text(
                text = formatTriggerTime(reminder.triggerAt),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (reminder.isActive) 1f else 0.45f
                    ),
                    fontSize = 12.sp,
                ),
            )
            if (reminder.repeatRule != RepeatRule.NONE || reminder.intervalMinutes != null) {
                val chipLabel = repeatLabel(reminder)
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = chipLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        )
                    },
                    modifier = Modifier.padding(top = 2.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = iconColor.copy(alpha = 0.12f),
                        labelColor = iconColor,
                    ),
                    border = null,
                )
            }
        }

        // Right: Switch + Delete
        Switch(
            checked = reminder.isActive,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            ),
        )

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete reminder",
                tint = Error.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun typeColor(type: ReminderType): Color = when (type) {
    ReminderType.BABY -> ScenarioFeeding
    ReminderType.PRAYER -> ScenarioPrayer
    ReminderType.MEDICINE -> ScenarioMedicine
    ReminderType.WATER -> ScenarioWater
    ReminderType.EXERCISE -> ScenarioExercise
    ReminderType.SLEEP -> ScenarioSleep
    ReminderType.CUSTOM -> ScenarioCustom
}

private fun typeIcon(type: ReminderType): ImageVector = when (type) {
    ReminderType.BABY -> Icons.Filled.ChildCare
    ReminderType.PRAYER -> Icons.Filled.SelfImprovement
    ReminderType.MEDICINE -> Icons.Filled.Medication
    ReminderType.WATER -> Icons.Filled.WaterDrop
    ReminderType.EXERCISE -> Icons.Filled.FitnessCenter
    ReminderType.SLEEP -> Icons.Filled.Bedtime
    ReminderType.CUSTOM -> Icons.Filled.Notifications
}

private fun repeatLabel(reminder: Reminder): String {
    reminder.intervalMinutes?.let { mins ->
        return when {
            mins < 60 -> "Every ${mins}m"
            mins % 60 == 0 -> "Every ${mins / 60}h"
            else -> "Every ${mins}m"
        }
    }
    return when (reminder.repeatRule) {
        RepeatRule.DAILY -> "Daily"
        RepeatRule.WEEKLY -> "Weekly"
        else -> ""
    }
}

private fun formatTriggerTime(epochMs: Long): String {
    val date = Date(epochMs)
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { time = date }

    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFmt.format(date)

    val dayDiff = target.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR)
    val yearDiff = target.get(Calendar.YEAR) - now.get(Calendar.YEAR)

    return when {
        yearDiff == 0 && dayDiff == 0 -> "Today $timeStr"
        yearDiff == 0 && dayDiff == 1 -> "Tomorrow $timeStr"
        yearDiff == 0 -> {
            val dateFmt = SimpleDateFormat("MMM d", Locale.getDefault())
            "${dateFmt.format(date)} $timeStr"
        }
        else -> {
            val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            "${dateFmt.format(date)} $timeStr"
        }
    }
}
