package com.example.fundoonotes.common.database.repository.sqlite

import android.content.Context
import android.util.Log
import com.example.fundoonotes.common.data.mappers.toDomain
import com.example.fundoonotes.common.data.mappers.toEntity
import com.example.fundoonotes.common.data.model.Note
import com.example.fundoonotes.common.database.repository.interfaces.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SQLiteNotesRepository(
    context: Context,
    private val sqliteAccount: SQLiteAccountRepository ) :
    BaseSQLiteRepository(context), NotesRepository {


    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    override val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _archivedNotes = MutableStateFlow<List<Note>>(emptyList())
    override val archivedNotes: StateFlow<List<Note>> = _archivedNotes.asStateFlow()

    private val _binNotes = MutableStateFlow<List<Note>>(emptyList())
    override val binNotes: StateFlow<List<Note>> = _binNotes.asStateFlow()

    private val _reminderNotes = MutableStateFlow<List<Note>>(emptyList())
    override val reminderNotes: StateFlow<List<Note>> = _reminderNotes.asStateFlow()

    private val _notesByLabel = MutableStateFlow<List<Note>>(emptyList())
    override val notesByLabel: StateFlow<List<Note>> = _notesByLabel.asStateFlow()

    private val _filteredNotes = MutableStateFlow<List<Note>>(emptyList())
    override val filteredNotes: StateFlow<List<Note>> = _filteredNotes.asStateFlow()

    private val _labelsForNote = MutableStateFlow<List<String>>(emptyList())
    override val labelsForNote: StateFlow<List<String>> = _labelsForNote.asStateFlow()


    fun setUpObservers(){
        fetchNotes()
        fetchArchivedNotes()
        fetchReminderNotes()
        fetchBinNotes()
    }

    fun replaceAllNotes(notes: List<Note>, onComplete: () -> Unit) {

        scope.launch {
            val userId = sqliteAccount.getUserId() ?: return@launch
            Log.d("SQLiteNotesRepository", "Replacing notes in Room DB for userId: $userId. Count: ${notes.size}")
            noteDao.clearNotesForUser(userId)
            notes.forEach {
                Log.d("SQLiteNotesRepository", "Inserting note into Room: ${it.id}, title: ${it.title}")
                noteDao.insertNote(it.toEntity())
            }
            Log.d("SQLiteNotesRepository", "All notes inserted for userId: $userId")
            onComplete()
        }
    }


    fun fetchLabelsForNote(noteId: String) {
        scope.launch {
            val note = noteDao.getNoteById(noteId)
            val labels = parseLabels(note?.labels ?: "")
            _labelsForNote.emit(labels)
        }
    }

    override fun fetchNotes() {
        scope.launch {
            val userId = sqliteAccount.getUserId() ?: return@launch
            noteDao.observeNotes(userId).collect {
                _notes.emit(it.map { entity -> entity.toDomain() })
            }
        }
    }

    override fun fetchArchivedNotes() {
        scope.launch {
            val userId = sqliteAccount.getUserId() ?: return@launch
            noteDao.observeArchivedNotes(userId).collect {
                _archivedNotes.emit(it.map { entity -> entity.toDomain() })
            }
        }
    }

    override fun fetchBinNotes() {
        scope.launch {
            val userId = sqliteAccount.getUserId() ?: return@launch
            noteDao.observeBinNotes(userId).collect {
                _binNotes.emit(it.map { entity -> entity.toDomain() })
            }
        }
    }

    override fun fetchReminderNotes() {
        scope.launch {
            val userId = sqliteAccount.getUserId() ?: return@launch
            noteDao.observeReminderNotes(userId).collect {
                _reminderNotes.emit(it.map { entity -> entity.toDomain() })
            }
        }
    }


    override fun addNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    noteDao.insertNote(note.toEntity())
                }
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
                refreshNotesBasedOnState(note)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    override fun deleteNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val updated = note.copy(inBin = true)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    noteDao.updateNote(updated.toEntity())
                }
                fetchNotes()
                fetchBinNotes()
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
        val restored = note.copy(inBin = false)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    noteDao.updateNote(restored.toEntity())
                }
                fetchBinNotes()
                if (restored.archived) fetchArchivedNotes() else fetchNotes()
                if (restored.hasReminder) fetchReminderNotes()
                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    override fun archiveNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val archived = note.copy(archived = true)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    noteDao.updateNote(archived.toEntity())
                }
                fetchNotes()
                fetchArchivedNotes()
                if (archived.hasReminder) fetchReminderNotes()
                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    override fun unarchiveNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val unarchived = note.copy(archived = false)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    noteDao.updateNote(unarchived.toEntity())
                }
                fetchNotes()
                fetchArchivedNotes()
                if (unarchived.hasReminder) fetchReminderNotes()
                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    override fun setReminder(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val updated = note.copy(hasReminder = true)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    noteDao.updateNote(updated.toEntity())
                }
                fetchReminderNotes()
                if (updated.archived) fetchArchivedNotes() else if (!updated.inBin) fetchNotes()
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
                fetchNotes()
            }
            else -> fetchNotes()
        }
    }

    }
