package com.example.fundoonotes.common.viewmodel

import androidx.lifecycle.ViewModel
import com.example.fundoonotes.common.data.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SelectionSharedViewModel : ViewModel() {

    private val _selectedNotes = MutableStateFlow<List<Note>>(emptyList())
    val selectedNotes: StateFlow<List<Note>> = _selectedNotes

    fun toggleSelection(note: Note) {
        val current = _selectedNotes.value.toMutableList()
        if (current.any { it.id == note.id }) {
            current.removeAll { it.id == note.id }
        } else {
            current.add(note)
        }
        _selectedNotes.value = current
    }

    fun setSelection(notes: List<Note>) {
        _selectedNotes.value = notes
    }

    fun clearSelection() {
        _selectedNotes.value = emptyList()
    }

    fun getSelection(): List<Note> = _selectedNotes.value
}