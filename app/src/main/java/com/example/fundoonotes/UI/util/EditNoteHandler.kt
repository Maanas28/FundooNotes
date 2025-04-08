package com.example.fundoonotes.UI.util

import com.example.fundoonotes.UI.data.model.Note

interface EditNoteHandler {
    fun onNoteEdit(note: Note)
    fun onNoteClick(note: Note)
    fun onNoteOptions(note: Note)
}