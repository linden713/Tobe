package com.example.tobe

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.tobe.data.UserPreferencesRepository
import com.example.tobe.service.MonitoringService
import com.example.tobe.ui.MainViewModel
import com.example.tobe.ui.TobeApp
import com.example.tobe.ui.theme.TobeTheme
import com.example.tobe.worker.InactivityCheckWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: MainViewModel

    // Move launcher to member variable
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.SEND_SMS] == true) {
            viewModel.setSmsEnabled(true)
        }
        // After system SMS/Notification prompts, ask for battery optimization
        requestBatteryOptimizationExemption()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Manual Dependency Injection
        val repository = UserPreferencesRepository(applicationContext)
        val factory = MainViewModel.Factory(repository)
        viewModel = androidx.lifecycle.ViewModelProvider(this, factory)[MainViewModel::class.java]

        setupWorkManager()

        setContent {
            TobeTheme {
                var showRationale by remember { 
                    mutableStateOf(needsPermissions()) 
                }

                if (showRationale) {
                    AlertDialog(
                        onDismissRequest = { /* Prevent dismiss */ },
                        title = { Text("需要必要权限", fontWeight = FontWeight.Bold) },
                        text = {
                            Text("为了确保“活着呢”能在您失联时准确发出求救短信，我们需要以下权限：\n\n" +
                                 "• 发送短信：用于在超时后联系紧急联系人。\n" +
                                 "• 通知权限：用于发送签到提示和预警通知。\n" +
                                 "• 忽略电池优化：确保应用在息屏时不会被强制休眠。")
                        },
                        confirmButton = {
                            Button(onClick = {
                                showRationale = false
                                triggerPermissionRequests()
                            }) {
                                Text("我知道了")
                            }
                        }
                    )
                }

                val userPrefs by viewModel.userPreferences.collectAsState()
                
                LaunchedEffect(userPrefs?.isMonitoringEnabled) {
                    val enabled = userPrefs?.isMonitoringEnabled ?: true
                    val intent = Intent(this@MainActivity, MonitoringService::class.java)
                    if (enabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent)
                        } else {
                            startService(intent)
                        }
                    } else {
                        stopService(intent)
                    }
                }

                TobeApp(viewModel)
            }
        }
    }

    private fun needsPermissions(): Boolean {
        val smsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm.isIgnoringBatteryOptimizations(packageName)
        } else true

        return !smsGranted || !notificationGranted || !batteryIgnored
    }

    private fun triggerPermissionRequests() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // If already have SMS/Notification, just check battery
            requestBatteryOptimizationExemption()
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName = packageName
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }

    private fun setupWorkManager() {
        // Enqueue periodic work
        // Interval: 1 hour (Minimum is 15 mins)
        val workRequest = PeriodicWorkRequestBuilder<InactivityCheckWorker>(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "InactivityMonitor",
            ExistingPeriodicWorkPolicy.KEEP, 
            workRequest
        )
    }
}
