// File: com/example/medicare/ReminderScheduler.kt

package com.example.medicare.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.*

object ReminderScheduler {

    @RequiresApi(Build.VERSION_CODES.M)
    fun scheduleMedicineReminder(context: Context, calendar: Calendar, medicineName: String) {
        val intent = Intent(context, MedicineReminderReceiver::class.java).apply {
            putExtra("medicineName", medicineName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicineName.hashCode(), // Ensures uniqueness per medicine
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        if (alarmManager == null) {
            // Fallback in case AlarmManager couldn't be retrieved
            return
        }

        // 🔒 Android 12+ requires permission to schedule exact alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                // Optional: prompt user to allow exact alarms in system settings
                // Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM (API 31+)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
}
