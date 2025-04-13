package com.example.fundoonotes.UI.data.mappers

import com.example.fundoonotes.UI.data.entity.*
import com.example.fundoonotes.UI.data.model.*
import com.google.firebase.Timestamp

fun Note.toEntity(): NoteEntity = NoteEntity(
    id, userId, title, content, timestamp.seconds, archived, deleted, inBin, hasReminder, reminderTime, labels.joinToString(",")
)

fun NoteEntity.toDomain(): Note = Note(
    userId, id, title, content, Timestamp(timestamp, 0), archived, deleted, inBin, hasReminder, reminderTime, labels.split(",").filter { it.isNotBlank() }
)

fun User.toEntity(): UserEntity = UserEntity(userId, firstName, lastName, email, profileImage)

fun UserEntity.toDomain(): User = User(userId, firstName, lastName, email, profileImage)

fun Label.toEntity(): LabelEntity = LabelEntity(id, userId, name)

fun LabelEntity.toDomain(): Label = Label(userId, id, name)
