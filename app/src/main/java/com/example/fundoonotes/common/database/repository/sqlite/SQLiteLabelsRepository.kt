package com.example.fundoonotes.common.database.repository.sqlite

import android.content.Context
import com.example.fundoonotes.common.data.mappers.toDomain
import com.example.fundoonotes.common.data.mappers.toEntity
import com.example.fundoonotes.common.data.model.Label
import com.example.fundoonotes.common.database.repository.interfaces.LabelsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SQLiteLabelsRepository(
    context: Context,
    private val sqliteAccount: SQLiteAccountRepository
) :
    BaseSQLiteRepository(context), LabelsRepository {


        private val _labels = MutableStateFlow<List<Label>>(emptyList())
        override val labels: StateFlow<List<Label>> = _labels.asStateFlow()

    override fun fetchLabels() {
        scope.launch {
            val userId = sqliteAccount.getUserId() ?: return@launch
            labelDao.observeLabels(userId).collect {
                _labels.emit(it.map { entity -> entity.toDomain() })
            }
        }
    }

    override fun addNewLabel(label: Label, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    labelDao.insertLabel(label.toEntity())
                }
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
                    labelDao.updateLabel(newLabel.toEntity())
                    noteDao.getAllNotes(sqliteAccount.getUserId()!!).forEach {
                        val labels = parseLabels(it.labels)
                        if (labels.contains(oldLabel.name)) {
                            val updatedLabels = labels.map { l -> if (l == oldLabel.name) newLabel.name else l }
                            noteDao.updateNote(it.copy(labels = formatLabels(updatedLabels)))
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
                withContext(Dispatchers.IO) {
                    labelDao.deleteLabel(label.id)
                    noteDao.getAllNotes(sqliteAccount.getUserId()!!).forEach {
                        val labels = parseLabels(it.labels)
                        if (labels.contains(label.name)) {
                            val updatedLabels = labels.filterNot { it == label.name }
                            noteDao.updateNote(it.copy(labels = formatLabels(updatedLabels)))
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
                    noteIds.forEach { id ->
                        val note = noteDao.getNoteById(id) ?: return@forEach
                        val labels = parseLabels(note.labels)
                        val updated = if (isChecked) labels + label.name else labels - label.name
                        noteDao.updateNote(note.copy(labels = formatLabels(updated)))
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    fun replaceAllLabels(labels: List<Label>, onComplete: () -> Unit) {
        scope.launch {
            val userId = sqliteAccount.getUserId()?: return@launch
            labelDao.clearLabelsForUser(userId)
            labels.forEach {
                labelDao.insertLabel(it.toEntity())
            }
            onComplete()
        }
    }
}
