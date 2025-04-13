package com.example.fundoonotes.UI.features.labels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.fundoonotes.UI.data.model.Label
import com.example.fundoonotes.UI.data.repository.DataBridgeNotesRepository
import kotlinx.coroutines.flow.StateFlow

class LabelsViewModel(
    private val repository: DataBridgeNotesRepository
) : ViewModel() {

    constructor(context: Context) : this(DataBridgeNotesRepository(context))

    val labelList: StateFlow<List<Label>> = repository.labels

    fun fetchLabels() = repository.fetchLabels()

    fun addLabel(name: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val dummyLabel = Label(id = "", name = name, userId = "")
        repository.addNewLabel(dummyLabel, onSuccess, onFailure)
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
