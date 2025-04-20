package com.example.fundoonotes.common.database.repository.sqlite


class RepositoryInitializer(
    private val accountRepository: SQLiteAccountRepository,
    private val notesRepository: SQLiteNotesRepository,
    private val labelsRepository: SQLiteLabelsRepository
) {
    fun initialize() {
        accountRepository.fetchAccountDetails()
        notesRepository.setUpObservers()
        labelsRepository.fetchLabels()
    }
}
