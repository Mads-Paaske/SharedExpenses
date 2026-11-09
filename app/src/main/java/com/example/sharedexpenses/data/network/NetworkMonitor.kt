package com.example.sharedexpenses.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Abstraction so tests can use a fake network. */
interface NetworkMonitor {
    /** true = requests can reach the server. */
    val online: StateFlow<Boolean>
    /** Demo switch from the lo-fi ("Demo: simulate network failure"). */
    val simulatedOffline: StateFlow<Boolean>
    fun setSimulatedOffline(offline: Boolean)
    fun isOnline(): Boolean = online.value
}

/** Real connectivity (ConnectivityManager) combined with the demo switch. */
class AndroidNetworkMonitor(context: Context) : NetworkMonitor {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private var deviceOnline = connectivity.activeNetwork
        ?.let { connectivity.getNetworkCapabilities(it) }
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    private val _simulatedOffline = MutableStateFlow(false)
    override val simulatedOffline: StateFlow<Boolean> = _simulatedOffline.asStateFlow()

    private val _online = MutableStateFlow(deviceOnline)
    override val online: StateFlow<Boolean> = _online.asStateFlow()

    init {
        connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = update(true)
            override fun onLost(network: Network) = update(false)
        })
    }

    private fun update(device: Boolean) {
        deviceOnline = device
        _online.value = deviceOnline && !_simulatedOffline.value
    }

    override fun setSimulatedOffline(offline: Boolean) {
        _simulatedOffline.value = offline
        update(deviceOnline)
    }
}
