package com.example.composeazurecalling.service

import CloudHospitalApi.apis.CommunicationsApi
import CloudHospitalApi.infrastructure.Serializer
import android.util.Log
import com.azure.android.communication.common.CommunicationTokenCredential
import com.cloudhospital.model.exception.RestException
import com.google.gson.annotations.JsonAdapter
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.CompletableFuture

interface TokenListener {
    fun onTokenRetreived(communicationTokenCredential: CommunicationTokenCredential)
}

class TokenService {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val communicationsApi = ApiClients.apiClient.createService(CommunicationsApi::class.java)

    fun getCommunicationTokenAsync(): CompletableFuture<String> {
        val tokenCompletableFuture = CompletableFuture<String>()

        val actionName = "apiV1CommunicationsGet"
        Log.d("debug", "$actionName started")
        coroutineScope.launch {
            val response = communicationsApi.apiV1CommunicationsGet()
            try {
                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.d("debug", "$actionName: ${response.body()}")
                        tokenCompletableFuture.complete(it.token)
                    }
                } else {
                    val jsonAdapter: JsonAdapter<RestException> = Serializer.moshi.adapter(
                        RestException::class.java
                    )
                    val restException: RestException? = jsonAdapter.lenient().fromJson(
                        response.errorBody()?.source()
                    )

                    Log.e("error", "$actionName errors: $restException")
                }
            } catch (e: Exception) {
                Log.d("debug", "$actionName failure: ${e.message}")
            }
        }

        return tokenCompletableFuture
    }
}