package com.example.fundoonotes.common.util.interfaces

import com.example.fundoonotes.common.data.model.Note

interface EditNoteHandler {
    fun onNoteEdit(note: Note)
}
