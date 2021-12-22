package com.example.composeazurecalling.utils

import android.util.Log
import com.example.composeazurecalling.model.IdentityToken
import com.example.composeazurecalling.service.ApiClients
import com.example.composeazurecalling.service.AuthsApi
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator: Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {

        // This is a synchronous call
        if (response.code == 401) {
            val refreshToken = PrefUtil.getCachedIdentityToken()?.refresh_token

            if (refreshToken != null) {
                Log.d("debug", "start refresh token")

                val requestBuilder = response.request.newBuilder()

                val updatedHeader = getUpdatedHeader(token = refreshToken)
                Log.d("debug","updated header: $updatedHeader")

                return updatedHeader?.let {
                    requestBuilder.header("Authorization", it).build()
                }
            }
        }

        return null
    }

    private fun getUpdatedHeader(token: String): String? {
        val authsApi = ApiClients.identityApiClient.createService(AuthsApi::class.java)
        var response = authsApi.refreshToken(Constants.clientId(), Constants.clientSecret, "refresh_token", token).execute()

        response.body()?.let {
            PrefUtil.cacheIdentityToken(IdentityToken(it.access_token, it.expires_in, it.token_type, it.refresh_token, it.scope))

            val accessToken = PrefUtil.getCachedIdentityToken()?.access_token
            val tokenType = PrefUtil.getCachedIdentityToken()?.token_type

            accessToken.let { accessToken ->
                tokenType.let { tokenType ->
                    val trimmedToken = accessToken?.replace("\n", "")?.replace("\r", "")

                    return "$tokenType $trimmedToken"
                }
            }
        }
        return null
    }
}