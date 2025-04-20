package com.example.fundoonotes.common.database.repository.interfaces

import com.example.fundoonotes.common.data.model.Label
import kotlinx.coroutines.flow.StateFlow

interface LabelsRepository {

    val labels: StateFlow<List<Label>>

    fun fetchLabels()

    fun addNewLabel(label: Label, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)
    fun updateLabel(oldLabel: Label, newLabel: Label, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})
    fun deleteLabel(label: Label, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {})
    fun toggleLabelForNotes(label: Label, isChecked: Boolean, noteIds: List<String>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit)
}