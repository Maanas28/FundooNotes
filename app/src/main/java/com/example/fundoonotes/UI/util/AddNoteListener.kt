package com.example.fundoonotes.UI.util

import com.example.fundoonotes.UI.data.model.Note

interface AddNoteListener {
    fun onNoteAdded(note: Note)
    fun onNoteUpdated(note: Note)
    fun onAddNoteCancelled()
}