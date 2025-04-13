package com.example.fundoonotes.UI.features.notes.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundoonotes.UI.data.model.User
import com.example.fundoonotes.UI.data.repository.DataBridgeNotesRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AccountViewModel(
    private val repository: DataBridgeNotesRepository
) : ViewModel() {

    constructor(context: Context) : this(DataBridgeNotesRepository(context))

    val accountDetails: StateFlow<User?> = repository.accountDetails

    fun fetchAccountDetails() {
        viewModelScope.launch {
            repository.fetchAccountDetails()
        }
    }
}
