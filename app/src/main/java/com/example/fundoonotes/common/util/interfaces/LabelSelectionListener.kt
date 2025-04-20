package com.example.fundoonotes.common.util.interfaces

import com.example.fundoonotes.common.data.model.Label

interface LabelSelectionListener {
    fun onLabelListUpdated(selectedLabels: List<Label>)
}
