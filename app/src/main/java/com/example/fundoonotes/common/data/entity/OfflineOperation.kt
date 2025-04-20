package com.example.fundoonotes.common.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_operations")
data class OfflineOperation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val operationType: String, // "ADD", "UPDATE", "DELETE", etc.
    val entityType: String,    // "NOTE", "LABEL", etc.
    val entityId: String,      // ID of the affected entity
    val timestamp: Long,       // When the operation occurred
    val payload: String        // JSON data of the entity
)