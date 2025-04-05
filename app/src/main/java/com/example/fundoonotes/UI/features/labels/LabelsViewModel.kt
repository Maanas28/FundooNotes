package com.example.fundoonotes.UI.features.labels

import android.util.Log
import com.example.fundoonotes.UI.data.repository.FirebaseNotesRepository
import androidx.lifecycle.ViewModel
import com.example.fundoonotes.UI.data.model.Label
import com.example.fundoonotes.UI.data.repository.NotesRepository
import kotlinx.coroutines.flow.StateFlow

class LabelsViewModel(
    private val repository: NotesRepository = FirebaseNotesRepository()
) : ViewModel() {

    val labelList: StateFlow<List<Label>> = repository.labels

    fun fetchLabels() = repository.fetchLabels()


    fun addLabel(name: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        repository.addNewLabel(name, onSuccess, onFailure)
    }

    fun updateLabel(
        oldLabel: Label,
        newLabel: Label,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        repository.updateLabel(oldLabel, newLabel, onSuccess, onFailure)
    }

    fun deleteLabel(label: Label, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        repository.deleteLabel(label, onSuccess, onFailure)
    }

    fun toggleLabelForNotes(label: Label, isChecked: Boolean, noteIds: List<String>) {
        repository.toggleLabelForNotes(label, isChecked, noteIds, {}, {})
    }


}
