package com.example.fundoonotes.features.notes.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.fundoonotes.common.data.model.Note
import com.example.fundoonotes.common.util.interfaces.AddNoteListener
import com.example.fundoonotes.features.addnote.AddNoteFragment
import com.example.fundoonotes.features.addnote.CanvasNoteFragment
import com.example.fundoonotes.features.addnote.ImageNoteFragment
import com.example.fundoonotes.features.addnote.ListNoteFragement
import com.example.fundoonotes.features.addnote.MicrophoneNoteFragement

object DashboardNavigationManager {

    fun openAddOrEditNote(
        fragmentManager: FragmentManager,
        containerId: Int,
        note: Note?,
        listener: AddNoteListener
    ) {
        val fragment = AddNoteFragment.Companion.newInstance(note).apply {
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
        fragment: Fragment
    ) {
        fragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(null)
            .commit()
    }
}
