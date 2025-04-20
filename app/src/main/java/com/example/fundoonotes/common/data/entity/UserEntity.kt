package com.example.fundoonotes.common.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val profileImage: String?
)