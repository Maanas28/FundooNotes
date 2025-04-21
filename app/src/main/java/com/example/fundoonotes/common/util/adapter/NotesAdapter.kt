package com.example.fundoonotes.common.util.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fundoonotes.R
import com.example.fundoonotes.common.data.model.Note

class NotesAdapter(
    private var notes: List<Note>,
    private val onNoteClick: (Note) -> Unit,
    private val onNoteLongClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteViewHolder>() {

    private var selectedNotes: Set<Note> = emptySet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view, onNoteClick, onNoteLongClick)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        val isSelected = selectedNotes.any { it.id == note.id }
        holder.bind(note, isSelected)
    }

    override fun getItemCount() = notes.size

    fun updateList(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }

    fun updateSelectedNotes(newSelectedNotes: Set<Note>) {
        val oldSelectedNotes = selectedNotes
        selectedNotes = newSelectedNotes

        notes.forEachIndexed { index, note ->
            val wasSelected = oldSelectedNotes.any { it.id == note.id }
            val isSelected = selectedNotes.any { it.id == note.id }
            if (wasSelected != isSelected) {
                notifyItemChanged(index)
            }
        }
    }

    fun refreshSingleExpiredReminder(noteId: String) {
        val index = notes.indexOfFirst { it.id == noteId }

        if (index != -1) {
            val original = notes[index]
            val updated = original.copy(reminderTime = System.currentTimeMillis() - 1)
            notes = notes.toMutableList().apply { set(index, updated) }
            notifyItemChanged(index)
        }
    }


    fun getCurrentNotes(): List<Note> = notes
}