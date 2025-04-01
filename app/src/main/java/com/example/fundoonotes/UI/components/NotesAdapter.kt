package com.example.fundoonotes.UI.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.data.model.Note

class NotesAdapter(
    private var notes: List<Note>,
    private val onNoteClick: (Note) -> Unit,
    private val onNoteLongClick: (Note) -> Unit,
    private val selectedNotes: Set<Note>) :
    RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title = itemView.findViewById<TextView>(R.id.noteTitle)
        val content = itemView.findViewById<TextView>(R.id.noteContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.title.text = note.title
        holder.content.text = note.content

        // Set background based on selection state
        if (selectedNotes.contains(note)) {
            holder.itemView.setBackgroundResource(R.drawable.selected_note_background)
        } else {
            holder.itemView.setBackgroundResource(R.drawable.default_note_background)
        }

        holder.itemView.setOnClickListener {
            onNoteClick(note)
        }

        holder.itemView.setOnLongClickListener {
            onNoteLongClick(note)
            true
        }
    }

    override fun getItemCount() = notes.size

    fun updateList(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }
}


