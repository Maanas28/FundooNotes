package com.example.fundoonotes.UI.features.notes.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.data.repository.DataBridgeNotesRepository
import com.example.fundoonotes.UI.data.repository.NotesRepository
import com.example.fundoonotes.UI.util.NotesGridContext
import com.example.fundoonotes.UI.util.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotesViewModel(
    private val repository: NotesRepository
) : ViewModel() {

    private var dataBridgeRepository: DataBridgeNotesRepository? = null

    constructor(context: Context) : this(DataBridgeNotesRepository(context)) {
        dataBridgeRepository = repository as? DataBridgeNotesRepository
    }

    val notesFlow: StateFlow<List<Note>> = repository.notes
    val archivedNotesFlow: StateFlow<List<Note>> = repository.archivedNotes
    val binNotes: StateFlow<List<Note>> = repository.binNotes
    val reminderNotes: StateFlow<List<Note>> = repository.reminderNotes

    private val searchQuery = MutableStateFlow("")

    private val _pagedNotes = MutableStateFlow<List<Note>>(emptyList())
    val pagedNotes: StateFlow<List<Note>> = _pagedNotes

    val filteredPagedNotes: StateFlow<List<Note>> = combine(_pagedNotes, searchQuery) { notes, query ->
        if (query.isBlank()) notes
        else notes.filter {
            it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun fetchNotes() = repository.fetchNotes()
    fun fetchArchivedNotes() = repository.fetchArchivedNotes()
    fun fetchBinNotes() = repository.fetchBinNotes()
    fun fetchReminderNotes() = repository.fetchReminderNotes()

    private var currentPage = 0
    private val pageSize = 10
    private var isLoadingMore = false
    private var hasMoreData = true

    private var currentContext: NotesGridContext? = null

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    fun resetPagination() {
        currentPage = 0
        hasMoreData = true
        _pagedNotes.value = emptyList()
    }

    fun refreshAndLoad(context: NotesGridContext) {
        resetPagination()
        dataBridgeRepository?.resetPaginationFor(context)
        loadNextPage(context)
    }

    fun loadNextPage(context: NotesGridContext, onComplete: () -> Unit = {}) {
        if (isLoadingMore || !hasMoreData) {
            onComplete()
            return
        }

        isLoadingMore = true
        currentContext = context

        viewModelScope.launch {
            try {
                val newNotes = dataBridgeRepository?.fetchNotesPage(context, currentPage, pageSize).orEmpty()
                if (newNotes.isNotEmpty()) {
                    // Update paged notes without recreating the entire list
                    val currentNotes = _pagedNotes.value
                    _pagedNotes.value = currentNotes + newNotes
                    currentPage++
                } else {
                    hasMoreData = false
                }
            } catch (e: Exception) {
                Log.e("NotesViewModel", "Pagination load failed", e)
            } finally {
                isLoadingMore = false
                onComplete()
            }
        }
    }

    fun saveNote(
        note: Note,
        context: Context,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        repository.addNote(note, {
            // Update the paged notes immediately before refresh
            _pagedNotes.value = _pagedNotes.value + note

            refreshAndLoad(currentContext ?: NotesGridContext.Notes)
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
            // Update the UI immediately
            _pagedNotes.value = _pagedNotes.value.map { if (it.id == newNote.id) newNote else it }

            if (newNote.hasReminder && reminderChanged) {
                ReminderScheduler(context).scheduleReminder(newNote, newNote.reminderTime!!)
            }
            refreshAndLoad(currentContext ?: NotesGridContext.Notes)
            onSuccess()
        }, onFailure)
    }

    fun deleteNote(
        note: Note,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        repository.deleteNote(note, {
            // Update UI immediately
            _pagedNotes.value = _pagedNotes.value.filter { it.id != note.id }

            refreshAndLoad(currentContext ?: NotesGridContext.Notes)
            onSuccess()
        }, onFailure)
    }

    fun archiveNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) =
        repository.archiveNote(note, {
            // Update UI immediately
            _pagedNotes.value = _pagedNotes.value.filter { it.id != note.id }

            refreshAndLoad(currentContext ?: NotesGridContext.Notes)
            onSuccess()
        }, onFailure)

    fun unarchiveNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) =
        repository.unarchiveNote(note, {
            // Update UI immediately
            _pagedNotes.value = _pagedNotes.value.filter { it.id != note.id }

            refreshAndLoad(currentContext ?: NotesGridContext.Notes)
            onSuccess()
        }, onFailure)

    fun permanentlyDeleteNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) =
        repository.permanentlyDeleteNote(note, {
            // Update UI immediately
            _pagedNotes.value = _pagedNotes.value.filter { it.id != note.id }

            refreshAndLoad(currentContext ?: NotesGridContext.Notes)
            onSuccess()
        }, onFailure)

    fun restoreNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) =
        repository.restoreNote(note, {
            // Update UI immediately
            _pagedNotes.value = _pagedNotes.value.filter { it.id != note.id }

            refreshAndLoad(currentContext ?: NotesGridContext.Notes)
            onSuccess()
        }, onFailure)

    fun reverseSyncNotes(context: Context) {
        _syncState.value = SyncState.Syncing
        viewModelScope.launch {
            dataBridgeRepository?.syncOnlineChanges(
                context = context,
                onSuccess = {
                    refreshAndLoad(currentContext ?: NotesGridContext.Notes)
                    _syncState.value = SyncState.Success
                },
                onFailure = { exception ->
                    _syncState.value = SyncState.Failed(exception.message ?: "Unknown error")
                }
            ) ?: run {
                _syncState.value = SyncState.Failed("Incompatible repository type")
            }
        }
    }

    sealed class SyncState {
        object Idle : SyncState()
        object Syncing : SyncState()
        object Success : SyncState()
        data class Failed(val error: String) : SyncState()
    }
}

