package com.example.fundoonotes.UI.util

import com.example.fundoonotes.UI.data.model.Label

interface LabelSelectionListener {
    fun onLabelListUpdated(selectedLabels: List<Label>)
}
