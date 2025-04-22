package com.example.fundoonotes.features.reminders.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.fundoonotes.common.util.managers.NotificationManager

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.getStringExtra("action")
        val noteTitle = intent.getStringExtra("noteTitle") ?: "Reminder"
        val noteContent = intent.getStringExtra("noteContent") ?: "You have a note reminder"

        Log.d("ReminderReceiver", "Reminder received for note: $noteTitle")

        if (action == "REFRESH_UI_AND_NOTIFY") {
            NotificationManager.showNotification(context, noteTitle, noteContent)

            // Broadcast to UI with note ID
            val noteId = intent.getStringExtra("noteId")
            val uiRefreshIntent = Intent("com.example.fundoonotes.REFRESH_UI").apply {
                putExtra("noteId", noteId)
            }
            context.sendBroadcast(uiRefreshIntent)
        }
    }
}
