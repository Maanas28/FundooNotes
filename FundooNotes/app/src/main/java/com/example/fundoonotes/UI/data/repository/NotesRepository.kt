package com.example.fundoonotes.UI.data.repository

import android.util.Log
import com.example.fundoonotes.UI.data.model.Note
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NotesRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    private val _archivedNotes = MutableStateFlow<List<Note>>(emptyList())
    val archivedNotes: StateFlow<List<Note>> = _archivedNotes

    fun fetchNotes() {
        firestore.collection("notes")
            .whereEqualTo("archived", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _notes.value = emptyList()
                    return@addSnapshotListener
                }
                _notes.value = snapshot.documents.mapNotNull {
                    it.toObject(Note::class.java)?.copy(id = it.id)
                }
            }
    }

    fun fetchArchivedNotes() {
        firestore.collection("notes")
            .whereEqualTo("archived", true)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _archivedNotes.value = emptyList()
                    return@addSnapshotListener
                }
                _archivedNotes.value = snapshot.documents.mapNotNull {
                    it.toObject(Note::class.java)?.copy(id = it.id)
                }
            }
    }


    fun addNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val noteToAdd = note.copy(archived = false)

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
            .addOnFailureListener {
                Log.e("FirestoreSave", "Failed to save", it)
                onFailure(it)
            }
    }


    fun archiveNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {

        firestore.collection("notes")
            .document(note.id)
            .update("archived", true)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onFailure(it)
            }
    }



    fun unarchiveNote(noteId: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        firestore.collection("notes").document(noteId)
            .update("archived", false)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }


}




