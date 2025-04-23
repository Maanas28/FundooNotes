package com.example.fundoonotes.common.database.repository.firebase

import android.util.Log
import com.example.fundoonotes.common.data.model.User
import com.example.fundoonotes.common.database.repository.interfaces.AccountRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class FirebaseAuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) :
    AccountRepository {

    // Configure Firestore to disable offline persistence.
    private val firestore = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
            .build()
    }

    private val _accountDetails = MutableStateFlow<User?>(null)
    override val accountDetails: StateFlow<User?> get() = _accountDetails


    override fun fetchAccountDetails() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    _accountDetails.value = null
                    return@addSnapshotListener
                }
                // Assume only one document per userId
                val user = snapshot.documents.first().toObject(User::class.java)
                _accountDetails.value = user
            }
    }


    fun loginWithEmail(
        email: String,
        password: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    fetchAccountDetails()
                }
                onComplete(task.isSuccessful, task.exception?.message)
            }
    }

    fun registerWithEmail(
        user: User,
        password: String,
        onComplete: (Boolean, String?) -> Unit
    ) {

        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    val userId = firebaseUser?.uid ?: return@addOnCompleteListener onComplete(false, "User ID is null")

                    val userToSave = user.copy(userId = userId)

                    firestore.collection("users")
                        .document(userId)
                        .set(userToSave)
                        .addOnSuccessListener {
                            onComplete(true, null)
                        }
                        .addOnFailureListener { e ->
                            onComplete(false, e.message)
                        }

                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    fun loginWithGoogleCredential(
        credential: AuthCredential,
        userInfo: User?, // If null => login, else => register
        onComplete: (Boolean, String?) -> Unit
    ) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    val uid = firebaseUser?.uid ?: return@addOnCompleteListener onComplete(false, "No UID")

                    if (userInfo != null) {
                        // REGISTER flow
                        val userToSave = userInfo.copy(userId = uid)
                        firestore.collection("users")
                            .document(uid)
                            .set(userToSave)
                            .addOnSuccessListener { onComplete(true, null) }
                            .addOnFailureListener { e -> onComplete(false, e.message) }
                    } else {
                        // LOGIN flow
                        onComplete(true, null)
                    }
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    fun fetchUserDetailsOnce(
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return onFailure(Exception("User not logged in"))

        firestore.collection("users")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onFailure(Exception("User not found in Firestore"))
                } else {
                    val user = snapshot.documents.first().toObject(User::class.java)
                    if (user != null) onSuccess(user)
                    else onFailure(Exception("Failed to parse user document"))
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun getCurrentFirebaseUser() : FirebaseUser? {
        return auth.currentUser
    }

}