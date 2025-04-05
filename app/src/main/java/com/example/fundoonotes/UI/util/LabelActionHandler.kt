package com.example.fundoonotes.UI.util

import com.example.fundoonotes.UI.data.model.Label

interface LabelActionHandler {
    fun onLabelSelected(noteIds: List<String>, isChecked: Boolean, selectedNoteIds: List<String>)
    fun onLabelToggledForNotes(label: Label, isChecked: Boolean, noteIds: List<String>)
}
