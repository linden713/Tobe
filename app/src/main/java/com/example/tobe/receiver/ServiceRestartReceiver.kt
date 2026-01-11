package com.example.tobe.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.tobe.data.UserPreferencesRepository
import com.example.tobe.service.MonitoringService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ServiceRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val repository = UserPreferencesRepository(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = repository.userPreferencesFlow.first()
                if (prefs.isMonitoringEnabled) {
                    val serviceIntent = Intent(context, MonitoringService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
