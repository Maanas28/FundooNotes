package com.example.fundoonotes.UI.features.notes.viewmodel

import androidx.lifecycle.ViewModel
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.data.repository.NotesRepository
import com.example.fundoonotes.UI.data.repository.FirebaseNotesRepository
import com.example.fundoonotes.UI.util.NotesGridContext
import kotlinx.coroutines.flow.*

class NotesViewModel (
    private val repository: NotesRepository = FirebaseNotesRepository()
) : ViewModel() {

    val notesFlow: StateFlow<List<Note>> = repository.notes
    val archivedNotesFlow: StateFlow<List<Note>> = repository.archivedNotes
    val binNotes: StateFlow<List<Note>> = repository.binNotes
    val reminderNotes: StateFlow<List<Note>> = repository.reminderNotes

    fun fetchForContext(context: NotesGridContext) {
        when (context) {
            is NotesGridContext.Notes -> fetchNotes()
            is NotesGridContext.Archive -> fetchArchivedNotes()
            is NotesGridContext.Bin -> fetchBinNotes()
            is NotesGridContext.Reminder -> fetchReminderNotes()
            is NotesGridContext.Label -> {} // future use
        }
    }

    fun getFlowForContext(context: NotesGridContext): StateFlow<List<Note>> {
        return when (context) {
            is NotesGridContext.Notes -> notesFlow
            is NotesGridContext.Archive -> archivedNotesFlow
            is NotesGridContext.Bin -> binNotes
            is NotesGridContext.Reminder -> reminderNotes
            is NotesGridContext.Label -> MutableStateFlow(emptyList()) // placeholder
        }
    }

    fun filterNotesForContext(notes: List<Note>, context: NotesGridContext): List<Note> {
        return when (context) {
            is NotesGridContext.Notes -> notes.filter { !it.archived && !it.inBin && !it.deleted }
            is NotesGridContext.Archive -> notes.filter { it.archived && !it.inBin && !it.deleted }
            is NotesGridContext.Bin -> notes.filter { it.inBin && !it.deleted }
            is NotesGridContext.Reminder -> notes.filter { it.hasReminder && !it.inBin }
            is NotesGridContext.Label -> notes // implement label logic later
        }
    }

    fun fetchNotes() = repository.fetchNotes()
    fun fetchArchivedNotes() = repository.fetchArchivedNotes()
    fun fetchBinNotes() = repository.fetchBinNotes()
    fun fetchReminderNotes() = repository.fetchReminderNotes()

    fun saveNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) =
        repository.addNote(note, onSuccess, onFailure)

    fun archiveNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) =
        repository.archiveNote(note)

    fun unarchiveNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) =
        repository.unarchiveNote(note, onSuccess, onFailure)

    fun deleteNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) =
        repository.deleteNote(note, onSuccess, onFailure)

    fun permanentlyDeleteNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) =
        repository.permanentlyDeleteNote(note, onSuccess, onFailure)

    fun restoreNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) =
        repository.restoreNote(note, onSuccess, onFailure)

    fun setReminder(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) =
        repository.setReminder(note, onSuccess, onFailure)
}
