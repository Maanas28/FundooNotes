package com.example.fundoonotes.UI.features.notes.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.data.repository.NotesRepository
import com.example.fundoonotes.UI.data.repository.FirebaseNotesRepository
import com.example.fundoonotes.UI.util.NotesGridContext
import com.example.fundoonotes.UI.util.ReminderScheduler
import kotlinx.coroutines.flow.*

class NotesViewModel (
    private val repository: NotesRepository = FirebaseNotesRepository()
) : ViewModel() {

    val notesFlow: StateFlow<List<Note>> = repository.notes
    val archivedNotesFlow: StateFlow<List<Note>> = repository.archivedNotes
    val binNotes: StateFlow<List<Note>> = repository.binNotes
    val reminderNotes: StateFlow<List<Note>> = repository.reminderNotes


    private val searchQuery = MutableStateFlow("")

    private fun filterNotesByQuery(notes: List<Note>, query: String): List<Note> {
        return if (query.isBlank()) notes
        else notes.filter {
            it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
        }
    }

    fun getFilteredNotesFlow(context: NotesGridContext): StateFlow<List<Note>> {
        return when (context) {
            is NotesGridContext.Notes -> combine(notesFlow, searchQuery, ::filterNotesByQuery)

            is NotesGridContext.Archive -> combine(archivedNotesFlow, searchQuery, ::filterNotesByQuery)

            is NotesGridContext.Bin -> combine(binNotes, searchQuery, ::filterNotesByQuery)

            is NotesGridContext.Reminder -> combine(reminderNotes, searchQuery, ::filterNotesByQuery)

            is NotesGridContext.Label -> combine(notesFlow, searchQuery) { notes, query ->
                notes.filter {
                    it.labels.contains(context.labelName) &&
                            (query.isBlank() || it.title.contains(query, true) || it.content.contains(query, true))
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }




    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun fetchForContext(context: NotesGridContext) {
        when (context) {
            is NotesGridContext.Notes -> fetchNotes()
            is NotesGridContext.Archive -> fetchArchivedNotes()
            is NotesGridContext.Bin -> fetchBinNotes()
            is NotesGridContext.Reminder -> fetchReminderNotes()
            is NotesGridContext.Label -> fetchNotes()
        }
    }

    fun getFlowForContext(context: NotesGridContext): StateFlow<List<Note>> {
        return when (context) {
            is NotesGridContext.Notes -> notesFlow
            is NotesGridContext.Archive -> archivedNotesFlow
            is NotesGridContext.Bin -> binNotes
            is NotesGridContext.Reminder -> reminderNotes
            is NotesGridContext.Label -> notesFlow
        }
    }

    fun filterNotesForContext(notes: List<Note>, context: NotesGridContext): List<Note> {
        return when (context) {
            is NotesGridContext.Notes ->
                notes.filter { !it.archived && !it.inBin && !it.deleted }
            is NotesGridContext.Archive ->
                notes.filter { it.archived && !it.inBin && !it.deleted }
            is NotesGridContext.Bin ->
                notes.filter { it.inBin && !it.deleted }
            is NotesGridContext.Reminder ->
                notes.filter { it.hasReminder && !it.inBin }
            is NotesGridContext.Label ->
                notes.filter {
                    !it.archived && !it.inBin && !it.deleted && it.labels.contains(context.labelName)
                }
        }
    }
    fun fetchNotes() = repository.fetchNotes()
    fun fetchArchivedNotes() = repository.fetchArchivedNotes()
    fun fetchBinNotes() = repository.fetchBinNotes()
    fun fetchReminderNotes() = repository.fetchReminderNotes()

    fun saveNote(
        note: Note,
        context: Context,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        repository.addNote(note, {
            if (note.reminderTime != null) {
                ReminderScheduler(context).scheduleReminder(note, note.reminderTime)
            }
            onSuccess()
        }, onFailure)
    }

    fun updateNote(
        newNote: Note,
        existingNote: Note,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val reminderChanged = newNote.reminderTime != existingNote.reminderTime

        repository.updateNote(newNote, {
            if (newNote.hasReminder && reminderChanged) {
                ReminderScheduler(context).scheduleReminder(newNote, newNote.reminderTime!!)
            }
            onSuccess()
        }, onFailure)
    }



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
