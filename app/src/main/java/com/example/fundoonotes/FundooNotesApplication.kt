package com.example.fundoonotes

import android.app.Application
import com.example.fundoonotes.common.data.model.Note
import com.example.fundoonotes.common.database.repository.databridge.DataBridge
import com.example.fundoonotes.common.database.repository.databridge.DataBridgeNotesRepository
import com.example.fundoonotes.common.util.managers.ConnectivityManager

class FundooNotesApplication : Application() {
    private lateinit var connectivityManager: ConnectivityManager<Note>
    private lateinit var repository: DataBridge<Note>

    override fun onCreate() {
        super.onCreate()
        repository = DataBridgeNotesRepository(this)
        connectivityManager = ConnectivityManager(this, repository)
        connectivityManager.startMonitoring()
    }

    override fun onTerminate() {
        super.onTerminate()
        connectivityManager.stopMonitoring()
    }
}
