package com.example.fundoonotes.common.util.interfaces

import com.example.fundoonotes.common.data.model.Label

interface LabelActionHandler {
    fun onLabelToggledForNotes(label: Label, isChecked: Boolean, noteIds: List<String>)
}

