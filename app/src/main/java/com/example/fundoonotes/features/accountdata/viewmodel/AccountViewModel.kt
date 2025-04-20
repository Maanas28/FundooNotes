package com.example.fundoonotes.features.accountdata.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundoonotes.common.data.model.User
import com.example.fundoonotes.common.database.repository.databridge.DataBridgeAuthRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AccountViewModel(
    private val repository: DataBridgeAuthRepository
) : ViewModel() {

    constructor(context: Context) : this(DataBridgeAuthRepository(context))

    val accountDetails: StateFlow<User?> = repository.accountDetails

    fun fetchAccountDetails() {
        viewModelScope.launch {
            repository.fetchAccountDetails()
        }
    }

    fun logout(onComplete: () -> Unit) {
        repository.logoutAndClearData(onComplete)
    }

}