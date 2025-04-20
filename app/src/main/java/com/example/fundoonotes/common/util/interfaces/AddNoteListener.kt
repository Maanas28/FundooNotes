package com.example.fundoonotes.common.util.interfaces

import com.example.fundoonotes.common.data.model.Note

interface AddNoteListener {
    fun onNoteAdded(note: Note)
    fun onNoteUpdated(note: Note)
    fun onAddNoteCancelled()
}