package com.oele3110.pvdataresolver.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import com.oele3110.pvdataresolver.data.auth.AuthRepository
import com.oele3110.pvdataresolver.websocket.IWebsocket
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val webSocket: IWebsocket,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val tag = "DashboardViewModel"

    val energyData = webSocket.data
    val connectionStatus = webSocket.connectionStatus
    val isLoggedIn = authRepository.isLoggedIn

    init {
        Log.i(tag, "created — starting WebSocket")
        webSocket.connect()
    }

    fun connectIfNeeded() {
        if (authRepository.isLoggedIn.value && !connectionStatus.value) {
            Log.i(tag, "connectIfNeeded() — reconnecting after returning to dashboard")
            webSocket.connect()
        }
    }

    fun logout() {
        Log.i(tag, "logout()")
        webSocket.disconnect()
        authRepository.logout()
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(tag, "cleared — stopping WebSocket")
        webSocket.disconnect()
    }
}
