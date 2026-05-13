package com.oele3110.pvdataresolver.ui.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.oele3110.pvdataresolver.data.auth.AuthRepository
import com.oele3110.pvdataresolver.websocket.IWebsocket
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val webSocket: IWebsocket,
    private val authRepository: AuthRepository,
    @ApplicationContext context: Context
) : ViewModel() {

    private val tag = "DashboardViewModel"

    private val prefs = context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)

    val energyData = webSocket.data
    val connectionStatus = webSocket.connectionStatus
    val isLoggedIn = authRepository.isLoggedIn

    private val _showCarSoc = MutableStateFlow(prefs.getBoolean("show_car_soc", true))
    val showCarSoc = _showCarSoc.asStateFlow()

    init {
        Log.i(tag, "created — starting WebSocket")
        webSocket.connect()
    }

    fun toggleCarSoc() {
        val new = !_showCarSoc.value
        _showCarSoc.value = new
        prefs.edit().putBoolean("show_car_soc", new).apply()
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
