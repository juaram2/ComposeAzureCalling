package com.example.composeazurecalling.service

import com.example.composeazurecalling.model.IdentityToken
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST


interface AuthsApi {
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @FormUrlEncoded
    @POST("/connect/token")
    suspend fun signinWithEmail(@Field("client_id") client_id: String, @Field("client_secret") client_secret: String, @Field("scope") scope: String,
                                @Field("grant_type") grant_type: String, @Field("username") username: String, @Field("password") password: String,): Response<IdentityToken>

    @Headers("Content-Type: application/x-www-form-urlencoded")
    @FormUrlEncoded
    @POST("/connect/token")
    suspend fun signInExternal(@Field("client_id") client_id: String, @Field("client_secret") client_secret: String, @Field("scope") scope: String, @Field("sub") sub: String,
                               @Field("grant_type") grant_type: String, @Field("provider") provider: String, @Field("external_token") external_token: String, @Field("email") email: String): Response<IdentityToken>

    @Headers("Content-Type: application/x-www-form-urlencoded")
    @FormUrlEncoded
    @POST("/connect/revocation")
    suspend fun signOut(@Field("client_id") client_id: String, @Field("client_secret") client_secret: String, @Field("token") token: String, @Field("token_type_hint") token_type_hint: String,): Response<ResponseBody>

    @Headers("Content-Type: application/x-www-form-urlencoded")
    @FormUrlEncoded
    @POST("/connect/token")
    fun refreshToken(@Field("client_id") client_id: String, @Field("client_secret") client_secret: String, @Field("grant_type") grant_type: String, @Field("refresh_token") refresh_token: String): Call<IdentityToken>
}