package com.example.fundoonotes.features.labels.util

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fundoonotes.R
import com.example.fundoonotes.common.data.model.Label

class SelectLabelViewHolder(
    inflater: LayoutInflater,
    parent: ViewGroup,
    private val onSelect: ((Label, Boolean) -> Unit)?
) : RecyclerView.ViewHolder(inflater.inflate(R.layout.item_label_checkbox, parent, false)) {

    private val labelName: TextView = itemView.findViewById(R.id.labelName)
    private val checkbox: CheckBox = itemView.findViewById(R.id.labelCheckbox)

    fun bind(label: Label, isChecked: Boolean) {
        labelName.text = label.name
        checkbox.setOnCheckedChangeListener(null)
        checkbox.isChecked = isChecked
        checkbox.setOnCheckedChangeListener { _, newChecked ->
            onSelect?.invoke(label, newChecked)
        }
    }

}