package com.example.composeazurecalling.model

import java.io.Serializable
import java.util.*

class JoinCallConfig(
    val joinId: UUID,
    val isMicrophoneMuted: Boolean,
    val isCameraOn: Boolean,
    val displayName: String,
    callType: JoinCallType
) : Serializable {
    private val callType: JoinCallType = callType
    fun getCallType(): JoinCallType {
        return callType
    }

}