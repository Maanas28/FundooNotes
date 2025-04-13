package com.example.fundoonotes.UI.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "labels")
data class LabelEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String
)