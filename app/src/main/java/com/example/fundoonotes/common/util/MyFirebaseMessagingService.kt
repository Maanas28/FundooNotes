package com.example.fundoonotes.common.util

import android.util.Log
import com.example.fundoonotes.common.util.managers.NotificationManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService  : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("MyFirebaseMessagingService", "Refreshed token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("MyFirebaseMessagingService", "Message received: ${message.data}")

        val title = message.data["title"] ?: "Reminder"
        val content = message.data["content"] ?: "You have a reminder from Fundoo Notes"

        NotificationManager.showNotification(
            applicationContext, title, content
        )
    }
}