// File: com/example/medicare/receiver/MedicineReminderReceiver.kt

package com.example.medicare.receiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.example.medicare.MainActivity
import com.example.medicare.R
import com.example.medicare.data.PreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MedicineReminderReceiver : BroadcastReceiver() {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra("medicineName") ?: "your medicine"
        val prefs = PreferenceStore(context)

        CoroutineScope(Dispatchers.Default).launch {
            val ringtoneUriString = prefs.ringtoneUri.first()
            val isVibrationEnabled = prefs.vibrationEnabled.first()

            val soundUri: Uri = if (ringtoneUriString.isNotBlank())
                ringtoneUriString.toUri()
            else
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val channelId = "medicine_channel"

            // Safe vibration
            if (isVibrationEnabled && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(3000, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(3000)
                }
            }

            // Recreate channel to update sound if changed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.deleteNotificationChannel(channelId) // refresh if custom ringtone
                val channel = NotificationChannel(
                    channelId,
                    "Medicine Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminder to take your medicine"
                    enableVibration(isVibrationEnabled)
                    if (isVibrationEnabled) vibrationPattern = longArrayOf(0, 500, 500, 1000)
                    setSound(
                        soundUri,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Tap to open app
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Action: Taken
            val takenIntent = Intent(context, ActionReceiver::class.java).apply {
                action = "ACTION_TAKEN"
                putExtra("medicineName", medicineName)
            }
            val takenPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                takenIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Action: Snooze
            val snoozeIntent = Intent(context, ActionReceiver::class.java).apply {
                action = "ACTION_SNOOZE"
                putExtra("medicineName", medicineName)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                snoozeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Build the notification
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your icon
                .setContentTitle("⏰ Time to take your medicine")
                .setContentText("Don't forget: $medicineName")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "Taken", takenPendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "Snooze", snoozePendingIntent)

            if (isVibrationEnabled) {
                builder.setVibrate(longArrayOf(0, 500, 500, 1000))
            }

            NotificationManagerCompat.from(context).notify(medicineName.hashCode(), builder.build())
        }
    }
}
