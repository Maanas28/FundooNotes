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

        // Check permission and request if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !permissionManager.canScheduleExactAlarms()) {
            permissionManager.requestExactAlarmPermission()
            Log.d("ReminderScheduler", "Cannot schedule exact alarms - permission denied")
            return // Only return if we don't have permission
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("noteId", note.id)
            putExtra("noteTitle", note.title)
            putExtra("noteContent", note.content)
            putExtra("action", "REFRESH_UI_AND_NOTIFY")
        }

        // Use a unique identifier for the pending intent (hash of the note ID)
        val requestCode = note.id.hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
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

    // Add a method to cancel reminders
    fun cancelReminder(noteId: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val requestCode = noteId.hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        Log.d("ReminderScheduler", "Reminder cancelled for note ID: $noteId")
    }
}