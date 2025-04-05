package com.example.fundoonotes.UI.features.labels

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.data.model.Label

class EditLabelAdapter(
    private val mode: LabelAdapterMode,
    private val preSelectedLabels: Set<String> = emptySet(),
    private val onDelete: ((Label) -> Unit)? = null,
    private val onRename: ((Label, String) -> Unit)? = null,
    private val onSelect: ((Label, Boolean) -> Unit)? = null
) : ListAdapter<Label, RecyclerView.ViewHolder>(DiffCallback()) {

    private var editPosition = -1

    override fun getItemViewType(position: Int): Int {
        return if (mode == LabelAdapterMode.EDIT) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_edit_label, parent, false)
            EditViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_label_checkbox, parent, false)
            SelectViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val label = getItem(position)
        if (holder is EditViewHolder) {
            holder.bind(label, position == editPosition)
        } else if (holder is SelectViewHolder) {
            holder.bind(label)
        }
    }

    fun resetEditState() {
        editPosition = -1
        notifyDataSetChanged()
    }

    inner class EditViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val editText = view.findViewById<EditText>(R.id.editLabelName)
        private val confirmButton = view.findViewById<ImageButton>(R.id.btnConfirmLabel)
        private val deleteButton = view.findViewById<ImageButton>(R.id.btnDeleteLabel)
        private val viewModeLayout = view.findViewById<View>(R.id.viewModeLayout)
        private val editModeLayout = view.findViewById<View>(R.id.editModeLayout)
        private val labelTextView = view.findViewById<TextView>(R.id.labelNameTextView)

        fun bind(label: Label, isEditing: Boolean) {
            editText.setText(label.name)
            labelTextView.text = label.name

            viewModeLayout.visibility = if (isEditing) View.GONE else View.VISIBLE
            editModeLayout.visibility = if (isEditing) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && editPosition != position) {
                    editPosition = position
                    notifyDataSetChanged()
                }
            }

            deleteButton.setOnClickListener {
                onDelete?.invoke(label)
            }

            confirmButton.setOnClickListener {
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != label.name) {
                    onRename?.invoke(label, newName)
                }
                editPosition = -1
                notifyDataSetChanged()
            }
        }
    }

    inner class SelectViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val labelName = view.findViewById<TextView>(R.id.labelName)
        private val checkbox = view.findViewById<CheckBox>(R.id.labelCheckbox)

        fun bind(label: Label) {
            labelName.text = label.name
            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = preSelectedLabels.contains(label.name)
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                onSelect?.invoke(label, isChecked)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Label>() {
        override fun areItemsTheSame(oldItem: Label, newItem: Label): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Label, newItem: Label): Boolean =
            oldItem == newItem
    }
}
