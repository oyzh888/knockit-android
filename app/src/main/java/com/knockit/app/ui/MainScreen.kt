package com.knockit.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.knockit.app.data.model.Reminder
import com.knockit.app.ui.components.HeaderView
import com.knockit.app.ui.components.QuickScenariosView
import com.knockit.app.ui.components.ReminderEditSheet
import com.knockit.app.ui.components.ReminderInputView
import com.knockit.app.ui.components.ReminderListView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ReminderViewModel,
    modifier: Modifier = Modifier,
) {
    val inputText by viewModel.inputText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val notificationEnabled by viewModel.notificationEnabled.collectAsState()
    val reminders by viewModel.reminders.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var editingReminder by remember { mutableStateOf<Reminder?>(null) }

    // Check notification permission on launch
    LaunchedEffect(Unit) {
        viewModel.checkNotificationStatus()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. Header
            HeaderView(
                notificationEnabled = notificationEnabled,
                onEnableClick = { viewModel.requestNotificationPermission() },
                modifier = Modifier.fillMaxWidth(),
            )

            // 2. Input Box
            ReminderInputView(
                inputText = inputText,
                isLoading = isLoading,
                errorMessage = errorMessage,
                snackbarHostState = snackbarHostState,
                onTextChanged = viewModel::onInputTextChanged,
                onSubmit = viewModel::submitInput,
                onErrorDismiss = viewModel::clearError,
                modifier = Modifier.fillMaxWidth(),
            )

            // 3. Quick Scenarios
            QuickScenariosView(
                onScenarioClick = viewModel::handleQuickScenario,
                modifier = Modifier.fillMaxWidth(),
            )

            // 4. Reminder List
            ReminderListView(
                reminders = reminders,
                onToggle = viewModel::toggleReminder,
                onDelete = viewModel::deleteReminder,
                onEdit = { reminder ->
                    editingReminder = reminder
                    scope.launch { sheetState.show() }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }

    // Edit Sheet (shown when editingReminder != null)
    editingReminder?.let { reminder ->
        ReminderEditSheet(
            reminder = reminder,
            sheetState = sheetState,
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                    editingReminder = null
                }
            },
            onSave = { title, triggerAt, repeatRule, intervalMinutes ->
                viewModel.updateReminder(
                    reminder = reminder,
                    title = title,
                    triggerAt = triggerAt,
                    repeatRule = repeatRule,
                    intervalMinutes = intervalMinutes,
                )
                scope.launch {
                    sheetState.hide()
                    editingReminder = null
                }
            },
        )
    }
}
