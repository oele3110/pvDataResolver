package com.oele3110.pvdataresolver.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class LoginRequest(val username: String, val password: String)

@Serializable
private data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String
)

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "AuthRepository"
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient()

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    init {
        // Initialize from persisted token — triggers lazy prefs init
        _isLoggedIn.value = getToken() != null
        Log.d(tag, "init — isLoggedIn=${_isLoggedIn.value}")
    }

    fun getToken(): String? = prefs.getString("access_token", null)

    fun logout() {
        Log.i(tag, "logout() — clearing stored token")
        prefs.edit().remove("access_token").apply()
        _isLoggedIn.value = false
    }

    suspend fun login(username: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        Log.i(tag, "login() — attempting login for user '$username'")
        try {
            val body = json.encodeToString(LoginRequest(username, password))
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://pv.dennislampert.de/api/login")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            Log.d(tag, "login() — HTTP ${response.code}")
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Leere Server-Antwort"))
                val loginResponse = json.decodeFromString<LoginResponse>(responseBody)
                prefs.edit().putString("access_token", loginResponse.accessToken).apply()
                _isLoggedIn.value = true
                Log.i(tag, "✅ Login successful — token stored")
                Result.success(Unit)
            } else {
                Log.w(tag, "❌ Login rejected — HTTP ${response.code}")
                Result.failure(Exception("Login fehlgeschlagen (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ Login network error: ${e.message}")
            Result.failure(Exception("Verbindungsfehler: ${e.message}"))
        }
    }
}
