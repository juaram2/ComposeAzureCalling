package com.example.composeazurecalling.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RestException(
    var title: String,
    var status: Int,
    var instance: String,
    var traceId: String,
    var errors: Errors
)
