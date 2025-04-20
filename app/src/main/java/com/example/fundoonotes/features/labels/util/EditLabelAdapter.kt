package com.example.fundoonotes.features.labels.util

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fundoonotes.common.data.model.Label

class EditLabelAdapter(
    private val mode: LabelAdapterMode,
    var preSelectedLabels: Set<String> = emptySet(),
    private val onDelete: ((Label) -> Unit)? = null,
    private val onRename: ((Label, String) -> Unit)? = null,
    private val onSelect: ((Label, Boolean) -> Unit)? = null
) : ListAdapter<Label, RecyclerView.ViewHolder>(DiffCallback()) {

    private var editPosition = -1

    override fun getItemViewType(position: Int): Int = if (mode == LabelAdapterMode.EDIT) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            EditLabelViewHolder(inflater, parent, onDelete, onRename) {
                val previous = editPosition
                editPosition = it
                if (previous != -1) notifyItemChanged(previous)
                notifyItemChanged(editPosition)
            }
        } else {
            SelectLabelViewHolder(inflater, parent, onSelect)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val label = getItem(position)
        when (holder) {
            is EditLabelViewHolder -> holder.bind(label, position == editPosition)
            is SelectLabelViewHolder -> {
                val isChecked = preSelectedLabels.contains(label.id)
                holder.bind(label, isChecked)
            }
        }
    }

    fun resetEditState() {
        val previous = editPosition
        editPosition = -1
        if (previous != -1) notifyItemChanged(previous)
    }

    fun updatePreSelectedLabels(newLabels: Set<String>) {
        preSelectedLabels = newLabels
        notifyDataSetChanged()
    }

    class DiffCallback : DiffUtil.ItemCallback<Label>() {
        override fun areItemsTheSame(oldItem: Label, newItem: Label): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Label, newItem: Label): Boolean = oldItem == newItem
    }
}