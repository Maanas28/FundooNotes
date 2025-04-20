package com.example.fundoonotes.common.database.repository.sqlite

import com.example.fundoonotes.common.data.entity.OfflineOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SQLiteOfflineOperationsRepository(context: android.content.Context) : BaseSQLiteRepository(context) {

    private val _offlineOps = MutableStateFlow<List<OfflineOperation>>(emptyList())
    val offlineOperations: StateFlow<List<OfflineOperation>> = _offlineOps.asStateFlow()

    fun getOfflineOperations(onComplete: (List<OfflineOperation>) -> Unit) {
        scope.launch {
            val ops = offlineOperationDao.getOfflineOperations()
            onComplete(ops)
        }
    }
    fun saveOfflineOperation(op: OfflineOperation, onComplete: () -> Unit) {
        scope.launch {
            offlineOperationDao.insertOfflineOperation(op)
            onComplete()
        }
    }

    fun removeOfflineOperation(opId: Int, onComplete: () -> Unit) {
        scope.launch {
            offlineOperationDao.removeOfflineOperation(opId)
            onComplete()
        }
    }
}
