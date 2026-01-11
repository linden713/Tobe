package com.example.tobe

import android.app.Application
import android.content.Intent
import android.os.Build
import com.example.tobe.data.UserPreferencesRepository
import com.example.tobe.service.MonitoringService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TobeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Start the monitoring service if enabled
        val repository = UserPreferencesRepository(this)
        MainScope().launch {
            val prefs = repository.userPreferencesFlow.first()
            if (prefs.isMonitoringEnabled) {
                val intent = Intent(this@TobeApplication, MonitoringService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }
    }
}
