package com.example.fundoonotes.UI.features.notes.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class NotesGridContext : Parcelable {
    @Parcelize object Notes : NotesGridContext()
    @Parcelize object Archive : NotesGridContext()
    @Parcelize object Trash : NotesGridContext()
    @Parcelize object Reminder : NotesGridContext()
    @Parcelize data class Label(val labelName: String) : NotesGridContext()
}
