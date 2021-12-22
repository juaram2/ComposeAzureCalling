package com.example.composeazurecalling.utils

import CloudHospitalApi.models.SendBirdUserViewModel
import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.example.composeazurecalling.BuildConfig
import com.example.composeazurecalling.model.IdentityToken
import com.pddstudio.preferences.encrypted.EncryptedPreferences
import java.time.LocalDateTime
import java.time.chrono.ChronoLocalDateTime

object PrefUtil {

    var ACCESS_TOKEN = "ACCESS_TOKEN-${BuildConfig.FLAVOR}"
    var REFRESH_TOKEN = "REFRESH_TOKEN-${BuildConfig.FLAVOR}"
    var TOKEN_TYPE = "TOKEN_TYPE-${BuildConfig.FLAVOR}"
    var EXPIRES_AT = "EXPIRES_AT-${BuildConfig.FLAVOR}"
    var SENDBIRD_ACCESS_TOKEN = "SENDBIRD_ACCESS_TOKEN-${BuildConfig.FLAVOR}"
    var SENDBIRD_NICKNAME = "SENDBIRD_NICKNAME-${BuildConfig.FLAVOR}"
    var SENDBIRD_USERID = "SENDBIRD_USERID-${BuildConfig.FLAVOR}"
    var SENDBIRD_PROFILE = "SENDBIRD_PROFILE-${BuildConfig.FLAVOR}"

    private lateinit var prefs: EncryptedPreferences

    fun init(context: Context) {
        prefs = EncryptedPreferences.Builder(context).withEncryptionPassword("cloudhospital.${BuildConfig.FLAVOR}").build()
    }

    fun cacheIdentityToken(identityToken: IdentityToken) {
        access_token = identityToken.access_token
        refresh_token = identityToken.refresh_token
        token_type = identityToken.token_type
        expires_at = LocalDateTime.now().plusSeconds(identityToken.expires_in.toLong()).toString()
    }

    fun getCachedIdentityToken(): IdentityToken? {
        var identityToken = IdentityToken(
            access_token = access_token,
            token_type = token_type,
            refresh_token =  refresh_token
        )

        if (identityToken.access_token != "") {
            return identityToken
        } else {
            return null
        }
    }

    fun getCachedAccessToken(): String {
        return access_token
    }

    fun checkIfTokenExpired(): Boolean {
        Log.d("debug","expires_at :: ${expires_at}")
        return if(TextUtils.isEmpty(expires_at)) true
        else LocalDateTime.parse(expires_at).isBefore(ChronoLocalDateTime.from((LocalDateTime.now())))
        //return LocalDate.parse(expires_at).isAfter(ChronoLocalDate.from(now()))
    }

    fun cacheSendbirdToken(sendBidUserViewModel: SendBirdUserViewModel) {
        sendbird_access_token = sendBidUserViewModel.accessToken!!
        sendbird_nickname = sendBidUserViewModel.nickname!!
        sendbird_userid = sendBidUserViewModel.userId!!
        sendbird_profile = sendBidUserViewModel.profileUrl!!
    }

    fun getCachedSendBirdUserViewModel(): SendBirdUserViewModel {
        var sendBirdUserViewModel = SendBirdUserViewModel(
            accessToken = sendbird_access_token,
            nickname = sendbird_nickname,
            userId = sendbird_userid,
            profileUrl = sendbird_profile
        )
        return sendBirdUserViewModel
    }

    fun clearIdentityToken() {
        access_token = ""
        refresh_token = ""
        token_type = ""
        expires_at = ""
        sendbird_access_token = ""
    }

    private var access_token: String
        get() = prefs.getString(ACCESS_TOKEN, "")
        set(value) = prefs.edit().putString(ACCESS_TOKEN, value).apply()

    private var refresh_token: String
        get() = prefs.getString(REFRESH_TOKEN, "")
        set(value) = prefs.edit().putString(REFRESH_TOKEN, value).apply()

    private var token_type: String
        get() = prefs.getString(TOKEN_TYPE, "")
        set(value) = prefs.edit().putString(TOKEN_TYPE, value).apply()

    private var expires_at: String
        get() = prefs.getString(EXPIRES_AT, "")
        set(value) = prefs.edit().putString(EXPIRES_AT, value).apply()

    private var sendbird_access_token: String
        get() = prefs.getString(SENDBIRD_ACCESS_TOKEN, "")
        set(value) = prefs.edit().putString(SENDBIRD_ACCESS_TOKEN, value).apply()

    private var sendbird_nickname: String
        get() = prefs.getString(SENDBIRD_NICKNAME, "")
        set(value) = prefs.edit().putString(SENDBIRD_NICKNAME, value).apply()

    private var sendbird_userid: String
        get() = prefs.getString(SENDBIRD_USERID, "")
        set(value) = prefs.edit().putString(SENDBIRD_USERID, value).apply()

    private var sendbird_profile: String
        get() = prefs.getString(SENDBIRD_PROFILE, "")
        set(value) = prefs.edit().putString(SENDBIRD_PROFILE, value).apply()

}