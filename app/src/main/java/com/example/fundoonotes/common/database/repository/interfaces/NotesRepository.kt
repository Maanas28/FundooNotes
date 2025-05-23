package com.example.fundoonotes.common.database.repository.interfaces

import com.example.fundoonotes.common.data.model.Note
import kotlinx.coroutines.flow.StateFlow

interface NotesRepository {

    val notes: StateFlow<List<Note>>
    val archivedNotes: StateFlow<List<Note>>
    val binNotes: StateFlow<List<Note>>
    val reminderNotes: StateFlow<List<Note>>
    val notesByLabel: StateFlow<List<Note>>
    val labelsForNote : StateFlow<List<String>>
    val filteredNotes: StateFlow<List<Note>>

    fun fetchNotes()
    fun fetchArchivedNotes()
    fun fetchBinNotes()
    fun fetchReminderNotes()

    fun addNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})
    fun updateNote(note : Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)
    fun archiveNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})
    fun unarchiveNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})
    fun deleteNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})
    fun permanentlyDeleteNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})
    fun restoreNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})
    fun setReminder(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})
}