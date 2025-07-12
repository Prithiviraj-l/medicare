// File: com/example/medicare/receiver/ActionReceiver.kt

package com.example.medicare.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.example.medicare.receiver.MedicineReminderReceiver
import java.util.*

class ActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra("medicineName") ?: "Medicine"

        when (intent.action) {
            // ✅ User clicked "Taken"
            "ACTION_TAKEN" -> {
                Toast.makeText(
                    context,
                    "$medicineName marked as taken ✅",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // 🔁 User clicked "Snooze"
            "ACTION_SNOOZE" -> {
                val snoozeTime = Calendar.getInstance().apply {
                    add(Calendar.MINUTE, 10) // 10 minute snooze
                }

                val snoozeIntent = Intent(context, MedicineReminderReceiver::class.java).apply {
                    putExtra("medicineName", medicineName)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    medicineName.hashCode(), // Unique per medicine
                    snoozeIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                // ⏰ Exact Alarm Handling for Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            snoozeTime.timeInMillis,
                            pendingIntent
                        )
                        Toast.makeText(context, "Snoozed for 10 minutes 💤", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Alarm permission not granted. Enable exact alarms in Settings ⚠️",
                            Toast.LENGTH_LONG
                        ).show()

                        // Optionally open settings
                        val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(settingsIntent)
                    }
                } else {
                    // Pre-Android 12
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        snoozeTime.timeInMillis,
                        pendingIntent
                    )
                    Toast.makeText(context, "Snoozed for 10 minutes 💤", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
