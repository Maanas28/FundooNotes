package com.example.fundoonotes.features.notes.util

import androidx.fragment.app.Fragment
import com.example.fundoonotes.R
import com.example.fundoonotes.features.addnote.CanvasNoteFragment
import com.example.fundoonotes.features.addnote.ImageNoteFragment
import com.example.fundoonotes.features.addnote.ListNoteFragement
import com.example.fundoonotes.features.addnote.MicrophoneNoteFragement

object DashboardBottomNavHandler {

    fun getFragmentForItem(itemId: Int): Fragment? {
        return when (itemId) {
            R.id.nav_lists -> ListNoteFragement("List Notes")
            R.id.nav_audio -> MicrophoneNoteFragement("Microphone Notes")
            R.id.nav_canvas -> CanvasNoteFragment("Canvas Notes")
            R.id.nav_gallery -> ImageNoteFragment("Image Notes")
            else -> null
        }
    }
}