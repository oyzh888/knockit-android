package com.knockit.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.knockit.app.services.NotificationPermissionCallback
import com.knockit.app.services.NotificationService
import com.knockit.app.ui.MainScreen
import com.knockit.app.ui.ReminderViewModel
import com.knockit.app.ui.theme.BackgroundDark
import com.knockit.app.ui.theme.KnockitTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: ReminderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create the notification channel (no-op below API 26)
        NotificationService.createNotificationChannel(this)

        // Listen for permission request events emitted by the ViewModel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.requestPermissionEvent.collect {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            NotificationService.REQUEST_CODE_NOTIFICATIONS
                        )
                    }
                }
            }
        }

        setContent {
            KnockitTheme {
                MainScreen(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundDark),
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.checkNotificationStatus()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NotificationService.REQUEST_CODE_NOTIFICATIONS) {
            val granted = grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
            viewModel.onPermissionResult(granted)
        }
        NotificationPermissionCallback.dispatch(requestCode, grantResults)
    }
}
