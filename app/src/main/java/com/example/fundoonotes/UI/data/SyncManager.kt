package com.example.fundoonotes.UI.data

import android.util.Log
import com.example.fundoonotes.UI.data.entity.OfflineOperation
import com.example.fundoonotes.UI.data.model.Label
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.data.model.User
import com.example.fundoonotes.UI.data.repository.FirebaseAuthRepository
import com.example.fundoonotes.UI.data.repository.FirebaseNotesRepository
import com.example.fundoonotes.UI.data.repository.SQLiteNotesRepository
import com.google.gson.Gson
import kotlinx.coroutines.withContext

class SyncManager(
    private val sqlite: SQLiteNotesRepository,
    private val firebase: FirebaseNotesRepository,
    private val firebaseAuth : FirebaseAuthRepository
) {
    private val gson = Gson()

    fun syncOfflineChanges() {
        Log.d("OfflineSyncManager", "Starting synchronization of offline operations")
        sqlite.getOfflineOperations { operations ->
            if (operations.isEmpty()) {
                Log.d("OfflineSyncManager", "No offline operations to sync")
                return@getOfflineOperations
            }
            Log.d("OfflineSyncManager", "Syncing ${operations.size} offline operations")
            processOfflineOperations(operations, 0)
        }
    }

    private fun processOfflineOperations(operations: List<OfflineOperation>, index: Int) {
        if (index >= operations.size) {
            Log.d("OfflineSyncManager", "All offline operations processed")
            return
        }

        val operation = operations[index]
        when (operation.entityType) {
            "NOTE" -> {
                // Convert payload to Note
                val note = gson.fromJson(operation.payload, Note::class.java)
                when (operation.operationType) {
                    "ADD" -> {
                        firebase.addNote(note, {
                            Log.d("OfflineSyncManager", "Synced ADD for note ${note.id}")
                            removeOpAndContinue(operation.id, operations, index)
                        }, { e ->
                            Log.e("OfflineSyncManager", "Sync ADD failed for note ${note.id}", e)
                            // Remove the operation to allow processing subsequent items
                            removeOpAndContinue(operation.id, operations, index)
                        })
                    }
                    "UPDATE" -> {
                        firebase.updateNote(note, {
                            Log.d("OfflineSyncManager", "Synced UPDATE for note ${note.id}")
                            removeOpAndContinue(operation.id, operations, index)
                        }, { e ->
                            Log.e("OfflineSyncManager", "Sync UPDATE failed for note ${note.id}", e)
                            removeOpAndContinue(operation.id, operations, index)
                        })
                    }
                    "DELETE" -> {
                        firebase.deleteNote(note, {
                            Log.d("OfflineSyncManager", "Synced DELETE for note ${note.id}")
                            removeOpAndContinue(operation.id, operations, index)
                        }, { e ->
                            Log.e("OfflineSyncManager", "Sync DELETE failed for note ${note.id}", e)
                            removeOpAndContinue(operation.id, operations, index)
                        })
                    }
                    "ARCHIVE" -> {
                        firebase.archiveNote(note, {
                            Log.d("OfflineSyncManager", "Synced ARCHIVE for note ${note.id}")
                            removeOpAndContinue(operation.id, operations, index)
                        }, { e ->
                            Log.e("OfflineSyncManager", "Sync ARCHIVE failed for note ${note.id}", e)
                            removeOpAndContinue(operation.id, operations, index)
                        })
                    }
                    "UNARCHIVE" -> {
                        firebase.unarchiveNote(note, {
                            Log.d("OfflineSyncManager", "Synced UNARCHIVE for note ${note.id}")
                            removeOpAndContinue(operation.id, operations, index)
                        }, { e ->
                            Log.e("OfflineSyncManager", "Sync UNARCHIVE failed for note ${note.id}", e)
                            removeOpAndContinue(operation.id, operations, index)
                        })
                    }
                    "RESTORE" -> {
                        firebase.restoreNote(note, {
                            Log.d("OfflineSyncManager", "Synced RESTORE for note ${note.id}")
                            removeOpAndContinue(operation.id, operations, index)
                        }, { e ->
                            Log.e("OfflineSyncManager", "Sync RESTORE failed for note ${note.id}", e)
                            removeOpAndContinue(operation.id, operations, index)
                        })
                    }
                    "SET_REMINDER" -> {
                        firebase.setReminder(note, {
                            Log.d("OfflineSyncManager", "Synced SET_REMINDER for note ${note.id}")
                            removeOpAndContinue(operation.id, operations, index)
                        }, { e ->
                            Log.e("OfflineSyncManager", "Sync SET_REMINDER failed for note ${note.id}", e)
                            removeOpAndContinue(operation.id, operations, index)
                        })
                    }
                    else -> {
                        Log.d("OfflineSyncManager", "Unknown NOTE operation: ${operation.operationType}. Removing.")
                        removeOpAndContinue(operation.id, operations, index)
                    }
                }
            }
            "LABEL" -> {
                // Convert payload to Label
                val label = gson.fromJson(operation.payload, Label::class.java)
                when (operation.operationType) {
                    "ADD" -> {
                        firebase.addNewLabel(label, {
                            Log.d("OfflineSyncManager", "Synced ADD for label ${label.id}")
                            removeOpAndContinue(operation.id, operations, index)
                        }, { e ->
                            Log.e("OfflineSyncManager", "Sync ADD failed for label ${label.id}", e)
                            removeOpAndContinue(operation.id, operations, index)
                        })
                    }
                    "UPDATE" -> {
                        // For a label update, you may need to pass additional data.
                        firebase.updateLabel(label, label, {
                            Log.d("OfflineSyncManager", "Synced UPDATE for label ${label.id}")
                            removeOpAndContinue(operation.id, operations, index)
                        }, { e ->
                            Log.e("OfflineSyncManager", "Sync UPDATE failed for label ${label.id}", e)
                            removeOpAndContinue(operation.id, operations, index)
                        })
                    }
                    "DELETE" -> {
                        firebase.deleteLabel(label, {
                            Log.d("OfflineSyncManager", "Synced DELETE for label ${label.id}")
                            removeOpAndContinue(operation.id, operations, index)
                        }, { e ->
                            Log.e("OfflineSyncManager", "Sync DELETE failed for label ${label.id}", e)
                            removeOpAndContinue(operation.id, operations, index)
                        })
                    }
                    else -> {
                        Log.d("OfflineSyncManager", "Unknown LABEL operation: ${operation.operationType}. Removing.")
                        removeOpAndContinue(operation.id, operations, index)
                    }
                }
            }
            else -> {
                Log.d("OfflineSyncManager", "Unknown entity type: ${operation.entityType}. Removing.")
                removeOpAndContinue(operation.id, operations, index)
            }
        }
    }

    private fun removeOpAndContinue(opId: Int, operations: List<OfflineOperation>, index: Int) {
        sqlite.removeOfflineOperation(opId) {
            processOfflineOperations(operations, index + 1)
        }
    }

    /**
     * Syncs data from Firestore to SQLite - to be used when connectivity is restored
     * or when user manually refreshes
     */
    fun syncOnlineChanges(
        user: User,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        try {
            val notesResult = runCatching {
                firebase.fetchNotesOnce(user.userId,
                    onResult = { notes ->
                        sqlite.replaceAllNotes(notes) {
                            Log.d("ReverseSync", "Notes synced")
                        }
                    },
                    onError = { throw it }
                )
            }

            if (notesResult.isFailure) {
                onFailure((notesResult.exceptionOrNull() ?: Exception("Notes fetch failed")) as Exception)
                return
            }

            val labelsResult = runCatching {
                firebase.fetchLabelsOnce(user.userId,
                    onResult = { labels ->
                        sqlite.replaceAllLabels(labels) {
                            Log.d("ReverseSync", "Labels synced")
                            onSuccess()
                        }
                    },
                    onError = { throw it }
                )
            }

            if (labelsResult.isFailure) {
                onFailure((labelsResult.exceptionOrNull() ?: Exception("Labels fetch failed")) as Exception)
            }

        } catch (e: Exception) {
            Log.e("ReverseSync", "Sync failed", e)
            onFailure(e)
        }
    }
}
