package com.example.fundoonotes.features.labels.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.example.fundoonotes.common.data.model.Label
import com.example.fundoonotes.common.database.repository.databridge.DataBridgeLabelRepository
import kotlinx.coroutines.flow.StateFlow

class LabelsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DataBridgeLabelRepository(application.applicationContext)


    val labelList: StateFlow<List<Label>> = repository.labels

    fun fetchLabels() = repository.fetchLabels()

    fun addLabel(name: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val existing = labelList.value.any { it.name.equals(name.trim(), ignoreCase = true) }

        if (existing) {
            onFailure(Exception("Label '$name' already exists"))
            return
        }

        val dummyLabel = Label(id = "", name = name.trim(), userId = "")
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