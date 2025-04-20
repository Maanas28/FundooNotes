package com.example.fundoonotes.common.data.model

data class User(
    var userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val profileImage : String? = ""
)
