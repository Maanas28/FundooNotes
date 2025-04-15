package com.example.fundoonotes.UI.util

import android.content.Context
import android.net.ConnectivityManager as SysConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.fundoonotes.UI.data.repository.DataBridgeNotesRepository

class ConnectivityManager(
    private val context: Context,
    private val repository: DataBridgeNotesRepository
) {
    private var networkCallback: SysConnectivityManager.NetworkCallback? = null
    private val sysConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as SysConnectivityManager

    /**
     * Checks if the network is available by ensuring the active network has both INTERNET
     * and VALIDATED capabilities.
     */
    fun isNetworkAvailable(): Boolean {
        val network = sysConnectivityManager.activeNetwork ?: return false
        val capabilities = sysConnectivityManager.getNetworkCapabilities(network) ?: return false
        Log.d("ConnectivityManager", "Network capabilities: $capabilities");
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Starts monitoring network changes and displays Toast messages when connectivity
     * is lost or restored. Also triggers sync when internet returns.
     */
    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        networkCallback = object : SysConnectivityManager.NetworkCallback() {
            var wasOffline = !isNetworkAvailable()

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                if (wasOffline) {
                    wasOffline = false
                    Log.d("ConnectivityManager", "Internet connection restored, initiating sync")
                    // Show Toast on the main thread.
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Internet connection is back", Toast.LENGTH_SHORT).show()
                    }
                    repository.syncOfflineChanges()
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                wasOffline = true
                Log.d("ConnectivityManager", "Internet connection lost")
                // Show Toast on the main thread.
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Internet connection lost", Toast.LENGTH_SHORT).show()
                }
            }
        }
        sysConnectivityManager.registerNetworkCallback(request, networkCallback!!)
    }

    /**
     * Stops monitoring network changes.
     */
    fun stopMonitoring() {
        networkCallback?.let {
            sysConnectivityManager.unregisterNetworkCallback(it)
            networkCallback = null
        }
    }
}
