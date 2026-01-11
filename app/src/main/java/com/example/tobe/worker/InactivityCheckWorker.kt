package com.example.tobe.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tobe.MainActivity
import com.example.tobe.R
import com.example.tobe.data.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class InactivityCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = UserPreferencesRepository(context)

    override suspend fun doWork(): Result {
        val preferences = repository.userPreferencesFlow.first()
        val currentTime = System.currentTimeMillis()
        val timeoutMillis = preferences.timeoutHours * 60 * 60 * 1000L
        val inactiveDuration = currentTime - preferences.lastActiveTime

        // Warning threshold: 1 hour before timeout
        val warningThreshold = timeoutMillis - (60 * 60 * 1000L)

        // Notification logic
        if (inactiveDuration > warningThreshold && inactiveDuration < timeoutMillis) {
            sendWarningNotification()
        }

        // Action logic (SMS)
        if (inactiveDuration >= timeoutMillis) {
            // Check if we already sent SMS for this cycle?
            // Logic: we have a 'lastSmsSentTime'. 
            // If lastSmsSentTime > lastActiveTime, it means we already acted since the user was last active.
            
            if (preferences.lastSmsSentTime < preferences.lastActiveTime) {
                if (preferences.isSmsEnabled) {
                    sendSms(preferences.contactPhone, preferences.smsMessage)
                }
                repository.markSmsSent()
            }
        }

        return Result.success()
    }

    private fun sendWarningNotification() {
        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.tobe)
            .setContentTitle("你还好吗？")
            .setContentText("检测到长时间未操作，点击这里确认状态。")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun sendSms(phone: String, message: String) {
        if (phone.isBlank()) return

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val wakeLock = powerManager.newWakeLock(
            android.os.PowerManager.PARTIAL_WAKE_LOCK,
            "Tobe:SmsWakeLock"
        )

        try {
            // Wake up CPU to ensure SMS radio can work
            wakeLock.acquire(5000L /* 5 seconds */)
            
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager?.sendTextMessage(phone, null, message, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "Inactivity Alerts"
        val descriptionText = "Notifications for inactivity warnings"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "inactivity_channel"
        const val NOTIFICATION_ID = 1001
    }
}
