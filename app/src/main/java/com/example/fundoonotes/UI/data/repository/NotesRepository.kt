package com.example.fundoonotes.UI.data.repository

import com.example.fundoonotes.UI.data.model.Label
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.data.model.User
import kotlinx.coroutines.flow.StateFlow

interface NotesRepository {

    val notes: StateFlow<List<Note>>
    val archivedNotes: StateFlow<List<Note>>
    val binNotes: StateFlow<List<Note>>
    val reminderNotes: StateFlow<List<Note>>
    val labels: StateFlow<List<Label>>
    val notesByLabel: StateFlow<List<Note>>
    val labelsForNote :  StateFlow<List<String>>
    val accountDetails : StateFlow<User?>
    val filteredNotes: StateFlow<List<Note>>

    fun fetchNotes()
    fun fetchArchivedNotes()
    fun fetchBinNotes()
    fun fetchReminderNotes()
    fun fetchLabels()
    fun fetchAccountDetails()

    fun addNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})
    fun updateNote(note : Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)
    fun archiveNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})
    fun unarchiveNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})
    fun deleteNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})
    fun permanentlyDeleteNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})
    fun restoreNote(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})
    fun setReminder(note: Note, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})
    fun addNewLabel(label: Label, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)
    fun updateLabel(oldLabel: Label, newLabel: Label, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})
    fun deleteLabel(label: Label, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})
    fun toggleLabelForNotes(label: Label, isChecked: Boolean, noteIds: List<String>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)
}
