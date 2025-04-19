package com.example.fundoonotes.UI.features.notes.util

import com.example.fundoonotes.R
import com.example.fundoonotes.UI.features.addnote.*

object DashboardBottomNavHandler {

    fun getFragmentForItem(itemId: Int): androidx.fragment.app.Fragment? {
        return when (itemId) {
            R.id.nav_lists -> ListNoteFragement("List Notes")
            R.id.nav_audio -> MicrophoneNoteFragement("Microphone Notes")
            R.id.nav_canvas -> CanvasNoteFragment("Canvas Notes")
            R.id.nav_gallery -> ImageNoteFragment("Image Notes")
            else -> null
        }
    }
}