package com.example.fundoonotes.UI.data.model

import com.google.firebase.Timestamp


data class Note(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val archived: Boolean = false,
    val deleted : Boolean = false,
    val inBin : Boolean = false,
    val hasReminder : Boolean = false
)
