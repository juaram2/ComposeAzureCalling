package com.example.composeazurecalling.utils

import android.os.Bundle
import androidx.navigation.NavType
import com.example.composeazurecalling.model.JoinCallConfig
import com.example.composeazurecalling.model.JoinCallType
import com.google.gson.Gson
import java.util.*

class CallNavType : NavType<JoinCallType>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): JoinCallType? {
        return bundle.getSerializable(key) as JoinCallType?
    }

    override fun parseValue(value: String): JoinCallType {
        return Gson().fromJson(value, JoinCallType::class.java)
    }

    override fun put(bundle: Bundle, key: String, value: JoinCallType) {
        bundle.putSerializable(key, value)
    }
}

class CallIdNavType : NavType<UUID>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): UUID? {
        return bundle.getSerializable(key) as UUID?
    }

    override fun parseValue(value: String): UUID {
        return Gson().fromJson(value, UUID::class.java)
    }

    override fun put(bundle: Bundle, key: String, value: UUID) {
        bundle.putSerializable(key, value)
    }
}

//@Parcelize
//data class JoinCallCfg(val joinId: UUID,
//                  val isMicrophoneMuted: Boolean,
//                  val isCameraOn: Boolean,
//                  val displayName: String,
//                  val callType: JoinCallType) : Parcelable

class CallConfigNavType : NavType<JoinCallConfig>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): JoinCallConfig? {
        return bundle.getSerializable(key) as JoinCallConfig?
    }

    override fun parseValue(value: String): JoinCallConfig {
        return Gson().fromJson(value, JoinCallConfig::class.java)
    }

    override fun put(bundle: Bundle, key: String, value: JoinCallConfig) {
        bundle.putSerializable(key, value)
    }
}