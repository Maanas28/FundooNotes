package com.example.fundoonotes.common.util.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fundoonotes.R
import com.example.fundoonotes.common.data.model.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteViewHolder(
    itemView: View,
    private val onNoteClick: (Note) -> Unit,
    private val onNoteLongClick: (Note) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private val title = itemView.findViewById<TextView>(R.id.noteTitle)
    private val content = itemView.findViewById<TextView>(R.id.noteContent)
    private val labelsContainer: LinearLayout = itemView.findViewById(R.id.labelsContainer)
    private val noteReminder: LinearLayout = itemView.findViewById(R.id.noteReminderLayout)
    private val noteReminderText: TextView = itemView.findViewById(R.id.noteReminderText)

    fun bind(note: Note, isSelected: Boolean) {

        title.text = note.title
        content.text = note.content

        itemView.setBackgroundResource(
            if (isSelected) R.drawable.selected_note_background
            else R.drawable.default_note_background
        )

        itemView.setOnClickListener { onNoteClick(note) }
        itemView.setOnLongClickListener {
            onNoteLongClick(note)
            true
        }

        bindLabels(note.labels)
        bindReminder(note)
    }

    private fun bindLabels(labels: List<String>) {
        labelsContainer.removeAllViews()
        val sortedLabels = labels.sortedByDescending { it.length }
        val truncate = { label: String -> if (label.length > 12) label.substring(0, 9) + "..." else label }
        val inflater = LayoutInflater.from(itemView.context)

        sortedLabels.take(2).forEach { label ->
            val pill = inflater.inflate(R.layout.item_note_label, labelsContainer, false) as TextView
            pill.text = truncate(label)
            labelsContainer.addView(pill)
        }

        if (sortedLabels.size > 2) {
            val morePill = inflater.inflate(R.layout.item_note_label, labelsContainer, false) as TextView
            morePill.text = "+${sortedLabels.size - 2}"
            labelsContainer.addView(morePill)
        }
    }

    private fun bindReminder(note: Note) {
        if (note.hasReminder && note.reminderTime != null) {
            val now = System.currentTimeMillis()
            val formatter = SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.getDefault())

            noteReminder.visibility = View.VISIBLE
            noteReminderText.text = formatter.format(Date(note.reminderTime))

            val isExpired = note.reminderTime < now
            if (isExpired) {
                noteReminderText.paintFlags = noteReminderText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                noteReminder.setBackgroundResource(R.drawable.reminder_pill_background_expired)
            } else {
                noteReminderText.paintFlags = noteReminderText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                noteReminderText.setTextColor(itemView.context.getColor(R.color.black))
                noteReminder.setBackgroundResource(R.drawable.reminder_pill_background)
            }
        } else {
            noteReminder.visibility = View.GONE
        }
    }
}