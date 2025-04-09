package com.example.fundoonotes.UI.features.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.fundoonotes.R

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.getStringExtra("action")
        val noteTitle = intent.getStringExtra("noteTitle") ?: "Reminder"
        val noteContent = intent.getStringExtra("noteContent") ?: "You have a note reminder"

        Log.d("ReminderReceiver", "Reminder received for note: $noteTitle")

        if (action == "REFRESH_UI_AND_NOTIFY") {
            showNotification(context, noteTitle, noteContent)

            // Broadcast to UI with note ID
            val noteId = intent.getStringExtra("noteId")
            val uiRefreshIntent = Intent("com.example.fundoonotes.REFRESH_UI").apply {
                putExtra("noteId", noteId)
            }
            context.sendBroadcast(uiRefreshIntent)
        }
    }

    private fun showNotification(context: Context, title: String, content: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "reminder_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reminder Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
