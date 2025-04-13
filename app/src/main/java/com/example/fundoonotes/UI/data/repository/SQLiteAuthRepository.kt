package com.example.fundoonotes.UI.data.repository

import com.example.fundoonotes.UI.data.dao.UserDao
import com.example.fundoonotes.UI.data.mappers.toDomain
import com.example.fundoonotes.UI.data.mappers.toEntity
import com.example.fundoonotes.UI.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SQLiteAuthRepository(
    private val userDao: UserDao,
    private val scope: CoroutineScope
) {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> get() = _currentUser

    fun registerUserLocally(user: User, onComplete: (Boolean, String?) -> Unit) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                userDao.insertUser(user.toEntity())
            }.onSuccess {
                _currentUser.emit(user)
                onComplete(true, null)
            }.onFailure {
                onComplete(false, it.message)
            }
        }
    }

    fun loginUserLocally(email: String, onComplete: (Boolean, String?) -> Unit) {
        scope.launch(Dispatchers.IO) {
            val users = userDao.getAllUsers()
            val user = users.find { it.email == email }?.toDomain()
            if (user != null) {
                _currentUser.emit(user)
                onComplete(true, null)
            } else {
                onComplete(false, "User not found.")
            }
        }
    }

    fun logoutUserLocally(onComplete: () -> Unit = {}) {
        scope.launch(Dispatchers.IO) {
            userDao.clearUsers()
            _currentUser.emit(null)
            onComplete()
        }
    }

    fun fetchCurrentUser(onComplete: (User?) -> Unit) {
        scope.launch(Dispatchers.IO) {
            val user = userDao.getAllUsers().firstOrNull()?.toDomain()
            _currentUser.emit(user)
            onComplete(user)
        }
    }
}
