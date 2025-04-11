package com.example.fundoonotes.UI.util

import com.example.fundoonotes.UI.data.model.Label

interface LabelActionHandler {
    fun onLabelToggledForNotes(label: Label, isChecked: Boolean, noteIds: List<String>)
}

