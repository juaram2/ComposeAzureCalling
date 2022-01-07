package com.example.composeazurecalling.model

data class UserInfo (
    var id: String?,
    var email: String?,
    var verified_email: Boolean?,
    var name: String?,
    var given_name: String?,
    var family_name: String?,
    var picture: String?,
    var locale: String?,
)
