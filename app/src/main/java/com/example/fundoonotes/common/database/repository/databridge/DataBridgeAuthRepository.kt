package com.example.fundoonotes.common.database.repository.databridge

import android.content.Context
import com.example.fundoonotes.R
import com.example.fundoonotes.common.data.model.User
import com.example.fundoonotes.common.database.repository.interfaces.AccountRepository
import com.example.fundoonotes.common.util.managers.NetworkUtils.isOnline
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DataBridgeAuthRepository(
    context: Context
) : DataBridge<User>(context), AccountRepository {

    init {
        // Ensure local user is loaded when offline
        if (!isOnline(context)) {
            sqliteAccount.fetchAccountDetails()
        }
    }

    override fun fetchAccountDetails() = if (isOnline(context)) {
        firebaseAuth.fetchAccountDetails()
    } else {
        sqliteAccount.fetchAccountDetails()
    }
    override val accountDetails: StateFlow<User?> get() = if (isOnline(context)) firebaseAuth.accountDetails else sqliteAccount.accountDetails

    // Auth operations require online connectivity
    fun register(user: User, password: String, onComplete: (Boolean, String?) -> Unit) {
        if (!isOnline(context)) return return onComplete(false, context.getString(R.string.no_internet_connection))

        firebaseAuth.registerWithEmail(user, password) { success, message ->
            onComplete(success, message)
        }
    }

    fun login(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        if (!isOnline(context)) return onComplete(false, context.getString(R.string.no_internet_connection))


        firebaseAuth.loginWithEmail(email, password) { success, message ->
            onComplete(success, message)
        }
    }

    fun registerWithGoogleCredential(
        credential: AuthCredential,
        userInfo: User,
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (!isOnline(context)) return onComplete(false, context.getString(R.string.no_internet_connection))
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
        if (!isOnline(context)) return onComplete(false, context.getString(R.string.no_internet_connection))
        firebaseAuth.loginWithGoogleCredential(credential, userInfo) { success, message ->
            onComplete(success, message)
        }
    }

    fun logoutAndClearData(onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            sqliteAccount.clearDatabase()
            signOut()
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    fun saveUserLocally(user: User) {
        sqliteAccount.saveUserLocally(user)
    }

    fun getLoggedInUser(
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (isOnline(context)) {
            firebaseAuth.fetchUserDetailsOnce(onSuccess, onFailure)
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                sqliteAccount.accountDetails.collect { user ->
                    if (user != null) {
                        withContext(Dispatchers.Main) { onSuccess(user) }
                        cancel() // stop collecting after success
                    }
                }
            }
        }
    }

    fun isAuthenticated(onResult: (Boolean) -> Unit) {
        // First check SQLite repository for cached user
        CoroutineScope(Dispatchers.Main).launch {
            val localUser = sqliteAccount.accountDetails.value

            if (localUser != null) {
                // We have a cached authenticated user
                withContext(Dispatchers.Main) { onResult(true) }
                return@launch
            }

            // If no local user and we're offline, we're definitely not authenticated
            if (!isOnline(context)) {
                withContext(Dispatchers.Main) { onResult(false) }
                return@launch
            }

            // If we're online, check Firebase
            withContext(Dispatchers.Main) {
                val firebaseUser = firebaseAuth.getCurrentFirebaseUser()
                onResult(firebaseUser != null)
            }
        }
    }


}