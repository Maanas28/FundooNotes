package com.example.fundoonotes.UI

import android.app.Application
import com.example.fundoonotes.UI.data.repository.DataBridgeNotesRepository
import com.example.fundoonotes.UI.util.ConnectivityManager

class FundooNotesApplication : Application() {
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var repository: DataBridgeNotesRepository

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