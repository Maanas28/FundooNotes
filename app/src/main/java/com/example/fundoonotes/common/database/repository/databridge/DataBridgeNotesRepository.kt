package com.example.fundoonotes.common.database.repository.databridge

import android.content.Context
import com.example.fundoonotes.R
import com.example.fundoonotes.common.data.model.Note
import com.example.fundoonotes.common.database.repository.interfaces.NotesRepository
import com.example.fundoonotes.common.util.managers.NetworkUtils.isOnline
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class DataBridgeNotesRepository(
    context: Context
) : DataBridge<Note>(context), NotesRepository {


    override val notes: StateFlow<List<Note>> get() = if (isOnline(context)) firebase.notes else sqlite.notes
    override val archivedNotes: StateFlow<List<Note>> get() = if (isOnline(context)) firebase.archivedNotes else sqlite.archivedNotes
    override val binNotes: StateFlow<List<Note>> get() = if (isOnline(context)) firebase.binNotes else sqlite.binNotes
    override val reminderNotes: StateFlow<List<Note>> get() = if (isOnline(context)) firebase.reminderNotes else sqlite.reminderNotes
    override val notesByLabel: StateFlow<List<Note>> get() = if (isOnline(context)) firebase.notesByLabel else sqlite.notesByLabel
    override val labelsForNote: StateFlow<List<String>> get() = if (isOnline(context)) firebase.labelsForNote else sqlite.labelsForNote
    override val filteredNotes: StateFlow<List<Note>> get() = if (isOnline(context)) firebase.filteredNotes else sqlite.filteredNotes


    override fun fetchNotes() = if (isOnline(context)) firebase.fetchNotes() else sqlite.fetchNotes()
    override fun fetchArchivedNotes() = if (isOnline(context)) firebase.fetchArchivedNotes() else sqlite.fetchArchivedNotes()
    override fun fetchBinNotes() = if (isOnline(context)) firebase.fetchBinNotes() else sqlite.fetchBinNotes()
    override fun fetchReminderNotes() = if (isOnline(context)) firebase.fetchReminderNotes() else sqlite.fetchReminderNotes()

    // CREATE operation: When online, update both; when offline, update only SQLite
    override fun addNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val noteWithId = note.copy(
            id = if (note.id.isBlank()) UUID.randomUUID().toString() else note.id,
            userId = firebaseAuth.getCurrentUserId() ?: note.userId
        )

        if (isOnline(context)) {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (firebaseUser == null) {
                onFailure(Exception(context.getString(R.string.user_not_found)))
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
            if (isOnline(context)) {
                // Online: Update both Firebase and SQLite
                firebase.updateNote(note, {
                    // Once Firebase succeeds, update SQLite
                    sqlite.updateNote(note, onSuccess, onFailure)
                }, { e ->
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
            if (isOnline(context)) {
                // Online: Update both Firebase and SQLite
                firebase.archiveNote(note, {
                    // Once Firebase succeeds, update SQLite
                    sqlite.archiveNote(note, onSuccess, onFailure)
                }, { e ->
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
            if (isOnline(context)) {
                // Online: Update both Firebase and SQLite
                firebase.unarchiveNote(note, {
                    // Once Firebase succeeds, update SQLite
                    sqlite.unarchiveNote(note, onSuccess, onFailure)
                }, { e ->
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
            if (isOnline(context)) {
                // Online: Update both Firebase and SQLite
                firebase.deleteNote(note, {
                    // Once Firebase succeeds, update SQLite
                    sqlite.deleteNote(note, onSuccess, onFailure)
                }, { e ->
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
            if (isOnline(context)) {
                // Online: Update both Firebase and SQLite
                firebase.permanentlyDeleteNote(note, {
                    // Once Firebase succeeds, update SQLite
                    sqlite.permanentlyDeleteNote(note, onSuccess, onFailure)
                }, { e ->
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
            if (isOnline(context)) {
                // Online: Update both Firebase and SQLite
                firebase.restoreNote(note, {
                    // Once Firebase succeeds, update SQLite
                    sqlite.restoreNote(note, onSuccess, onFailure)
                }, { e ->
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
            if (isOnline(context)) {
                // Online: Update both Firebase and SQLite
                firebase.setReminder(note, {
                    // Once Firebase succeeds, update SQLite
                    sqlite.setReminder(note, onSuccess, onFailure)
                }, { e ->
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
}