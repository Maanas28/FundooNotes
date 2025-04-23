package com.example.fundoonotes.common.database.repository.databridge

import android.content.Context
import android.util.Log
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

// Abstract base class providing offline-first bridging between Firebase and SQLite
abstract class DataBridge<T>(
    protected val context: Context
) {
    // Firebase repositories for cloud operations
    protected val firebase = FirebaseNotesRepository()
    protected val firebaseLabel = FirebaseLabelsRepository()
    protected val firebaseAuth = FirebaseAuthRepository(FirebaseAuth.getInstance())

    // SQLite repositories for offline storage and access
    protected val sqliteAccount = SQLiteAccountRepository(context)
    protected val sqlite = SQLiteNotesRepository(context, sqliteAccount)
    protected val sqliteLabels = SQLiteLabelsRepository(context, sqliteAccount)
    protected val sqliteOffline = SQLiteOfflineOperationsRepository(context)

    // JSON parser for serializing offline payloads
    protected val gson = Gson()

    // SyncManager handles syncing operations between SQLite and Firebase
    protected val syncManager = SyncManager(sqliteLabels, sqliteOffline, sqlite, firebase, firebaseLabel)

    init {
        // Initialize and observe SQLite repositories after account data is available
        RepositoryInitializer(sqliteAccount).initialize {
            sqlite.setUpObservers()
            sqliteLabels.fetchLabels()
        }

        syncOfflineChanges()
    }

    // Utility method to check internet connectivity
    fun isOnline(): Boolean = NetworkUtils.isOnline(context)

    // Log the user out of Firebase
    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }

    // Trigger syncing of all offline operations
    fun syncOfflineChanges() = syncManager.syncOfflineChanges()

    // Reverse-sync data from Firebase to Room safely with exception handling
    suspend fun syncOnlineChangesSafe(): Result<Unit> {
        return try {
            val userId = sqliteAccount.getUserId() ?: return Result.failure(Exception("User ID not found"))
            syncManager.syncOnlineChanges(userId) // Perform two-way sync
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Track a local operation to be synced later when online
    protected fun trackOfflineOperation(type: String, entityType: String, entityId: String, entity: Any) {
        val payload = gson.toJson(entity) // Serialize object to JSON
        val offlineOp = OfflineOperation(
            operationType = type,
            entityType = entityType,
            entityId = entityId,
            timestamp = System.currentTimeMillis(),
            payload = payload
        )
        // Save the offline operation and trigger observer updates
        sqliteOffline.saveOfflineOperation(offlineOp) {

            // Re-fetch to refresh observers (e.g., UI updates)
            when (entityType) {
                "NOTE" -> sqlite.fetchNotes()
                "LABEL" -> sqliteLabels.fetchLabels()
            }
        }
    }
}
