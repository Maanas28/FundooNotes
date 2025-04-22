package com.example.fundoonotes.common.database.repository.sqlite

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


class RepositoryInitializer(
    private val accountRepository: SQLiteAccountRepository,
) {
    fun initialize(onUserAvailable: () -> Unit) {
        accountRepository.fetchAccountDetails()

        CoroutineScope(Dispatchers.IO).launch {
            accountRepository.accountDetails.collect { user ->
                if (user != null) {
                    onUserAvailable()
                    cancel() // Stop collecting once we have the user
                }
            }
        }
    }


}
