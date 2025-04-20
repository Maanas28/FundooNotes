package com.example.fundoonotes.common.database.repository.interfaces

import com.example.fundoonotes.common.data.model.User
import kotlinx.coroutines.flow.StateFlow

interface AccountRepository {

    val accountDetails : StateFlow<User?>

    fun fetchAccountDetails()

}