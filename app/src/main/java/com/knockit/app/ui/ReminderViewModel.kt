package com.knockit.app.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knockit.app.data.db.KnockitDatabase
import com.knockit.app.data.model.Reminder
import com.knockit.app.data.model.ReminderType
import com.knockit.app.data.model.RepeatRule
import com.knockit.app.data.repository.ReminderRepository
import com.knockit.app.services.ApiKeys
import com.knockit.app.services.DateUtils
import com.knockit.app.services.GeminiService
import com.knockit.app.services.LocationService
import com.knockit.app.services.NotificationPermissionCallback
import com.knockit.app.services.NotificationService
import com.knockit.app.services.PrayerService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReminderRepository by lazy {
        val dao = KnockitDatabase.getInstance(application).reminderDao()
        ReminderRepository(dao)
    }

    // -------------------------------------------------------------------------
    // UI State
    // -------------------------------------------------------------------------

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _notificationEnabled = MutableStateFlow(false)
    val notificationEnabled: StateFlow<Boolean> = _notificationEnabled.asStateFlow()

    val reminders: StateFlow<List<Reminder>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Event: asks the Activity to show the POST_NOTIFICATIONS permission dialog
    private val _requestPermissionEvent = MutableSharedFlow<Unit>()
    val requestPermissionEvent: SharedFlow<Unit> = _requestPermissionEvent.asSharedFlow()

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    /**
     * Sends [inputText] to Gemini AI, parses the result into Reminder(s),
     * saves them locally, and schedules notifications.
     */
    fun submitInput() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val parsedList = GeminiService.parseReminder(text, ApiKeys.GEMINI_API_KEY)
                for (parsed in parsedList) {
                    val triggerAt = try {
                        DateUtils.parseIso8601ToMillis(parsed.triggerAt)
                    } catch (_: Exception) {
                        System.currentTimeMillis() + 3_600_000L
                    }
                    val reminder = Reminder(
                        id = UUID.randomUUID().toString(),
                        title = parsed.title,
                        triggerAt = triggerAt,
                        repeatRule = parsed.repeatRule ?: RepeatRule.NONE,
                        intervalMinutes = parsed.intervalMinutes,
                        createdAt = System.currentTimeMillis(),
                        isActive = true,
                        type = parsed.type ?: ReminderType.CUSTOM.rawValue,
                    )
                    repository.insert(reminder)
                    NotificationService.scheduleNotification(getApplication(), reminder)
                }
                _inputText.value = ""
                vibrate()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create reminder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Creates a reminder from a quick-start template.
     * Prayer type triggers GPS-based prayer time scheduling.
     */
    fun handleQuickScenario(type: ReminderType) {
        if (type == ReminderType.PRAYER) {
            handlePrayerScenario()
            return
        }
        viewModelScope.launch {
            try {
                val intervalMs = type.defaultIntervalMinutes?.let { it.toLong() * 60_000L }
                    ?: 3_600_000L
                val triggerAt = System.currentTimeMillis() + intervalMs
                val reminder = Reminder(
                    id = UUID.randomUUID().toString(),
                    title = type.defaultTitle,
                    triggerAt = triggerAt,
                    repeatRule = type.defaultRepeatRule,
                    intervalMinutes = type.defaultIntervalMinutes,
                    createdAt = System.currentTimeMillis(),
                    isActive = true,
                    type = type.rawValue,
                )
                repository.insert(reminder)
                NotificationService.scheduleNotification(getApplication(), reminder)
                vibrate()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create reminder: ${e.message}"
            }
        }
    }

    private fun handlePrayerScenario() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val location = LocationService.requestLocation(getApplication())
                val timings = PrayerService.fetchPrayerTimes(
                    location.latitude,
                    location.longitude
                )

                val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
                val calendar = Calendar.getInstance()
                val today = Calendar.getInstance()

                for (prayer in timings.allPrayers) {
                    val parsedTime = timeFmt.parse(prayer.time) ?: continue
                    val prayerCal = Calendar.getInstance().apply { time = parsedTime }

                    calendar.set(Calendar.YEAR, today.get(Calendar.YEAR))
                    calendar.set(Calendar.MONTH, today.get(Calendar.MONTH))
                    calendar.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH))
                    calendar.set(Calendar.HOUR_OF_DAY, prayerCal.get(Calendar.HOUR_OF_DAY))
                    calendar.set(Calendar.MINUTE, prayerCal.get(Calendar.MINUTE))
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)

                    // If already past today, schedule for tomorrow
                    if (calendar.timeInMillis <= System.currentTimeMillis()) {
                        calendar.add(Calendar.DAY_OF_MONTH, 1)
                    }

                    val reminder = Reminder(
                        id = UUID.randomUUID().toString(),
                        title = "${prayer.name} Prayer",
                        triggerAt = calendar.timeInMillis,
                        repeatRule = RepeatRule.DAILY,
                        intervalMinutes = null,
                        createdAt = System.currentTimeMillis(),
                        isActive = true,
                        type = ReminderType.PRAYER.rawValue,
                    )
                    repository.insert(reminder)
                    NotificationService.scheduleNotification(getApplication(), reminder)
                }
                vibrate()
            } catch (e: Exception) {
                _errorMessage.value = "Could not load prayer times: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleReminder(reminder: Reminder) {
        viewModelScope.launch {
            val updated = reminder.copy(isActive = !reminder.isActive)
            repository.update(updated)
            if (updated.isActive) {
                NotificationService.scheduleNotification(getApplication(), updated)
            } else {
                NotificationService.cancelNotification(getApplication(), updated.id)
            }
            vibrate(30)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            NotificationService.cancelNotification(getApplication(), reminder.id)
            repository.delete(reminder)
            vibrate(30)
        }
    }

    fun updateReminder(
        reminder: Reminder,
        title: String? = null,
        triggerAt: Long? = null,
        repeatRule: String? = null,
        intervalMinutes: Int? = reminder.intervalMinutes,
        isActive: Boolean? = null,
    ) {
        viewModelScope.launch {
            NotificationService.cancelNotification(getApplication(), reminder.id)
            val updated = reminder.copy(
                title = title ?: reminder.title,
                triggerAt = triggerAt ?: reminder.triggerAt,
                repeatRule = repeatRule ?: reminder.repeatRule,
                intervalMinutes = intervalMinutes,
                isActive = isActive ?: reminder.isActive,
            )
            repository.update(updated)
            if (updated.isActive) {
                NotificationService.scheduleNotification(getApplication(), updated)
            }
            vibrate()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun checkNotificationStatus() {
        _notificationEnabled.value = NotificationService.checkPermission(getApplication())
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Permission is implicit on older Android versions
            _notificationEnabled.value = true
            return
        }
        viewModelScope.launch {
            _requestPermissionEvent.emit(Unit)
        }
    }

    /** Called by MainActivity after the user responds to the permission dialog. */
    fun onPermissionResult(granted: Boolean) {
        _notificationEnabled.value = granted
        NotificationPermissionCallback.dispatch(
            NotificationService.REQUEST_CODE_NOTIFICATIONS,
            if (granted) intArrayOf(0) else intArrayOf(-1)
        )
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun vibrate(durationMs: Long = 50L) {
        val vibrator = getApplication<Application>()
            .getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }
}
