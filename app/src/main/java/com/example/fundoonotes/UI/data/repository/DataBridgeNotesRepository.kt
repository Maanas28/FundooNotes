package com.example.fundoonotes.UI.data.repository

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.fundoonotes.UI.data.NotesDatabase
import com.example.fundoonotes.UI.data.SyncManager
import com.example.fundoonotes.UI.data.entity.OfflineOperation
import com.example.fundoonotes.UI.data.model.*
import com.example.fundoonotes.UI.data.ConnectivityManager
import com.example.fundoonotes.UI.util.ReminderScheduler
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DataBridgeNotesRepository(
    context: Context
) : NotesRepository {

    private val firebase = FirebaseNotesRepository()
    private val firebaseAuth = FirebaseAuthRepository(FirebaseAuth.getInstance())


    private val sqlite = SQLiteNotesRepository(
        noteDao = NotesDatabase.getInstance(context).noteDao(),
        labelDao = NotesDatabase.getInstance(context).labelDao(),
        userDao = NotesDatabase.getInstance(context).userDao(),
        offlineOperationDao = NotesDatabase.getInstance(context).offlineOperationDao(),
        scope = CoroutineScope(Dispatchers.IO)
    )


    private val appContext = context.applicationContext

    private val offlineSyncManager = SyncManager(sqlite, firebase, firebaseAuth)

    private fun trackOfflineOperation(type: String, entityType: String, entityId: String, entity: Any) {
        val gson = Gson() // Gson is a popular library for converting Java/Kotlin objects into JSON strings and vice versa.
        val payload = gson.toJson(entity)
        val offlineOp = OfflineOperation(
            operationType = type,
            entityType = entityType,
            entityId = entityId,
            timestamp = System.currentTimeMillis(),
            payload = payload
        )
        sqlite.saveOfflineOperation(offlineOp) {
            Log.d("DataBridge", "Tracked offline operation: $type for $entityType with id $entityId")
        }
    }

    fun syncOfflineChanges() {
        offlineSyncManager.syncOfflineChanges()
    }

    fun syncOnlineChanges(
        context: Context,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val connectivityManager = ConnectivityManager(context, this)

        if (!connectivityManager.isNetworkAvailable()) {
            // Post Toast to main thread safely
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            }
            onFailure(Exception("No internet connection"))
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                firebase.fetchUserDetailsOnce(
                    onSuccess = { user ->
                        // Still background thread; pass to SyncManager
                        offlineSyncManager.syncOnlineChanges(user, onSuccess, onFailure)
                    },
                    onFailure = { ex ->
                        // We CANNOT call withContext(Main), so use launch again
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, "User details not found", Toast.LENGTH_SHORT).show()
                        }
                        onFailure(ex)
                    }
                )
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    fun setUpObservers(){
        sqlite.setUpObservers()
    }


    fun saveUserLocally(user: User) {
        sqlite.insertUser(user)
    }

    fun getLoggedInUser(
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d("DataBridge", "Fetching logged in user from Firestore")
        firebase.fetchUserDetailsOnce(onSuccess, onFailure)
    }

    override val notes: StateFlow<List<Note>> get() = if (isOnline()) firebase.notes else sqlite.notes
    override val archivedNotes: StateFlow<List<Note>> get() = if (isOnline()) firebase.archivedNotes else sqlite.archivedNotes
    override val binNotes: StateFlow<List<Note>> get() = if (isOnline()) firebase.binNotes else sqlite.binNotes
    override val reminderNotes: StateFlow<List<Note>> get() = if (isOnline()) firebase.reminderNotes else sqlite.reminderNotes
    override val labels: StateFlow<List<Label>> get() = if (isOnline()) firebase.labels else sqlite.labels
    override val notesByLabel: StateFlow<List<Note>> get() = if (isOnline()) firebase.notesByLabel else sqlite.notesByLabel
    override val labelsForNote: StateFlow<List<String>> get() = if (isOnline()) firebase.labelsForNote else sqlite.labelsForNote
    override val accountDetails: StateFlow<User?> get() = if (isOnline()) firebase.accountDetails else sqlite.accountDetails
    override val filteredNotes: StateFlow<List<Note>> get() = if (isOnline()) firebase.filteredNotes else sqlite.filteredNotes

    private fun isOnline() = ConnectivityManager(appContext, this).isNetworkAvailable()

    override fun fetchNotes() = if (isOnline()) firebase.fetchNotes() else sqlite.fetchNotes()
    override fun fetchArchivedNotes() = if (isOnline()) firebase.fetchArchivedNotes() else sqlite.fetchArchivedNotes()
    override fun fetchBinNotes() = if (isOnline()) firebase.fetchBinNotes() else sqlite.fetchBinNotes()
    override fun fetchReminderNotes() = if (isOnline()) firebase.fetchReminderNotes() else sqlite.fetchReminderNotes()
    override fun fetchLabels() = if (isOnline()) firebase.fetchLabels() else sqlite.fetchLabels()
    override fun fetchAccountDetails() = if (isOnline()) firebase.fetchAccountDetails() else sqlite.fetchAccountDetails()

    // CREATE operation: When online, update both; when offline, update only SQLite
    override fun addNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val noteWithId = note.copy(
            id = if (note.id.isBlank()) UUID.randomUUID().toString() else note.id,
            userId = firebaseAuth.getCurrentUserId() ?: note.userId
        )

        if (isOnline()) {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (firebaseUser == null) {
                onFailure(Exception("User not logged in"))
                return
            }

            firebaseUser.getIdToken(true)
                .addOnSuccessListener { result ->
                    val idToken = result.token ?: return@addOnSuccessListener onFailure(Exception("Token is null"))

                    // Call Retrofit-based Firestore note creation
                    firebase.addNoteWithRetrofit(noteWithId, idToken, {
                        // Save to SQLite after success
                        sqlite.addNote(noteWithId, onSuccess, onFailure)
                    }, onFailure)
                }
                .addOnFailureListener {
                    onFailure(it)
                }
        } else {
            sqlite.addNote(noteWithId, {
                trackOfflineOperation("ADD", "NOTE", noteWithId.id, noteWithId)
                onSuccess()
            }, onFailure)
        }
    }




    // UPDATE operation: Refactored to track offline operations
    override fun updateNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        try {
            if (isOnline()) {
                // Online: Update both Firebase and SQLite
                firebase.updateNote(note, {
                    // Once Firebase succeeds, update SQLite
                    sqlite.updateNote(note, onSuccess, onFailure)
                }, { e ->
                    Log.e("DataBridge", "Firebase update failed", e)
                    onFailure(e)
                })
            } else {
                // Offline: Update SQLite and track the change
                sqlite.updateNote(note, {
                    trackOfflineOperation("UPDATE", "NOTE", note.id, note)
                    onSuccess()
                }, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Archive operation: Refactored to track offline operations
    override fun archiveNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        try {
            if (isOnline()) {
                // Online: Update both Firebase and SQLite
                firebase.archiveNote(note, {
                    // Once Firebase succeeds, update SQLite
                    sqlite.archiveNote(note, onSuccess, onFailure)
                }, { e ->
                    Log.e("DataBridge", "Firebase archive failed", e)
                    onFailure(e)
                })
            } else {
                // Offline: Update SQLite and track the change
                sqlite.archiveNote(note, {
                    trackOfflineOperation("ARCHIVE", "NOTE", note.id, note)
                    onSuccess()
                }, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Unarchive operation: Refactored to track offline operations
    override fun unarchiveNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        try {
            if (isOnline()) {
                // Online: Update both Firebase and SQLite
                firebase.unarchiveNote(note, {
                    // Once Firebase succeeds, update SQLite
                    sqlite.unarchiveNote(note, onSuccess, onFailure)
                }, { e ->
                    Log.e("DataBridge", "Firebase unarchive failed", e)
                    onFailure(e)
                })
            } else {
                // Offline: Update SQLite and track the change
                sqlite.unarchiveNote(note, {
                    trackOfflineOperation("UNARCHIVE", "NOTE", note.id, note)
                    onSuccess()
                }, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Delete operation: Refactored to track offline operations
    override fun deleteNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        try {
            if (isOnline()) {
                // Online: Update both Firebase and SQLite
                firebase.deleteNote(note, {
                    // Once Firebase succeeds, update SQLite
                    sqlite.deleteNote(note, onSuccess, onFailure)
                }, { e ->
                    Log.e("DataBridge", "Firebase delete failed", e)
                    onFailure(e)
                })
            } else {
                // Offline: Update SQLite and track the change
                sqlite.deleteNote(note, {
                    trackOfflineOperation("DELETE", "NOTE", note.id, note)
                    onSuccess()
                }, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Permanent delete operation: Refactored to track offline operations
    override fun permanentlyDeleteNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        try {
            if (isOnline()) {
                // Online: Update both Firebase and SQLite
                firebase.permanentlyDeleteNote(note, {
                    // Once Firebase succeeds, update SQLite
                    sqlite.permanentlyDeleteNote(note, onSuccess, onFailure)
                }, { e ->
                    Log.e("DataBridge", "Firebase permanent delete failed", e)
                    onFailure(e)
                })
            } else {
                // Offline: Update SQLite and track the change
                sqlite.permanentlyDeleteNote(note, {
                    trackOfflineOperation("PERMANENT_DELETE", "NOTE", note.id, note)
                    onSuccess()
                }, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Restore operation: Refactored to track offline operations
    override fun restoreNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        try {
            if (isOnline()) {
                // Online: Update both Firebase and SQLite
                firebase.restoreNote(note, {
                    // Once Firebase succeeds, update SQLite
                    sqlite.restoreNote(note, onSuccess, onFailure)
                }, { e ->
                    Log.e("DataBridge", "Firebase restore failed", e)
                    onFailure(e)
                })
            } else {
                // Offline: Update SQLite and track the change
                sqlite.restoreNote(note, {
                    trackOfflineOperation("RESTORE", "NOTE", note.id, note)
                    onSuccess()
                }, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Set reminder operation: Refactored to track offline operations
    override fun setReminder(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        try {
            if (isOnline()) {
                // Online: Update both Firebase and SQLite
                firebase.setReminder(note, {
                    // Once Firebase succeeds, update SQLite
                    sqlite.setReminder(note, onSuccess, onFailure)
                }, { e ->
                    Log.e("DataBridge", "Firebase set reminder failed", e)
                    onFailure(e)
                })
            } else {
                // Offline: Update SQLite and track the change
                sqlite.setReminder(note, {
                    trackOfflineOperation("SET_REMINDER", "NOTE", note.id, note)
                    onSuccess()
                }, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Add label operation: Refactored to track offline operations
    override fun addNewLabel(label: Label, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = firebaseAuth.getCurrentUserId() ?: return onFailure(Exception("User not logged in"))
        Log.d("DataBridge", "Adding label with name: ${label.name}, userId: $userId")

        if (isOnline()) {
            // Online: Update both Firebase and SQLite
            firebase.addNewLabel(label, {
                Log.d("DataBridge", "Firebase success, now retrieving label details")

                // Retrieve the newly created label to get the proper ID
                firebase.getLabelByName(label.name, userId, { retrievedLabel ->
                    Log.d("DataBridge", "Retrieved label: id=${retrievedLabel.id}, userId=${retrievedLabel.userId}")

                    // Add to SQLite with the complete information
                    Log.d("DataBridge", "Adding to SQLite: id=${retrievedLabel.id}, userId=${retrievedLabel.userId}")
                    sqlite.addNewLabel(retrievedLabel, {
                        Log.d("DataBridge", "SQLite success")
                        onSuccess()
                    }, { e ->
                        Log.e("DataBridge", "SQLite failure", e)
                        onFailure(e)
                    })
                }, { e ->
                    Log.e("DataBridge", "Failed to retrieve label", e)
                    onFailure(e)
                })
            }, { e ->
                Log.e("DataBridge", "Firebase failure", e)
                onFailure(e)
            })
        } else {
            // Offline: Update SQLite and track the change
            val offlineLabel = Label(
                id = UUID.randomUUID().toString(),
                name = label.name,
                userId = userId
            )

            Log.d("DataBridge", "Offline mode: Adding to SQLite only: id=${offlineLabel.id}, userId=${offlineLabel.userId}")
            sqlite.addNewLabel(offlineLabel, {
                Log.d("DataBridge", "SQLite success (offline mode)")
                trackOfflineOperation("ADD", "LABEL", offlineLabel.id, offlineLabel)
                onSuccess()
            }, { e ->
                Log.e("DataBridge", "SQLite failure (offline mode)", e)
                onFailure(e)
            })
        }
    }

    // Update label operation: Refactored to track offline operations
    override fun updateLabel(oldLabel: Label, newLabel: Label, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        try {
            if (isOnline()) {
                // Online: Update both Firebase and SQLite
                firebase.updateLabel(oldLabel, newLabel, {
                    // Once Firebase succeeds, update SQLite
                    sqlite.updateLabel(oldLabel, newLabel, onSuccess, onFailure)
                }, { e ->
                    Log.e("DataBridge", "Firebase update label failed", e)
                    onFailure(e)
                })
            } else {
                // Offline: Update SQLite and track the change
                sqlite.updateLabel(oldLabel, newLabel, {
                    trackOfflineOperation("UPDATE", "LABEL", oldLabel.id, newLabel)
                    onSuccess()
                }, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Delete label operation: Refactored to track offline operations
    override fun deleteLabel(label: Label, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        try {
            if (isOnline()) {
                // Online: Update both Firebase and SQLite
                firebase.deleteLabel(label, {
                    // Once Firebase succeeds, update SQLite
                    sqlite.deleteLabel(label, onSuccess, onFailure)
                }, { e ->
                    Log.e("DataBridge", "Firebase delete label failed", e)
                    onFailure(e)
                })
            } else {
                // Offline: Update SQLite and track the change
                sqlite.deleteLabel(label, {
                    trackOfflineOperation("DELETE", "LABEL", label.id, label)
                    onSuccess()
                }, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Toggle label operation: Refactored to track offline operations
    override fun toggleLabelForNotes(
        label: Label,
        isChecked: Boolean,
        noteIds: List<String>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            if (isOnline()) {
                // Online: Update both Firebase and SQLite
                firebase.toggleLabelForNotes(label, isChecked, noteIds, {
                    // Once Firebase succeeds, update SQLite
                    sqlite.toggleLabelForNotes(label, isChecked, noteIds, onSuccess, onFailure)
                }, { e ->
                    Log.e("DataBridge", "Firebase toggle label failed", e)
                    onFailure(e)
                })
            } else {
                // Offline: Update SQLite and track the change
                sqlite.toggleLabelForNotes(label, isChecked, noteIds, {
                    // Create a data object to store the toggle operation details
                    val toggleData = mapOf(
                        "labelId" to label.id,
                        "isChecked" to isChecked,
                        "noteIds" to noteIds
                    )
                    trackOfflineOperation("TOGGLE_LABEL", "LABEL_NOTE", label.id, toggleData)
                    onSuccess()
                }, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Auth operations require online connectivity
    fun register(user: User, password: String, onComplete: (Boolean, String?) -> Unit) {
        if (!isOnline()) return onComplete(false, "No internet connection")

        firebaseAuth.registerWithEmail(user, password) { success, message ->
            onComplete(success, message)
        }
    }

    fun login(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        if (!isOnline()) return onComplete(false, "No internet connection")

        firebaseAuth.loginWithEmail(email, password) { success, message ->
            onComplete(success, message)
        }
    }

    fun loginWithGoogleCredential(credential: AuthCredential, userInfo: User?, onComplete: (Boolean, String?) -> Unit) {
        if (!isOnline()) return onComplete(false, "No internet connection")

        firebaseAuth.loginWithGoogleCredential(credential, userInfo, onComplete)
    }



}