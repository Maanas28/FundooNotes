package com.example.fundoonotes.UI.data.repository

import android.util.Log
import com.example.fundoonotes.UI.data.model.Note
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FirebaseNotesRepository : NotesRepository {

    private val firestore = FirebaseFirestore.getInstance()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    override val notes: StateFlow<List<Note>> = _notes

    private val _archivedNotes = MutableStateFlow<List<Note>>(emptyList())
    override val archivedNotes: StateFlow<List<Note>> = _archivedNotes

    private val _deletedNotes = MutableStateFlow<List<Note>>(emptyList())
    override val binNotes: StateFlow<List<Note>> = _deletedNotes

    private val _reminderNotes = MutableStateFlow<List<Note>>(emptyList())
    override val reminderNotes: StateFlow<List<Note>> = _reminderNotes

    override fun fetchNotes() {
        firestore.collection("notes")
            .whereEqualTo("archived", false)
            .whereEqualTo("deleted", false)
            .whereEqualTo("inBin", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _notes.value = emptyList()
                    return@addSnapshotListener
                }
                _notes.value = snapshot.documents.mapNotNull {
                    it.toObject(Note::class.java)?.copy(id = it.id)
                }.sortedByDescending { it.timestamp }
            }
    }

    override fun fetchArchivedNotes() {
        firestore.collection("notes")
            .whereEqualTo("archived", true)
            .whereEqualTo("deleted", false)
            .whereEqualTo("inBin", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _archivedNotes.value = emptyList()
                    return@addSnapshotListener
                }
                _archivedNotes.value = snapshot.documents.mapNotNull {
                    it.toObject(Note::class.java)?.copy(id = it.id)
                }.sortedByDescending { it.timestamp }
            }
    }

    override fun fetchBinNotes() {
        firestore.collection("notes")
            .whereEqualTo("deleted", false)
            .whereEqualTo("inBin", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _deletedNotes.value = emptyList()
                    return@addSnapshotListener
                }
                _deletedNotes.value = snapshot.documents.mapNotNull {
                    it.toObject(Note::class.java)?.copy(id = it.id)
                }.sortedByDescending { it.timestamp }
            }
    }

    override fun fetchReminderNotes() {
        firestore.collection("notes")
            .whereEqualTo("hasReminder", true)
            .whereEqualTo("inBin", false)
            .whereEqualTo("deleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _reminderNotes.value = emptyList()
                    return@addSnapshotListener
                }
                _reminderNotes.value = snapshot.documents.mapNotNull {
                    it.toObject(Note::class.java)?.copy(id = it.id)
                }.sortedByDescending { it.timestamp }
            }
    }

    override fun addNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val noteToAdd = note.copy(archived = false, deleted = false, inBin = false)
        firestore.collection("notes")
            .add(noteToAdd)
            .addOnSuccessListener { documentRef ->
                val id = documentRef.id
                documentRef.update("id", id)
                    .addOnSuccessListener {
                        Log.d("FirestoreSave", "Saved successfully with ID $id")
                        onSuccess()
                    }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    override fun archiveNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("notes")
            .document(note.id)
            .update("archived", true)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    override fun unarchiveNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("notes")
            .document(note.id)
            .update("archived", false)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    override fun deleteNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("notes")
            .document(note.id)
            .update("inBin", true)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    override fun permanentlyDeleteNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("notes")
            .document(note.id)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    override fun restoreNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("notes")
            .document(note.id)
            .update("inBin", false)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    override fun setReminder(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        note.reminderTime?.let { time ->
            firestore.collection("notes")
                .document(note.id)
                .update(
                    mapOf(
                        "hasReminder" to true,
                        "reminderTime" to time,
                    )
                )
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure(it) }
        } ?: onFailure(Exception("Reminder time is null"))
    }
}
