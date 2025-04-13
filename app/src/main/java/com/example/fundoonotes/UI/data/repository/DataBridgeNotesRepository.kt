package com.example.fundoonotes.UI.data.repository

import android.content.Context
import android.util.Log
import com.example.fundoonotes.UI.data.NotesDatabase
import com.example.fundoonotes.UI.util.NetworkUtils
import com.example.fundoonotes.UI.data.model.*
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class DataBridgeNotesRepository(
    context: Context
) : NotesRepository {

    private val firebase = FirebaseNotesRepository()
    private val firebaseAuth = FirebaseAuthRepository(FirebaseAuth.getInstance())

    private val sqlite = SQLiteNotesRepository(
        noteDao = NotesDatabase.getInstance(context).noteDao(),
        labelDao = NotesDatabase.getInstance(context).labelDao(),
        userDao = NotesDatabase.getInstance(context).userDao(),
        scope = CoroutineScope(Dispatchers.IO)
    )

    private val sqliteAuth = SQLiteAuthRepository(
        NotesDatabase.getInstance(context).userDao(),
        CoroutineScope(Dispatchers.IO)
    )

    private val appContext = context.applicationContext

    val currentUser = if (isOnline()) firebase.accountDetails else sqliteAuth.currentUser

    // READ operations: Always prefer Firestore when online
    override val notes: StateFlow<List<Note>> get() = if (isOnline()) firebase.notes else sqlite.notes
    override val archivedNotes: StateFlow<List<Note>> get() = if (isOnline()) firebase.archivedNotes else sqlite.archivedNotes
    override val binNotes: StateFlow<List<Note>> get() = if (isOnline()) firebase.binNotes else sqlite.binNotes
    override val reminderNotes: StateFlow<List<Note>> get() = if (isOnline()) firebase.reminderNotes else sqlite.reminderNotes
    override val labels: StateFlow<List<Label>> get() = if (isOnline()) firebase.labels else sqlite.labels
    override val notesByLabel: StateFlow<List<Note>> get() = if (isOnline()) firebase.notesByLabel else sqlite.notesByLabel
    override val labelsForNote: StateFlow<List<String>> get() = if (isOnline()) firebase.labelsForNote else sqlite.labelsForNote
    override val accountDetails: StateFlow<User?> get() = if (isOnline()) firebase.accountDetails else sqlite.accountDetails
    override val filteredNotes: StateFlow<List<Note>> get() = if (isOnline()) firebase.filteredNotes else sqlite.filteredNotes

    private fun isOnline() = NetworkUtils.isNetworkAvailable(appContext)

    // Fetch methods: Always use Firestore when online
    override fun fetchNotes() = if (isOnline()) firebase.fetchNotes() else sqlite.fetchNotes()
    override fun fetchArchivedNotes() = if (isOnline()) firebase.fetchArchivedNotes() else sqlite.fetchArchivedNotes()
    override fun fetchBinNotes() = if (isOnline()) firebase.fetchBinNotes() else sqlite.fetchBinNotes()
    override fun fetchReminderNotes() = if (isOnline()) firebase.fetchReminderNotes() else sqlite.fetchReminderNotes()
    override fun fetchLabels() = if (isOnline()) firebase.fetchLabels() else sqlite.fetchLabels()
    override fun fetchAccountDetails() = if (isOnline()) firebase.fetchAccountDetails() else sqlite.fetchAccountDetails()

    // CREATE operation: When online, update both; when offline, update only SQLite
    override fun addNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        try {
            val noteWithId = note.copy(
                id = if (note.id.isBlank()) UUID.randomUUID().toString() else note.id,
                userId = firebaseAuth.getCurrentUserId() ?: note.userId
            )

            if (isOnline()) {
                // Online: Update both Firebase and SQLite
                firebase.addNote(noteWithId, {
                    // Once Firebase succeeds, update SQLite
                    sqlite.addNote(noteWithId, onSuccess, onFailure)
                }, onFailure)
            } else {
                // Offline: Update only SQLite
                sqlite.addNote(noteWithId, onSuccess, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // UPDATE operation: When online, update both; when offline, update only SQLite
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
                // Offline: Update only SQLite
                sqlite.updateNote(note, onSuccess, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Archive operation: When online, update both; when offline, update only SQLite
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
                // Offline: Update only SQLite
                sqlite.archiveNote(note, onSuccess, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Unarchive operation: When online, update both; when offline, update only SQLite
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
                // Offline: Update only SQLite
                sqlite.unarchiveNote(note, onSuccess, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Delete operation: When online, update both; when offline, update only SQLite
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
                // Offline: Update only SQLite
                sqlite.deleteNote(note, onSuccess, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Permanent delete operation: When online, update both; when offline, update only SQLite
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
                // Offline: Update only SQLite
                sqlite.permanentlyDeleteNote(note, onSuccess, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Restore operation: When online, update both; when offline, update only SQLite
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
                // Offline: Update only SQLite
                sqlite.restoreNote(note, onSuccess, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Set reminder operation: When online, update both; when offline, update only SQLite
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
                // Offline: Update only SQLite
                sqlite.setReminder(note, onSuccess, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Add label operation: When online, update both; when offline, update only SQLite
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
            // Offline: Update only SQLite
            // Generate a temporary ID for offline mode
            val offlineLabel = Label(
                id = UUID.randomUUID().toString(),
                name = label.name,
                userId = userId
            )

            Log.d("DataBridge", "Offline mode: Adding to SQLite only: id=${offlineLabel.id}, userId=${offlineLabel.userId}")
            sqlite.addNewLabel(offlineLabel, {
                Log.d("DataBridge", "SQLite success (offline mode)")
                onSuccess()
            }, { e ->
                Log.e("DataBridge", "SQLite failure (offline mode)", e)
                onFailure(e)
            })
        }
    }

    // Update label operation: When online, update both; when offline, update only SQLite
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
                // Offline: Update only SQLite
                sqlite.updateLabel(oldLabel, newLabel, onSuccess, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Delete label operation: When online, update both; when offline, update only SQLite
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
                // Offline: Update only SQLite
                sqlite.deleteLabel(label, onSuccess, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Toggle label operation: When online, update both; when offline, update only SQLite
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
                // Offline: Update only SQLite
                sqlite.toggleLabelForNotes(label, isChecked, noteIds, onSuccess, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Auth operations require online connectivity
    fun register(user: User, password: String, onComplete: (Boolean, String?) -> Unit) {
        if (!isOnline()) return onComplete(false, "No internet connection")

        firebaseAuth.registerWithEmail(user, password) { success, message ->
            if (success) {
                val updatedUser = user.copy(userId = firebaseAuth.getCurrentUserId() ?: "")
                sqliteAuth.registerUserLocally(updatedUser) { _, _ -> }
            }
            onComplete(success, message)
        }
    }

    fun login(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        if (!isOnline()) return onComplete(false, "No internet connection")

        firebaseAuth.loginWithEmail(email, password) { success, message ->
            if (success) {
                sqliteAuth.loginUserLocally(email) { _, _ -> }
            }
            onComplete(success, message)
        }
    }

    fun loginWithGoogleCredential(credential: AuthCredential, userInfo: User?, onComplete: (Boolean, String?) -> Unit) {
        if (!isOnline()) return onComplete(false, "No internet connection")

        firebaseAuth.loginWithGoogleCredential(credential, userInfo, onComplete)
    }
}