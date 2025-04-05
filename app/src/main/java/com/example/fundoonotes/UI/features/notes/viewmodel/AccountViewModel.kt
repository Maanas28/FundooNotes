package com.example.fundoonotes.UI.features.notes.viewmodel

import androidx.lifecycle.ViewModel
import com.example.fundoonotes.UI.data.model.User
import com.example.fundoonotes.UI.data.repository.FirebaseNotesRepository
import com.example.fundoonotes.UI.data.repository.NotesRepository
import kotlinx.coroutines.flow.StateFlow

class AccountViewModel (
    private val repository: NotesRepository = FirebaseNotesRepository()
) : ViewModel() {

    val accountDetails : StateFlow<User?> =  repository.accountDetails

    fun fetchAccountDetails() {
        repository.fetchAccountDetails()
    }
}
