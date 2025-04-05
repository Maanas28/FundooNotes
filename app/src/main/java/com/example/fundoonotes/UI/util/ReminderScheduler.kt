package com.example.fundoonotes.UI.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.features.reminders.ReminderReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderScheduler(private val context: Context) {

    fun scheduleReminder(note: Note, triggerTimeMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            requestExactAlarmPermission(context)
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("noteTitle", note.title)
            putExtra("noteContent", note.content)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
            val formatted = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())
            Log.d("ReminderScheduler", "Reminder scheduled at: ${formatted.format(Date(triggerTimeMillis))} for note: ${note.title}")

        } catch (e: SecurityException) {
            Log.e("ReminderScheduler", "SecurityException: ${e.message}")
        }
    }

    private fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
