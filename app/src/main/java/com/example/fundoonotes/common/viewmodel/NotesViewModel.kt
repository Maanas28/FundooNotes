package com.example.fundoonotes.common.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundoonotes.common.data.model.Note
import com.example.fundoonotes.common.database.repository.databridge.DataBridgeNotesRepository
import com.example.fundoonotes.common.database.repository.interfaces.NotesRepository
import com.example.fundoonotes.common.util.NotesGridContext
import com.example.fundoonotes.features.reminders.util.ReminderScheduler
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val dataBridgeRepository = DataBridgeNotesRepository(application)
    private val repository: NotesRepository = dataBridgeRepository


    val notesFlow: StateFlow<List<Note>> = repository.notes
    val archivedNotesFlow: StateFlow<List<Note>> = repository.archivedNotes
    val binNotes: StateFlow<List<Note>> = repository.binNotes
    val reminderNotes: StateFlow<List<Note>> = repository.reminderNotes

    private val searchQuery = MutableStateFlow("")

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState


    fun getFilteredNotesFlow(context: NotesGridContext): StateFlow<List<Note>> {
        val baseFlow = when (context) {
            is NotesGridContext.Notes -> notesFlow
            is NotesGridContext.Archive -> archivedNotesFlow
            is NotesGridContext.Bin -> binNotes
            is NotesGridContext.Reminder -> reminderNotes
            is NotesGridContext.Label -> notesFlow
        }

        return searchQuery
            .debounce(300)
            .combine(baseFlow) { query, notes ->
                filterNotesForContext(notes, context).filter {
                    query.isBlank() ||
                            it.title.contains(query, true) ||
                            it.content.contains(query, true)
                }
            }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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

    fun filterNotesForContext(notes: List<Note>, context: NotesGridContext): List<Note> {
        return when (context) {
            is NotesGridContext.Notes -> notes.filter { !it.archived && !it.inBin && !it.deleted }
            is NotesGridContext.Archive -> notes.filter { it.archived && !it.inBin && !it.deleted }
            is NotesGridContext.Bin -> notes.filter { it.inBin && !it.deleted }
            is NotesGridContext.Reminder -> notes.filter { it.hasReminder && !it.inBin }
            is NotesGridContext.Label -> notes.filter {
                !it.archived && !it.inBin && !it.deleted && it.labels.contains(context.labelName)
            }
        }
    }

    fun fetchNotes() = repository.fetchNotes()
    fun fetchArchivedNotes() = repository.fetchArchivedNotes()
    fun fetchBinNotes() = repository.fetchBinNotes()
    fun fetchReminderNotes() = repository.fetchReminderNotes()

    fun saveNote(note: Note, context: Context, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
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
        repository.archiveNote(note, onSuccess, onFailure)

    fun unarchiveNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) =
        repository.unarchiveNote(note, onSuccess, onFailure)

    fun deleteNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) =
        repository.deleteNote(note, onSuccess, onFailure)

    fun permanentlyDeleteNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) =
        repository.permanentlyDeleteNote(note, onSuccess, onFailure)

    fun restoreNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) =
        repository.restoreNote(note, onSuccess, onFailure)

    fun reverseSyncNotes() {
        _syncState.value = SyncState.Syncing
        viewModelScope.launch {
            dataBridgeRepository.syncOnlineChanges(
                onSuccess = {
                    fetchNotes()
                    fetchArchivedNotes()
                    fetchBinNotes()
                    fetchReminderNotes()
                    _syncState.value = SyncState.Success
                },
                onFailure = { exception ->
                    _syncState.value = SyncState.Failed(exception.message ?: "Unknown error")
                }
            )
        }
    }

    fun checkInternetConnection(): Boolean = dataBridgeRepository.isOnline()

    fun getNotesByIds(ids: List<String>): List<Note> {
        return notesFlow.value.filter { it.id in ids }
    }

    sealed class SyncState {
        object Idle : SyncState()
        object Syncing : SyncState()
        object Success : SyncState()
        data class Failed(val error: String) : SyncState()
    }
}