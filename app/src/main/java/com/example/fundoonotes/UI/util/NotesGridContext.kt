package com.example.fundoonotes.UI.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class NotesGridContext : Parcelable {
    @Parcelize object Notes : NotesGridContext()
    @Parcelize object Archive : NotesGridContext()
    @Parcelize object Bin : NotesGridContext()
    @Parcelize object Reminder : NotesGridContext()
    @Parcelize data class Label(val labelName: String) : NotesGridContext()
}
