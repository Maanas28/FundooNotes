package com.example.fundoonotes.common.util.managers

import com.example.fundoonotes.common.data.entity.OfflineOperation
import com.example.fundoonotes.common.data.model.Label
import com.example.fundoonotes.common.data.model.Note
import com.example.fundoonotes.common.database.repository.firebase.FirebaseLabelsRepository
import com.example.fundoonotes.common.database.repository.firebase.FirebaseNotesRepository
import com.example.fundoonotes.common.database.repository.sqlite.SQLiteLabelsRepository
import com.example.fundoonotes.common.database.repository.sqlite.SQLiteNotesRepository
import com.example.fundoonotes.common.database.repository.sqlite.SQLiteOfflineOperationsRepository
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred

// Define a type alias for the sync action function signature
private typealias SyncAction = (OfflineOperation) -> Unit

class SyncManager(
    private val sqliteLabels: SQLiteLabelsRepository,
    private val sqliteOperation: SQLiteOfflineOperationsRepository,
    private val sqliteNote: SQLiteNotesRepository,
    private val firebase: FirebaseNotesRepository,
    private val firebaseLabel: FirebaseLabelsRepository
) {
    private val gson = Gson()

    // Start syncing all offline operations
    fun syncOfflineChanges() {
        sqliteOperation.getOfflineOperations { operations ->
            if (operations.isEmpty()) {
                return@getOfflineOperations
            }
            operations.forEach { processOfflineOperation(it) }
        }
    }

    // Dispatch maps for note and label sync actions
    private val noteActions = mapOf<String, SyncAction>(
        "ADD" to ::syncAddNote,
        "UPDATE" to ::syncUpdateNote,
        "DELETE" to ::syncDeleteNote,
        "ARCHIVE" to ::syncArchiveNote,
        "UNARCHIVE" to ::syncUnarchiveNote,
        "RESTORE" to ::syncRestoreNote,
        "SET_REMINDER" to ::syncSetReminderNote
    )

    private val labelActions = mapOf<String, SyncAction>(
        "ADD" to ::syncAddLabel,
        "UPDATE" to ::syncUpdateLabel,
        "DELETE" to ::syncDeleteLabel
    )

    // Process each operation based on its entityType and operationType
    private fun processOfflineOperation(op: OfflineOperation) {
        val actionMap = when (op.entityType) {
            "NOTE" -> noteActions
            "LABEL" -> labelActions
            else -> null
        }

        val action = actionMap?.get(op.operationType)
        if (action != null) {
            action(op)
        } else {
            removeOp(op)
        }
    }

    // -------- Note Handlers --------
    private fun syncAddNote(op: OfflineOperation) = handleNote(op) {
        firebase.addNote(it, onSuccess(op), onFailure(op))
    }

    private fun syncUpdateNote(op: OfflineOperation) = handleNote(op) {
        firebase.updateNote(it, onSuccess(op), onFailure(op))
    }

    private fun syncDeleteNote(op: OfflineOperation) = handleNote(op) {
        firebase.deleteNote(it, onSuccess(op), onFailure(op))
    }

    private fun syncArchiveNote(op: OfflineOperation) = handleNote(op) {
        firebase.archiveNote(it, onSuccess(op), onFailure(op))
    }

    private fun syncUnarchiveNote(op: OfflineOperation) = handleNote(op) {
        firebase.unarchiveNote(it, onSuccess(op), onFailure(op))
    }

    private fun syncRestoreNote(op: OfflineOperation) = handleNote(op) {
        firebase.restoreNote(it, onSuccess(op), onFailure(op))
    }

    private fun syncSetReminderNote(op: OfflineOperation) = handleNote(op) {
        firebase.setReminder(it, onSuccess(op), onFailure(op))
    }

    private fun handleNote(op: OfflineOperation, block: (Note) -> Unit) {
        try {
            val note = gson.fromJson(op.payload, Note::class.java)
            block(note)
        } catch (e: Exception) {
            removeOp(op)
        }
    }

    // -------- Label Handlers --------
    private fun syncAddLabel(op: OfflineOperation) = handleLabel(op) {
        firebaseLabel.addNewLabel(it, onSuccess(op), onFailure(op))
    }

    private fun syncUpdateLabel(op: OfflineOperation) = handleLabel(op) {
        firebaseLabel.updateLabel(it, it, onSuccess(op), onFailure(op))
    }

    private fun syncDeleteLabel(op: OfflineOperation) = handleLabel(op) {
        firebaseLabel.deleteLabel(it, onSuccess(op), onFailure(op))
    }

    private fun handleLabel(op: OfflineOperation, block: (Label) -> Unit) {
        try {
            val label = gson.fromJson(op.payload, Label::class.java)
            block(label)
        } catch (e: Exception) {
            removeOp(op)
        }
    }

    // -------- Utility Handlers --------
    private fun onSuccess(op: OfflineOperation): () -> Unit = {
        removeOp(op)
    }

    private fun onFailure(op: OfflineOperation): (Exception) -> Unit = { e ->
        removeOp(op)
    }

    private fun removeOp(op: OfflineOperation) {
        sqliteOperation.removeOfflineOperation(op.id) {
        }
    }

    /**
     * Syncs data from Firestore to SQLite - to be used when connectivity is restored
     * or when user manually refreshes.
     */
    suspend fun syncOnlineChanges(userId: String) {
        val notes = firebase.getAllNotesForUser(userId)
        val labels = firebaseLabel.getAllLabelsForUser(userId)

        val completion = CompletableDeferred<Unit>()
        var notesDone = false
        var labelsDone = false

        sqliteNote.replaceAllNotes(notes) {
            notesDone = true
            if (labelsDone) completion.complete(Unit)
        }

        sqliteLabels.replaceAllLabels(labels) {
            labelsDone = true
            if (notesDone) completion.complete(Unit)
        }

        completion.await()
    }
}
