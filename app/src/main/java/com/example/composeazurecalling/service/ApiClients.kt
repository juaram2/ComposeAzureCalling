package com.example.composeazurecalling.service

import CloudHospitalApi.infrastructure.ApiClient
import android.util.Log
import com.example.composeazurecalling.BuildConfig
import com.example.composeazurecalling.utils.AuthInterceptor
import com.example.composeazurecalling.utils.Constants
import com.example.composeazurecalling.utils.NoConnectionInterceptor
import com.example.composeazurecalling.utils.TokenAuthenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object ApiClients {
    var apiClient: ApiClient
    var identityApiClient: ApiClient

    val okHttpClientBuilder = OkHttpClient()
            .newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .authenticator(TokenAuthenticator())
            .addInterceptor(NoConnectionInterceptor())
            .addInterceptor(HttpLoggingInterceptor { message -> Log.d("debug", message) }.apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else { HttpLoggingInterceptor.Level.NONE }
            })

    init {
        Log.d("debug", "Singletone class invoked.")

        identityApiClient = ApiClient(baseUrl = Constants.identityServer(), okHttpClientBuilder = okHttpClientBuilder)
        identityApiClient.addAuthorization("oauth", AuthInterceptor())
        apiClient = ApiClient(baseUrl = Constants.apiServer(), okHttpClientBuilder = okHttpClientBuilder)
    }
}