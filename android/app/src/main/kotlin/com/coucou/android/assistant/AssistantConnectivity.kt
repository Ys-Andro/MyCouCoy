package com.coucou.android.assistant

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

/**
 * App-lifetime connection observer for the phone's cloud models, independent of
 * the desktop Wi-Fi discovery service. A connected Wi-Fi network without
 * validated internet access is considered offline.
 *
 * The callback may run off the main thread; the owner must dispatch state
 * updates to the main thread before touching Compose state.
 */
class AssistantConnectivity(context: Context, private val onChanged: (Boolean) -> Unit) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private var observing = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = notifyCurrent()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            onChanged(connected(capabilities))
        }
        override fun onLost(network: Network) = notifyCurrent()
        override fun onUnavailable() = onChanged(false)
    }

    fun start() {
        if (observing) return
        try {
            connectivity.registerDefaultNetworkCallback(callback)
            observing = true
            notifyCurrent()
        } catch (_: SecurityException) {
            // The app declares ACCESS_NETWORK_STATE, but do not crash on
            // vendor-restricted devices. The HTTP transport still handles errors.
        } catch (_: RuntimeException) {
            // Never prevent a user from opening Chat because monitoring failed.
        }
    }

    fun stop() {
        if (!observing) return
        observing = false
        runCatching { connectivity.unregisterNetworkCallback(callback) }
    }

    private fun notifyCurrent() = onChanged(isOnline(connectivity))

    companion object {
        fun connected(capabilities: NetworkCapabilities?): Boolean =
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        fun isOnline(context: Context): Boolean =
            isOnline(context.getSystemService(ConnectivityManager::class.java))

        private fun isOnline(connectivity: ConnectivityManager): Boolean {
            val network = connectivity.activeNetwork ?: return false
            return connected(connectivity.getNetworkCapabilities(network))
        }
    }
}
