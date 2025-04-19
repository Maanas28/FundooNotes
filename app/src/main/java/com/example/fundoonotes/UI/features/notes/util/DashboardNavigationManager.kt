package com.example.fundoonotes.UI.features.notes.util

import androidx.fragment.app.FragmentManager
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.features.addnote.*
import com.example.fundoonotes.UI.util.AddNoteListener
import com.example.fundoonotes.UI.data.model.Note

object DashboardNavigationManager {

    fun openAddOrEditNote(
        fragmentManager: FragmentManager,
        containerId: Int,
        note: Note?,
        listener: AddNoteListener
    ) {
        val fragment = AddNoteFragment.newInstance(note).apply {
            setAddNoteListener(listener)
        }
        openFullScreen(fragmentManager, containerId, fragment)
    }

    fun openCanvasNote(fragmentManager: FragmentManager, containerId: Int) {
        openFullScreen(fragmentManager, containerId, CanvasNoteFragment("Canvas Notes"))
    }

    fun openListNote(fragmentManager: FragmentManager, containerId: Int) {
        openFullScreen(fragmentManager, containerId, ListNoteFragement("List Notes"))
    }

    fun openImageNote(fragmentManager: FragmentManager, containerId: Int) {
        openFullScreen(fragmentManager, containerId, ImageNoteFragment("Image Notes"))
    }

    fun openMicrophoneNote(fragmentManager: FragmentManager, containerId: Int) {
        openFullScreen(fragmentManager, containerId, MicrophoneNoteFragement("Microphone Notes"))
    }

    private fun openFullScreen(
        fragmentManager: FragmentManager,
        containerId: Int,
        fragment: androidx.fragment.app.Fragment
    ) {
        fragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(null)
            .commit()
    }
}
