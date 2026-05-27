package com.knockit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knockit.app.data.model.ReminderType
import com.knockit.app.ui.theme.ScenarioExercise
import com.knockit.app.ui.theme.ScenarioFeeding
import com.knockit.app.ui.theme.ScenarioMedicine
import com.knockit.app.ui.theme.ScenarioPrayer
import com.knockit.app.ui.theme.ScenarioSleep
import com.knockit.app.ui.theme.ScenarioWater

private data class ScenarioItem(
    val type: ReminderType,
    val icon: ImageVector,
    val color: Color,
)

private val scenarios = listOf(
    ScenarioItem(ReminderType.BABY, Icons.Filled.ChildCare, ScenarioFeeding),
    ScenarioItem(ReminderType.PRAYER, Icons.Filled.SelfImprovement, ScenarioPrayer),
    ScenarioItem(ReminderType.MEDICINE, Icons.Filled.Medication, ScenarioMedicine),
    ScenarioItem(ReminderType.WATER, Icons.Filled.WaterDrop, ScenarioWater),
    ScenarioItem(ReminderType.EXERCISE, Icons.Filled.FitnessCenter, ScenarioExercise),
    ScenarioItem(ReminderType.SLEEP, Icons.Filled.Bedtime, ScenarioSleep),
)

@Composable
fun QuickScenariosView(
    onScenarioClick: (ReminderType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Quick Add",
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(scenarios) { scenario ->
                ScenarioCard(
                    item = scenario,
                    onClick = { onScenarioClick(scenario.type) },
                )
            }
        }
    }
}

@Composable
private fun ScenarioCard(
    item: ScenarioItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(item.color.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(item.color.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.type.displayName,
                tint = item.color,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.type.displayName,
            style = MaterialTheme.typography.labelSmall.copy(
                color = item.color,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
            ),
            maxLines = 1,
        )
    }
}
