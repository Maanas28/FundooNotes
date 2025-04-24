package com.example.fundoonotes.common.database.repository.sqlite

import com.example.fundoonotes.common.data.mappers.toDomain
import com.example.fundoonotes.common.data.mappers.toEntity
import com.example.fundoonotes.common.data.model.User
import com.example.fundoonotes.common.database.repository.interfaces.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SQLiteAccountRepository(context: android.content.Context) :
    BaseSQLiteRepository(context), AccountRepository {

    private val _accountDetails = MutableStateFlow<User?>(null)
    override val accountDetails: StateFlow<User?> = _accountDetails.asStateFlow()

    override fun fetchAccountDetails() {
        scope.launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    userDao.getAllUsers().firstOrNull()?.toDomain()
                }
                _accountDetails.emit(user)
            } catch (e: Exception) {
                // Log or handle
            }
        }
    }

    fun saveUserLocally(user: User) {
        scope.launch {
            userDao.insertUser(user.toEntity())
            fetchAccountDetails() // ensures StateFlow is updated
        }
    }

    fun getUserId(): String? {
        return accountDetails.value?.userId
    }

    fun clearDatabase() {
        clearUserData()
    }

    fun clearUserData() {
        scope.launch {
            val userId = getUserId() ?: return@launch
            withContext(Dispatchers.IO) {
                userDao.clearUsers()
                noteDao.clearNotesForUser(userId)
                labelDao.clearLabelsForUser(userId)
            }
            _accountDetails.emit(null) // also clear local cache
        }
    }

}
