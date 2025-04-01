package com.example.fundoonotes.UI.features.auth.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fundoonotes.UI.data.repository.AuthRepository
import com.example.fundoonotes.UI.features.auth.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

class AuthViewModelFactory(private val auth: FirebaseAuth) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = AuthRepository(auth)
        return AuthViewModel(repo) as T
    }
}
