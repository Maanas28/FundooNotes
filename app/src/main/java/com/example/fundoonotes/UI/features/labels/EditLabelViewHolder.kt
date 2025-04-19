package com.example.fundoonotes.UI.features.labels

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.data.model.Label

class EditLabelViewHolder(
    inflater: LayoutInflater,
    parent: ViewGroup,
    private val onDelete: ((Label) -> Unit)?,
    private val onRename: ((Label, String) -> Unit)?,
    private val onEditRequested: (Int) -> Unit
) : RecyclerView.ViewHolder(inflater.inflate(R.layout.item_edit_label, parent, false)) {

    private val editText: EditText = itemView.findViewById(R.id.editLabelName)
    private val confirmButton: ImageButton = itemView.findViewById(R.id.btnConfirmLabel)
    private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteLabel)
    private val viewModeLayout: View = itemView.findViewById(R.id.viewModeLayout)
    private val editModeLayout: View = itemView.findViewById(R.id.editModeLayout)
    private val labelTextView: TextView = itemView.findViewById(R.id.labelNameTextView)

    fun bind(label: Label, isEditing: Boolean) {
        editText.setText(label.name)
        labelTextView.text = label.name

        viewModeLayout.visibility = if (isEditing) View.GONE else View.VISIBLE
        editModeLayout.visibility = if (isEditing) View.VISIBLE else View.GONE

        itemView.setOnClickListener {
            if (!isEditing) onEditRequested(adapterPosition)
        }

        deleteButton.setOnClickListener {
            onDelete?.invoke(label)
        }

        confirmButton.setOnClickListener {
            val newName = editText.text.toString().trim()
            if (newName.isNotEmpty() && newName != label.name) {
                onRename?.invoke(label, newName)
            }
        }
    }
}
