package com.example.fundoonotes.UI.data.repository

import com.example.fundoonotes.UI.data.dao.LabelDao
import com.example.fundoonotes.UI.data.dao.NoteDao
import com.example.fundoonotes.UI.data.dao.OfflineOperationDao
import com.example.fundoonotes.UI.data.dao.UserDao
import com.example.fundoonotes.UI.data.entity.OfflineOperation
import com.example.fundoonotes.UI.data.mappers.toDomain
import com.example.fundoonotes.UI.data.mappers.toEntity
import com.example.fundoonotes.UI.data.model.Label
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SQLiteNotesRepository(
    private val noteDao: NoteDao,
    private val labelDao: LabelDao,
    private val userDao: UserDao,
    private val offlineOperationDao: OfflineOperationDao,
    private val scope: CoroutineScope
) : NotesRepository {

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    override val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _archivedNotes = MutableStateFlow<List<Note>>(emptyList())
    override val archivedNotes: StateFlow<List<Note>> = _archivedNotes.asStateFlow()

    private val _binNotes = MutableStateFlow<List<Note>>(emptyList())
    override val binNotes: StateFlow<List<Note>> = _binNotes.asStateFlow()

    private val _reminderNotes = MutableStateFlow<List<Note>>(emptyList())
    override val reminderNotes: StateFlow<List<Note>> = _reminderNotes.asStateFlow()

    private val _labels = MutableStateFlow<List<Label>>(emptyList())
    override val labels: StateFlow<List<Label>> = _labels.asStateFlow()

    private val _notesByLabel = MutableStateFlow<List<Note>>(emptyList())
    override val notesByLabel: StateFlow<List<Note>> = _notesByLabel.asStateFlow()

    private val _labelsForNote = MutableStateFlow<List<String>>(emptyList())
    override val labelsForNote: StateFlow<List<String>> = _labelsForNote.asStateFlow()

    private val _accountDetails = MutableStateFlow<User?>(null)
    override val accountDetails: StateFlow<User?> = _accountDetails.asStateFlow()

    private val _filteredNotes = MutableStateFlow<List<Note>>(emptyList())
    override val filteredNotes: StateFlow<List<Note>> = _filteredNotes.asStateFlow()

    // Constants for label manipulation
    private val LABEL_SEPARATOR = "|:|" // Unlikely to appear in label names

    init {
        // Initialize by fetching account details first
        fetchAccountDetails()
    }

    fun saveOfflineOperation(operation: OfflineOperation, onComplete: () -> Unit) {
        scope.launch {
            offlineOperationDao.insertOfflineOperation(operation)
            onComplete()
        }
    }

    fun getOfflineOperations(onComplete: (List<OfflineOperation>) -> Unit) {
        scope.launch {
            val ops = offlineOperationDao.getOfflineOperations()
            onComplete(ops)
        }
    }

    fun removeOfflineOperation(opId: Int, onComplete: () -> Unit) {
        scope.launch {
            offlineOperationDao.removeOfflineOperation(opId)
            onComplete()
        }
    }

    override fun fetchNotes() {
        scope.launch {
            val userId = accountDetails.value?.userId ?: return@launch
            try {
                val notes = withContext(Dispatchers.IO) {
                    noteDao.getNotes(userId).map { it.toDomain() }
                }
                _notes.emit(notes)
            } catch (e: Exception) {
                // Log error or handle exception
            }
        }
    }

    override fun fetchArchivedNotes() {
        scope.launch {
            val userId = accountDetails.value?.userId ?: return@launch
            try {
                val archived = withContext(Dispatchers.IO) {
                    noteDao.getArchivedNotes(userId).map { it.toDomain() }
                }
                _archivedNotes.emit(archived)
            } catch (e: Exception) {
                // Log error or handle exception
            }
        }
    }

    override fun fetchBinNotes() {
        scope.launch {
            val userId = accountDetails.value?.userId ?: return@launch
            try {
                val bin = withContext(Dispatchers.IO) {
                    noteDao.getBinNotes(userId).map { it.toDomain() }
                }
                _binNotes.emit(bin)
            } catch (e: Exception) {
                // Log error or handle exception
            }
        }
    }

    override fun fetchReminderNotes() {
        scope.launch {
            val userId = accountDetails.value?.userId ?: return@launch
            try {
                val reminders = withContext(Dispatchers.IO) {
                    noteDao.getReminderNotes(userId).map { it.toDomain() }
                }
                _reminderNotes.emit(reminders)
            } catch (e: Exception) {
                // Log error or handle exception
            }
        }
    }

    override fun fetchLabels() {
        scope.launch {
            val userId = accountDetails.value?.userId ?: return@launch
            try {
                val allLabels = withContext(Dispatchers.IO) {
                    labelDao.getLabels(userId).map { it.toDomain() }
                }
                _labels.emit(allLabels)
            } catch (e: Exception) {
                // Log error or handle exception
            }
        }
    }

    override fun fetchAccountDetails() {
        scope.launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    userDao.getAllUsers().firstOrNull()?.toDomain()
                }
                _accountDetails.emit(user)

                // If user exists, fetch all their data
                user?.let {
                    fetchNotes()
                    fetchArchivedNotes()
                    fetchBinNotes()
                    fetchReminderNotes()
                    fetchLabels()
                }
            } catch (e: Exception) {
                // Log error or handle exception
            }
        }
    }

    override fun addNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    noteDao.insertNote(note.toEntity())
                }
                // Refresh appropriate flows based on note state
                refreshNotesBasedOnState(note)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    override fun updateNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    noteDao.updateNote(note.toEntity())
                }
                // Refresh appropriate flows based on note state
                refreshNotesBasedOnState(note)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    private fun refreshNotesBasedOnState(note: Note) {
        when {
            note.inBin -> fetchBinNotes()
            note.archived -> fetchArchivedNotes()
            note.hasReminder -> {
                fetchReminderNotes()
                fetchNotes() // Also refresh main notes if it has a reminder
            }
            else -> fetchNotes()
        }
    }

    override fun deleteNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val updatedNote = note.copy(inBin = true)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    noteDao.updateNote(updatedNote.toEntity())
                }
                // Refresh both main notes and bin notes
                fetchNotes()
                fetchBinNotes()
                // If note was archived or had reminder, refresh those lists too
                if (note.archived) fetchArchivedNotes()
                if (note.hasReminder) fetchReminderNotes()

                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    override fun permanentlyDeleteNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    noteDao.deleteNote(note.id)
                }
                // Refresh all relevant lists
                fetchBinNotes()
                fetchNotes()
                fetchArchivedNotes()
                fetchReminderNotes()

                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    override fun restoreNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val updatedNote = note.copy(inBin = false)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    noteDao.updateNote(updatedNote.toEntity())
                }
                // Refresh both bin notes and its destination list
                fetchBinNotes()
                if (updatedNote.archived) {
                    fetchArchivedNotes()
                } else {
                    fetchNotes()
                }
                if (updatedNote.hasReminder) {
                    fetchReminderNotes()
                }

                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    override fun archiveNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val updatedNote = note.copy(archived = true)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    noteDao.updateNote(updatedNote.toEntity())
                }
                // Refresh both main notes and archived notes
                fetchNotes()
                fetchArchivedNotes()
                if (updatedNote.hasReminder) {
                    fetchReminderNotes()
                }

                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    override fun unarchiveNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val updatedNote = note.copy(archived = false)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    noteDao.updateNote(updatedNote.toEntity())
                }
                // Refresh both main notes and archived notes
                fetchNotes()
                fetchArchivedNotes()
                if (updatedNote.hasReminder) {
                    fetchReminderNotes()
                }

                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    override fun setReminder(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val updatedNote = note.copy(hasReminder = true)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    noteDao.updateNote(updatedNote.toEntity())
                }
                // Refresh both reminder notes and the appropriate list based on note state
                fetchReminderNotes()
                if (updatedNote.archived) {
                    fetchArchivedNotes()
                } else if (!updatedNote.inBin) {
                    fetchNotes()
                }

                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    override fun addNewLabel(label: Label, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = accountDetails.value?.userId ?: return onFailure(Exception("User not logged in"))
        val labelEntity = Label(
            userId = label.userId,
            id = label.id,
            name = label.name
        ).toEntity()

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    labelDao.insertLabel(labelEntity)
                }
                // Refresh labels list
                fetchLabels()
                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    override fun updateLabel(oldLabel: Label, newLabel: Label, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Update the label itself
                    labelDao.updateLabel(newLabel.toEntity())

                    // Update all notes that have this label
                    val userId = accountDetails.value?.userId ?: return@withContext
                    val allNotes = noteDao.getNotes(userId) +
                            noteDao.getArchivedNotes(userId) +
                            noteDao.getBinNotes(userId)

                    for (noteEntity in allNotes) {
                        val labels = parseLabels(noteEntity.labels)
                        if (labels.contains(oldLabel.name)) {
                            val updatedLabels = labels.map {
                                if (it == oldLabel.name) newLabel.name else it
                            }
                            noteDao.updateNote(noteEntity.copy(
                                labels = formatLabels(updatedLabels)
                            ))
                        }
                    }
                }

                // Refresh all notes and labels
                fetchLabels()
                fetchNotes()
                fetchArchivedNotes()
                fetchBinNotes()
                fetchReminderNotes()

                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    override fun deleteLabel(label: Label, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Delete the label
                    labelDao.deleteLabel(label.id)

                    // Remove this label from all notes
                    val userId = accountDetails.value?.userId ?: return@withContext
                    val allNotes = noteDao.getNotes(userId) +
                            noteDao.getArchivedNotes(userId) +
                            noteDao.getBinNotes(userId)

                    for (noteEntity in allNotes) {
                        val labels = parseLabels(noteEntity.labels)
                        if (labels.contains(label.name)) {
                            val updatedLabels = labels.filter { it != label.name }
                            noteDao.updateNote(noteEntity.copy(
                                labels = formatLabels(updatedLabels)
                            ))
                        }
                    }
                }

                // Refresh labels and notes
                fetchLabels()
                fetchNotes()
                fetchArchivedNotes()
                fetchBinNotes()
                fetchReminderNotes()

                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    override fun toggleLabelForNotes(
        label: Label,
        isChecked: Boolean,
        noteIds: List<String>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    for (noteId in noteIds) {
                        val noteEntity = noteDao.getNoteById(noteId) ?: continue
                        val currentLabels = parseLabels(noteEntity.labels)

                        val updatedLabels = if (isChecked) {
                            if (!currentLabels.contains(label.name)) {
                                currentLabels + label.name
                            } else {
                                currentLabels
                            }
                        } else {
                            currentLabels.filterNot { it == label.name }
                        }

                        noteDao.updateNote(noteEntity.copy(
                            labels = formatLabels(updatedLabels)
                        ))
                    }
                }

                // Refresh notes
                fetchNotes()
                fetchArchivedNotes()
                fetchBinNotes()
                fetchReminderNotes()

                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    // Fetch notes by a specific label
    fun fetchNotesByLabel(labelName: String) {
        scope.launch {
            val userId = accountDetails.value?.userId ?: return@launch
            try {
                val allNotes = withContext(Dispatchers.IO) {
                    (noteDao.getNotes(userId) + noteDao.getArchivedNotes(userId))
                        .filter { !it.inBin && !it.deleted }
                        .map { it.toDomain() }
                        .filter { note ->
                            val noteLabels = parseLabels(note.toEntity().labels)
                            noteLabels.contains(labelName)
                        }
                }
                _notesByLabel.emit(allNotes)
            } catch (e: Exception) {
                // Log error or handle exception
            }
        }
    }

    // Fetch all labels for a specific note
    fun fetchLabelsForNote(noteId: String) {
        scope.launch {
            try {
                val note = withContext(Dispatchers.IO) {
                    noteDao.getNoteById(noteId)
                } ?: return@launch

                val noteLabels = parseLabels(note.labels)
                _labelsForNote.emit(noteLabels)
            } catch (e: Exception) {
                // Log error or handle exception
            }
        }
    }

    // Helper methods for label parsing
    private fun parseLabels(labelsString: String): List<String> {
        return if (labelsString.isBlank()) {
            emptyList()
        } else {
            labelsString.split(LABEL_SEPARATOR).filter { it.isNotBlank() }
        }
    }

    private fun formatLabels(labels: List<String>): String {
        return labels.joinToString(LABEL_SEPARATOR)
    }
}