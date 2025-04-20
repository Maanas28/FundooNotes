package com.example.fundoonotes.common.database.repository.databridge

import android.content.Context
import android.util.Log
import com.example.fundoonotes.common.data.model.User
import com.example.fundoonotes.common.database.repository.interfaces.AccountRepository
import com.example.fundoonotes.common.util.managers.NetworkUtils.isOnline
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DataBridgeAuthRepository(
    context: Context
) : DataBridge<User>(context), AccountRepository {


    override fun fetchAccountDetails() = if (isOnline(context)) {
        firebaseAuth.fetchAccountDetails()
    } else {
        sqliteAccount.fetchAccountDetails()
    }

    fun saveUserLocally(user: User) {
        sqliteAccount.insertUser(user)
    }

    fun getLoggedInUser(
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d("DataBridge", "Fetching logged in user from Firestore")
        firebaseAuth.fetchUserDetailsOnce(onSuccess, onFailure)
    }
    override val accountDetails: StateFlow<User?> get() = if (isOnline(context)) firebaseAuth.accountDetails else sqliteAccount.accountDetails

    // Auth operations require online connectivity
    fun register(user: User, password: String, onComplete: (Boolean, String?) -> Unit) {
        if (!isOnline(context)) return onComplete(false, "No internet connection")

        firebaseAuth.registerWithEmail(user, password) { success, message ->
            onComplete(success, message)
        }
    }

    fun login(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        if (!isOnline(context)) return onComplete(false, "No internet connection")

        firebaseAuth.loginWithEmail(email, password) { success, message ->
            onComplete(success, message)
        }
    }

    fun registerWithGoogleCredential(
        credential: AuthCredential,
        userInfo: User,
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (!isOnline(context)) return onComplete(false, "No internet connection")
        firebaseAuth.loginWithGoogleCredential(credential, userInfo) { success, message ->
            if (success && userInfo.userId != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    sqliteAccount.saveUserLocally(userInfo.copy(userId = firebaseAuth.getCurrentUserId()!!))
                }
            }
            onComplete(success, message)
        }

    }

    fun loginWithGoogleCredential(
        credential: AuthCredential,
        userInfo: User? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (!isOnline(context)) return onComplete(false, "No internet connection")
        firebaseAuth.loginWithGoogleCredential(credential, userInfo) { success, message ->
            onComplete(success, message)
        }
    }

    fun logoutAndClearData(onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            // Clear all Room tables
            sqliteAccount.clearDatabase()

            // Sign out from Firebase
            signOut()

            // Notify ViewModel/UI
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }


}