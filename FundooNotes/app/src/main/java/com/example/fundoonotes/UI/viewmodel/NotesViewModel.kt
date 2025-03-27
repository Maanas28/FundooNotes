package com.example.fundoonotes.UI.viewmodel

import androidx.lifecycle.ViewModel
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.data.repository.NotesRepository
import kotlinx.coroutines.flow.StateFlow

class NotesViewModel : ViewModel() {

    private val repository = NotesRepository()

    val notesFlow: StateFlow<List<Note>> = repository.notes
    val archivedNotesFlow: StateFlow<List<Note>> = repository.archivedNotes

    fun saveNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        repository.addNote(note, onSuccess, onFailure)
    }

    fun archiveNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        repository.archiveNote(note, onSuccess, onFailure)
    }

    fun unarchiveNote(id: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        repository.unarchiveNote(id, onSuccess, onFailure)
    }

    fun fetchNotes() {
        repository.fetchNotes()
    }

    fun fetchArchivedNotes() {
        repository.fetchArchivedNotes()
    }
}



