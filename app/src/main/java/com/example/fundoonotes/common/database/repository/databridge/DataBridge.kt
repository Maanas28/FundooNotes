package com.example.fundoonotes.common.database.repository.databridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.fundoonotes.common.data.entity.OfflineOperation
import com.example.fundoonotes.common.database.repository.firebase.FirebaseAuthRepository
import com.example.fundoonotes.common.database.repository.firebase.FirebaseLabelsRepository
import com.example.fundoonotes.common.database.repository.firebase.FirebaseNotesRepository
import com.example.fundoonotes.common.database.repository.sqlite.RepositoryInitializer
import com.example.fundoonotes.common.database.repository.sqlite.SQLiteAccountRepository
import com.example.fundoonotes.common.database.repository.sqlite.SQLiteLabelsRepository
import com.example.fundoonotes.common.database.repository.sqlite.SQLiteNotesRepository
import com.example.fundoonotes.common.database.repository.sqlite.SQLiteOfflineOperationsRepository
import com.example.fundoonotes.common.util.managers.NetworkUtils
import com.example.fundoonotes.common.util.managers.SyncManager
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class DataBridge<T>(
    protected val context: Context
) {
    protected val firebase = FirebaseNotesRepository()
    protected val firebaseLabel = FirebaseLabelsRepository()
    protected val firebaseAuth = FirebaseAuthRepository(FirebaseAuth.getInstance())

    // SQLite Repositories initialized via BaseSQLiteRepository
    protected val sqliteAccount = SQLiteAccountRepository(context)
    protected val sqlite = SQLiteNotesRepository(context, sqliteAccount)
    protected val sqliteLabels = SQLiteLabelsRepository(context, sqliteAccount)
    protected val sqliteOffline = SQLiteOfflineOperationsRepository(context)

    protected val gson = Gson()
    protected val syncManager = SyncManager(sqliteLabels,sqliteOffline, sqlite, firebase, firebaseLabel)

    init {
        RepositoryInitializer(sqliteAccount, sqlite, sqliteLabels).initialize()
        syncOfflineChanges()
    }

    fun isOnline(): Boolean = NetworkUtils.isOnline(context)

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }

    fun syncOfflineChanges() = syncManager.syncOfflineChanges()

    suspend fun syncOnlineChangesSafe(): Result<Unit> {
        return try {
            val userId = sqliteAccount.getUserId() ?: return Result.failure(Exception("User ID not found"))
            syncManager.syncOnlineChanges(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    protected fun trackOfflineOperation(type: String, entityType: String, entityId: String, entity: Any) {
        val payload = gson.toJson(entity)
        val offlineOp = OfflineOperation(
            operationType = type,
            entityType = entityType,
            entityId = entityId,
            timestamp = System.currentTimeMillis(),
            payload = payload
        )
        sqliteOffline.saveOfflineOperation(offlineOp) {
            Log.d("DataBridge", "Tracked offline operation: $type for $entityType with id $entityId")

            when (entityType) {
                "NOTE" -> sqlite.fetchNotes()
                "LABEL" -> sqliteLabels.fetchLabels()
            }
        }
    }
}
