package com.example.tobe.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.Display
import com.example.tobe.data.UserPreferencesRepository
import com.example.tobe.service.MonitoringService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UserPresentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_USER_PRESENT || 
            action == Intent.ACTION_SCREEN_ON || 
            action == Intent.ACTION_BOOT_COMPLETED) {
            
            val pendingResult = goAsync()
            val repository = UserPreferencesRepository(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.updateLastActiveTime()
                    
                    // Only restart service if monitoring is enabled
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
}
