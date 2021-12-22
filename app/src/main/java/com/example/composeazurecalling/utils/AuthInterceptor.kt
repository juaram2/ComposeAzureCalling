package com.example.composeazurecalling.utils

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response


class AuthInterceptor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        val accessToken = PrefUtil.getCachedIdentityToken()?.access_token
        val tokenType = PrefUtil.getCachedIdentityToken()?.token_type

        // If token has been saved, add it to the request
        accessToken.let { accessToken ->
            tokenType.let { tokenType ->
                val authHeaderValue = "$tokenType $accessToken"
                requestBuilder.addHeader("Authorization", authHeaderValue)
                Log.d("intercept", "addHeader $authHeaderValue")
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}