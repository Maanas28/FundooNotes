package com.example.fundoonotes

import android.app.Application
import com.example.fundoonotes.common.data.model.Note
import com.example.fundoonotes.common.database.repository.databridge.DataBridge
import com.example.fundoonotes.common.database.repository.databridge.DataBridgeNotesRepository
import com.example.fundoonotes.common.util.managers.ConnectivityManager

class FundooNotesApplication : Application() {

    private val repository: DataBridge<Note> by lazy {
        DataBridgeNotesRepository(this)
    }

    private val connectivityManager: ConnectivityManager<Note> by lazy {
        ConnectivityManager(this, repository)
    }

    override fun onCreate() {
        super.onCreate()
        connectivityManager.startMonitoring()
    }

    override fun onTerminate() {
        super.onTerminate()
        connectivityManager.stopMonitoring()
    }
}
