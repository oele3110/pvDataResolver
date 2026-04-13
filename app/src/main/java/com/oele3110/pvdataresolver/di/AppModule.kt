package com.oele3110.pvdataresolver.di

import com.oele3110.pvdataresolver.data.api.PvApiService
import com.oele3110.pvdataresolver.data.auth.TokenInterceptor
import com.oele3110.pvdataresolver.websocket.IWebsocket
import com.oele3110.pvdataresolver.websocket.WebSocketManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenInterceptor: TokenInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(tokenInterceptor)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://pv.dennislampert.de/api/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json; charset=UTF8".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun providePvApiService(retrofit: Retrofit): PvApiService =
        retrofit.create(PvApiService::class.java)

    @Provides
    @Singleton
    fun provideWebSocket(webSocketManager: WebSocketManager): IWebsocket = webSocketManager
}
