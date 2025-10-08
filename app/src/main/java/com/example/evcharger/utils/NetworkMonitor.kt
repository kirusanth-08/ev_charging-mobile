package com.example.evcharger.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * NetworkMonitor - Monitors network connectivity changes
 * Provides real-time network status updates and handles reconnection logic
 */
class NetworkMonitor(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected
    
    private val _networkType = MutableLiveData<NetworkType>()
    val networkType: LiveData<NetworkType> = _networkType

    private var isMonitoring = false

    enum class NetworkType {
        WIFI,
        CELLULAR,
        ETHERNET,
        NONE
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            updateNetworkStatus(true)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            updateNetworkStatus(false)
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val validated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            
            if (hasInternet && validated) {
                updateNetworkStatus(true)
                updateNetworkType(networkCapabilities)
            } else {
                updateNetworkStatus(false)
            }
        }
    }

    init {
        // Initialize with current status
        _isConnected.postValue(isNetworkAvailable())
        _networkType.postValue(getCurrentNetworkType())
    }

    /**
     * Start monitoring network changes
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            isMonitoring = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Stop monitoring network changes
     */
    fun stopMonitoring() {
        if (!isMonitoring) return
        
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            isMonitoring = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Check if network is currently available
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Get current network type
     */
    private fun getCurrentNetworkType(): NetworkType {
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.NONE
        }
    }

    private fun updateNetworkStatus(isConnected: Boolean) {
        _isConnected.postValue(isConnected)
    }

    private fun updateNetworkType(capabilities: NetworkCapabilities) {
        val type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.NONE
        }
        _networkType.postValue(type)
    }

    companion object {
        @Volatile
        private var instance: NetworkMonitor? = null

        fun getInstance(context: Context): NetworkMonitor {
            return instance ?: synchronized(this) {
                instance ?: NetworkMonitor(context.applicationContext).also { instance = it }
            }
        }
    }
}
