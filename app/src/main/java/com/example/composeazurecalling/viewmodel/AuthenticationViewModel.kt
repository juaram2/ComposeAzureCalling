package com.example.composeazurecalling.viewmodel

import CloudHospitalApi.infrastructure.Serializer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.composeazurecalling.model.GrantValidationResult
import com.example.composeazurecalling.model.IdentityToken
import com.example.composeazurecalling.service.ApiClients
import com.example.composeazurecalling.service.AuthsApi
import com.example.composeazurecalling.utils.Constants
import com.example.composeazurecalling.utils.PrefUtil
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody

class AuthenticationViewModel : ViewModel() {
    private val authsApi = ApiClients.identityApiClient.createService(AuthsApi::class.java)

    fun onClickSignin() {
        val actionName = "signInWithEmail"
//        _loading.value = true

        viewModelScope.launch(Dispatchers.Main) {
            val response = authsApi.signinWithEmail(
                Constants.clientId(),
                Constants.clientSecret,
                Constants.scope,
                "password",
                "aaadkfka@naver.com",
                "Ydkfka42!"
            )
            Log.e("Debug", "response : $response")
            try {
//                _loading.value = false
                Log.e("Debug", "responseTry : $response")
                if (response.isSuccessful) {
                    response.body()?.let {
                        PrefUtil.cacheIdentityToken(
                            IdentityToken(
                                it.access_token,
                                it.expires_in,
                                it.token_type,
                                it.refresh_token,
                                it.scope
                            )
                        )
                        Log.e("Debug", "IdentityToken : "+response.body().toString())
                        Log.d("debug", "$actionName: ${response.body()}")
//                        _signedIn.postValue(true)
                    }
                } else {
                    val grantValidationResult: GrantValidationResult? = decodeGrantValidationResult(
                        actionName,
                        response.errorBody()
                    )
                    Log.e("error", grantValidationResult?.error_description.toString())
//                    _signInError.postValue(grantValidationResult)
                }
            } catch (e: Exception) {
                Log.e("Exception", e.message!! )
                Log.e("Exception", e.localizedMessage )
                // TODO: Handle connection orcommunication error, report to AppCenter
//                _loading.value = false
                Log.e("error", "$actionName: ${e.localizedMessage}")
            }
        }
    }

    private fun decodeGrantValidationResult(actionName: String, data: ResponseBody?): GrantValidationResult? {
        data?.let {
            val jsonAdapter: JsonAdapter<GrantValidationResult> = Serializer.moshi.adapter(
                GrantValidationResult::class.java).lenient()
            val grantValidationResult: GrantValidationResult? = jsonAdapter.lenient().fromJson(it.source())

            Log.e("error", "$actionName errors: $grantValidationResult")
            val properties: MutableMap<String, String> = HashMap()
            properties["ErrorType"] = "GrantValidation"
            properties["ErrorBody"] = grantValidationResult.toString()
//            Analytics.trackEvent(actionName, properties)
            return grantValidationResult
        }
        return null
    }
}