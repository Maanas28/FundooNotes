package com.example.fundoonotes.features.reminders.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.fundoonotes.common.data.model.Note
import com.example.fundoonotes.common.util.managers.PermissionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderScheduler(private val context: Context) {

    fun scheduleReminder(note: Note, triggerTimeMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val permissionManager = PermissionManager(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && permissionManager.canScheduleExactAlarms()) {
            permissionManager.requestExactAlarmPermission()
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("noteId", note.id)
            putExtra("noteTitle", note.title)
            putExtra("noteContent", note.content)
            putExtra("action", "REFRESH_UI_AND_NOTIFY")
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
            Log.d("ReminderScheduler", "Reminder scheduled at: ${formatted.format(
                Date(triggerTimeMillis)
            )} for note: ${note.title}")

        } catch (e: SecurityException) {
            Log.e("ReminderScheduler", "SecurityException: ${e.message}")
        }
    }
}