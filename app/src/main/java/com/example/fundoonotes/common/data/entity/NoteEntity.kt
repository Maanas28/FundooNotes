package com.example.fundoonotes.common.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val archived: Boolean,
    val deleted: Boolean,
    val inBin: Boolean,
    val hasReminder: Boolean,
    val reminderTime: Long?,
    val labels: String // comma-separated
)