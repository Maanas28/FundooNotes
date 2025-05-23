package com.example.fundoonotes.common.data.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
data class Note(
    val userId: String = "",
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val archived: Boolean = false,
    val deleted: Boolean = false,
    val inBin: Boolean = false,
    val hasReminder: Boolean = false,
    val reminderTime: Long? = null,
    val labels: List<String> = emptyList()
) : Parcelable
