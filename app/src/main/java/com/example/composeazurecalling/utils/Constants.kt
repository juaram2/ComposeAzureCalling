package com.example.composeazurecalling.utils

class Constants {
    companion object {
//        var clientId_dev = "com.cloudhospital.int"
//        var clientId_stg = "com.cloudhospital.stg"
//        var clientId_prd = "com.cloudhospital.app"
//
//        var clientSecret = "CloudHospitalSecret"
//        var api_dev = "https://api-int.icloudhospital.com"
//        var api_stg = "https://api-stg.icloudhospital.com"
//        var api_prd = "https://api.icloudhospital.com"
//
//        var identity_dev = "https://identity-int.icloudhospital.com"
//        var identity_stg = "https://identity-stg.icloudhospital.com"
//        var identity_prd = "https://identity.icloudhospital.com"
//
//        var scope = "openid email profile roles CloudHospital_api IdentityServerApi offline_access"
//
//        var sendBirdAppId_dev = "D39AC198-F710-498F-8D68-46793C09293E"
//        var sendBirdAppId_stg = "6BFD4F62-DB98-4954-814C-C665501854A8"
//        var sendBirdAppId_prd = "164B502D-D1AE-4243-AA99-BBC1FEC2BA4A"
//
//        val facebookAppID = "628222671314782"
//        val googleClientId = "458555718648-1b9q73s8vi8anjp1j51jnh2n24c21rpd.apps.googleusercontent.com"
//
//
//        val appleClientId = "com.cloudhospital.identity"
//
//        val appleScope = "name%20email"
//
//        val appleAuthUrl = "https://appleid.apple.com/auth/authorize"
//        val appleTokenUrl = "https://appleid.apple.com/auth/token"
//
//        val appleRedirectUri_dev = "https://int.icloudhospital.com"
//        val appleRedirectUri_stg = "https://stg.icloudhospital.com"
//        val appleRedirectUri_prd = "https://icloudhospital.com"
//
//        var appCenter_dev = "14848225-386e-4791-86b0-8ace6fc8829b"
//        var appCenter_stg = ""
//        var appCenter_prd = "3f11e027-aa2a-4a2b-93d6-cc1a612e07fd"
//
//        fun identityServer():String {
//            when (BuildConfig.FLAVOR) {
//                "dev" -> return identity_dev
//                "stg" -> return identity_stg
//                "prd" -> return identity_prd
//            }
//            return identity_dev
//        }
//
//        fun apiServer():String {
//            when (BuildConfig.FLAVOR) {
//                "dev" -> return api_dev
//                "stg" -> return api_stg
//                "prd" -> return api_prd
//            }
//            return api_dev
//        }
//
//        fun clientId():String {
//            when (BuildConfig.FLAVOR) {
//                "dev" -> return clientId_dev
//                "stg" -> return clientId_stg
//                "prd" -> return clientId_prd
//            }
//            return clientId_dev
//        }
//
//        fun getSendBirdAppId(): String {
//            when (BuildConfig.FLAVOR) {
//                "dev" -> return sendBirdAppId_dev
//                "stg" -> return sendBirdAppId_stg
//                "prd" -> return sendBirdAppId_prd
//            }
//            return sendBirdAppId_dev
//        }
//
//        fun appleRedirectUri():String {
//            when (BuildConfig.FLAVOR) {
//                "dev" -> return appleRedirectUri_dev
//                "stg" -> return appleRedirectUri_stg
//                "prd" -> return appleRedirectUri_prd
//            }
//            return appleRedirectUri_dev
//        }
//
//        fun appCenterKey():String {
//            when (BuildConfig.FLAVOR) {
//                "dev" -> return appCenter_dev
//                "stg" -> return appCenter_stg
//                "prd" -> return appCenter_prd
//            }
//            return appCenter_dev
//        }

        /* The constant values for the limit number of participant views */
        const val DISPLAYED_REMOTE_PARTICIPANT_SIZE_LIMIT = 3;

        /* The key for the meeting name */
        const val JOIN_ID = "JOIN_ID";

        /* The key for the Join Call Config */
        const val JOIN_CALL_CONFIG = "JOIN_CALL_CONFIG";

        /* The key for the Join Call Type */
        const val CALL_TYPE = "CALL_TYPE";

    }
}
