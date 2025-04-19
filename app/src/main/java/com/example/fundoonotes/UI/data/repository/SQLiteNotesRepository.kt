package com.example.fundoonotes.UI.data.repository

import android.util.Log
import com.example.fundoonotes.UI.data.dao.LabelDao
import com.example.fundoonotes.UI.data.dao.NoteDao
import com.example.fundoonotes.UI.data.dao.OfflineOperationDao
import com.example.fundoonotes.UI.data.dao.UserDao
import com.example.fundoonotes.UI.data.entity.OfflineOperation
import com.example.fundoonotes.UI.data.entity.UserEntity
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

    fun setUpObservers(){
        fetchNotes()
        fetchArchivedNotes()
        fetchBinNotes()
        fetchReminderNotes()
        fetchLabels()
    }

    suspend fun saveUserLocally(user: User) {
        val entity = UserEntity(
            userId = user.userId ?: "", // Firebase UID should be non-null at this point
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            profileImage = user.profileImage
        )
        userDao.insertUser(entity)
    }



    override fun fetchNotes() {
        val userId = accountDetails.value?.userId ?: return
        scope.launch {
            noteDao.observeNotes(userId).collect {
                _notes.emit(it.map { e -> e.toDomain() })
            }
        }
    }

    override fun fetchArchivedNotes() {
        val userId = accountDetails.value?.userId ?: return
        scope.launch {
            noteDao.observeArchivedNotes(userId).collect {
                _archivedNotes.emit(it.map { e -> e.toDomain() })
            }
        }
    }

    override fun fetchBinNotes() {
        val userId = accountDetails.value?.userId ?: return
        scope.launch {
            noteDao.observeBinNotes(userId).collect {
                _binNotes.emit(it.map { e -> e.toDomain() })
            }
        }
    }

    override fun fetchReminderNotes() {
        val userId = accountDetails.value?.userId ?: return
        scope.launch {
            noteDao.observeReminderNotes(userId).collect {
                _reminderNotes.emit(it.map { e -> e.toDomain() })
            }
        }
    }

    override fun fetchLabels() {
        val userId = accountDetails.value?.userId ?: return
        scope.launch {
            labelDao.observeLabels(userId).collect {
                _labels.emit(it.map { label -> label.toDomain() })
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

                // Start observing all flows if user exists
                user?.let {
                    setUpObservers()
                }
            } catch (e: Exception) {
                // Handle error
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
                val userId = accountDetails.value?.userId ?: return@launch

                withContext(Dispatchers.IO) {
                    // Update label
                    labelDao.updateLabel(newLabel.toEntity())

                    // Update notes that use this label
                    noteDao.getAllNotes(userId).forEach { noteEntity ->
                        val labels = parseLabels(noteEntity.labels)
                        if (labels.contains(oldLabel.name)) {
                            val updatedLabels = labels.map { if (it == oldLabel.name) newLabel.name else it }
                            noteDao.updateNote(noteEntity.copy(labels = formatLabels(updatedLabels)))
                        }
                    }
                }

                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }


    override fun deleteLabel(label: Label, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        scope.launch {
            try {
                val userId = accountDetails.value?.userId ?: return@launch

                withContext(Dispatchers.IO) {
                    // Delete label
                    labelDao.deleteLabel(label.id)

                    // Remove label from notes
                    noteDao.getAllNotes(userId).forEach { noteEntity ->
                        val labels = parseLabels(noteEntity.labels)
                        if (labels.contains(label.name)) {
                            val updatedLabels = labels.filterNot { it == label.name }
                            noteDao.updateNote(noteEntity.copy(labels = formatLabels(updatedLabels)))
                        }
                    }
                }

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
                    noteIds.forEach { noteId ->
                        val noteEntity = noteDao.getNoteById(noteId) ?: return@forEach
                        val labels = parseLabels(noteEntity.labels)

                        val updatedLabels = if (isChecked) {
                            if (!labels.contains(label.name)) labels + label.name else labels
                        } else {
                            labels.filterNot { it == label.name }
                        }

                        noteDao.updateNote(noteEntity.copy(labels = formatLabels(updatedLabels)))
                    }
                }

                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
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

    fun insertUser(user: User) {
        scope.launch {
            userDao.insertUser(user.toEntity())
        }
    }

    fun replaceAllNotes(notes: List<Note>, onComplete: () -> Unit) {
        scope.launch {
            val userId = accountDetails.value?.userId ?: return@launch
            noteDao.clearNotesForUser(userId)
            notes.forEach {
                noteDao.insertNote(it.toEntity())
            }
            onComplete()
        }
    }

    fun replaceAllLabels(labels: List<Label>, onComplete: () -> Unit) {
        scope.launch {
            val userId = accountDetails.value?.userId ?: return@launch
            labelDao.clearLabelsForUser(userId)
            labels.forEach {
                labelDao.insertLabel(it.toEntity())
            }
            onComplete()
        }
    }


}