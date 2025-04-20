package com.example.fundoonotes.common.database.repository.sqlite

import com.example.fundoonotes.common.data.dao.LabelDao
import com.example.fundoonotes.common.data.dao.NoteDao
import com.example.fundoonotes.common.data.dao.OfflineOperationDao
import com.example.fundoonotes.common.data.dao.UserDao
import com.example.fundoonotes.common.database.NotesDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

abstract class BaseSQLiteRepository(context: android.content.Context) {

    protected val db = NotesDatabase.getInstance(context)
    protected val noteDao: NoteDao = db.noteDao()
    protected val labelDao: LabelDao = db.labelDao()
    protected val userDao: UserDao = db.userDao()
    protected val offlineOperationDao: OfflineOperationDao = db.offlineOperationDao()
    protected val scope = CoroutineScope(Dispatchers.IO)


    private val LABEL_SEPARATOR = "\u001F"

    protected fun parseLabels(str: String): List<String> =
        if (str.isBlank()) emptyList() else str.split(LABEL_SEPARATOR)

    protected fun formatLabels(labels: List<String>): String =
        labels.joinToString(LABEL_SEPARATOR)

}