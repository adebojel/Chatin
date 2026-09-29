package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ActiveNetworkState(
    val title: String,
    val description: String,
    val isVoipCapable: Boolean,
    val suggestedBitrateKbps: Int
) {
    WIFI_HD("Wi-Fi Jaringan Cepat", "VoIP HD 1080p WebRTC Aktif", true, 3500),
    CELLULAR_DATA("Data Seluler (4G/5G)", "VoIP Adaptif 720p WebRTC Aktif", true, 1600),
    GSM_FALLBACK_ACTIVE("Mode Bebas Kuota / GSM", "Telepon Operator & SMS Seluler Fallback", false, 0)
}

/**
 * Pemantau konektivitas jaringan sistem Android asli secara realtime.
 * Mendeteksi peralihan Wi-Fi HD, Data Seluler 5G, dan Mode Pesawat / Offline.
 */
class NetworkManager(private val context: Context? = null) {
    private val connectivityManager =
        context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _networkState = MutableStateFlow(determineCurrentNetwork())
    val networkState: StateFlow<ActiveNetworkState> = _networkState.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _networkState.value = determineCurrentNetwork()
        }

        override fun onLost(network: Network) {
            _networkState.value = determineCurrentNetwork()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            _networkState.value = determineCurrentNetwork()
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            // Ditangani secara aman jika permission background dibatasi
        }
    }

    fun determineCurrentNetwork(): ActiveNetworkState {
        val cm = connectivityManager ?: return ActiveNetworkState.GSM_FALLBACK_ACTIVE
        val activeNetwork = cm.activeNetwork ?: return ActiveNetworkState.GSM_FALLBACK_ACTIVE
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return ActiveNetworkState.GSM_FALLBACK_ACTIVE
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        if (!hasInternet) {
            return ActiveNetworkState.GSM_FALLBACK_ACTIVE
        }

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ActiveNetworkState.WIFI_HD
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ActiveNetworkState.CELLULAR_DATA
            else -> ActiveNetworkState.WIFI_HD
        }
    }
}
