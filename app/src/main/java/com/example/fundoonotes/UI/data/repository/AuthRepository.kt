package com.example.fundoonotes.UI.data.repository

import android.util.Log
import com.example.fundoonotes.UI.data.model.User
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository(private val auth: FirebaseAuth) {

    private val firestore = FirebaseFirestore.getInstance()

    fun loginWithEmail(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful, task.exception?.message)
            }
    }

    fun registerWithEmail(user: User, password: String, onComplete: (Boolean, String?) -> Unit) {
        Log.d("AuthRepo", "Starting registration for email: ${user.email}")

        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    val userId = firebaseUser?.uid ?: return@addOnCompleteListener onComplete(false, "User ID is null")

                    Log.d("AuthRepo", "Firebase user created with UID: $userId")

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

    fun loginWithGoogle(credential: AuthCredential, onComplete: (Boolean, String?) -> Unit) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful, task.exception?.message)
            }
    }
}
