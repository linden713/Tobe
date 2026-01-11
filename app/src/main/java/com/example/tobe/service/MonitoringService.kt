package com.example.tobe.service

import android.app.*
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.tobe.MainActivity
import com.example.tobe.R
import com.example.tobe.receiver.ServiceRestartReceiver
import com.example.tobe.receiver.UserPresentReceiver

class MonitoringService : Service() {

    private val userPresentReceiver = UserPresentReceiver()
    private val CHANNEL_ID = "monitoring_service_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Dynamically register receiver here
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(userPresentReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Force post/update notification whenever the service is "started"
        val notification = createNotification()
        startForeground(1, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(userPresentReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val startOfYear2026 = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.JANUARY, 1, 0, 0, 0)
        }.timeInMillis
        val diffMillis = System.currentTimeMillis() - startOfYear2026
        val hoursLived = diffMillis / (1000 * 60 * 60)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Create a delete intent that fires when the notification is swiped away
        val deleteIntent = PendingIntent.getBroadcast(
            this, 0, Intent(this, ServiceRestartReceiver::class.java),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("活着守护中")
            .setContentText("在2026年您已经活了 $hoursLived 小时")
            .setSmallIcon(R.drawable.tobe)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(deleteIntent) // This triggers when user swipes
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "守护服务频道",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
