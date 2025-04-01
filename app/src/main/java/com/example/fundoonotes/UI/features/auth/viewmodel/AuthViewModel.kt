package com.example.fundoonotes.UI.features.auth.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.fundoonotes.UI.data.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.GoogleAuthProvider

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    val authResult = MutableLiveData<Pair<Boolean, String?>>()

    fun login(email: String, password: String) {
        repository.loginWithEmail(email, password) { success, message ->
            authResult.postValue(Pair(success, message))
        }
    }

    fun register(email: String, password: String) {
        repository.registerWithEmail(email, password) { success, message ->
            authResult.postValue(Pair(success, message))
        }
    }

    fun loginWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        repository.loginWithGoogle(credential) { success, message ->
            authResult.postValue(Pair(success, message))
        }
    }
}
