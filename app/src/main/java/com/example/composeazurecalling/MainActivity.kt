package com.example.composeazurecalling

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.core.app.ActivityCompat
import com.azure.android.communication.common.CommunicationTokenCredential
import com.azure.android.communication.common.CommunicationTokenRefreshOptions
import com.azure.android.communication.ui.CallComposite
import com.azure.android.communication.ui.CallCompositeBuilder
import com.azure.android.communication.ui.GroupCallOptions
import com.example.composeazurecalling.ui.theme.ComposeAzureCallingTheme
import com.example.composeazurecalling.viewmodel.AuthenticationViewModel
import com.example.composeazurecalling.viewmodel.CommunicationCallingViewModel
import java.util.*

class MainActivity : ComponentActivity() {
    private val allPermissions = arrayOf(
//        Manifest.permission.RECORD_AUDIO,
//        Manifest.permission.CAMERA,
//        Manifest.permission.CALL_PHONE,
//        Manifest.permission.MODIFY_AUDIO_SETTINGS,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.READ_PHONE_STATE
    )

    private val communicationCallingVM by viewModels<CommunicationCallingViewModel>()
    private val authenticationVM by viewModels<AuthenticationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getAllPermissions()

        authenticationVM.onClickSignin()
//        communicationCallingVM.setupCalling()
//        CloudHospitalApp.instance?.createAudioSessionManager()

        setContent {
            ComposeAzureCallingTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
//                    Call()
                    Button(onClick = { startCallComposite() }) {
                        Text(text = "Call")
                    }
                }
            }
        }
    }

    private fun startCallComposite() {
        val communicationTokenRefreshOptions = CommunicationTokenRefreshOptions({ fetchToken() }, true)
        val communicationTokenCredential = CommunicationTokenCredential(communicationTokenRefreshOptions)

        val options = GroupCallOptions(
            this,
            communicationTokenCredential,
            UUID.fromString("5cb04a0a-c28a-416e-b002-08d9e99754fb"),
            "DISPLAY_NAME",
        )

        val callComposite: CallComposite = CallCompositeBuilder().build()
        callComposite.launch(options)
    }

    private fun fetchToken(): String? {
        return "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEwNCIsIng1dCI6IlJDM0NPdTV6UENIWlVKaVBlclM0SUl4Szh3ZyIsInR5cCI6IkpXVCJ9.eyJza3lwZWlkIjoiYWNzOjkxODU5N2JlLTNmMjktNDY4MC1hOTFhLWI0ZjVlNjc1OGExN18wMDAwMDAwZi0yZmM0LTdkMGItZWM4ZC0wODQ4MjIwMDIwNzgiLCJzY3AiOjE3OTIsImNzaSI6IjE2NDQyMTM2NzMiLCJleHAiOjE2NDQzMDAwNzMsImFjc1Njb3BlIjoidm9pcCxjaGF0IiwicmVzb3VyY2VJZCI6IjkxODU5N2JlLTNmMjktNDY4MC1hOTFhLWI0ZjVlNjc1OGExNyIsImlhdCI6MTY0NDIxMzY3M30.b3NdenV_Ml1CZuH0UTiMGyjUakZBusPiXmFrIjtIBCAxkv3uDGW1Al5bv4QlMvnYHsBW7MhGx7aEKgBcIq0OH6QN51TdDTy47Et2DwVxisSPq4kJja652Y5xPycE34Vlm7ocrr785LYwYiJWIuiEgDXvGwUS_HoMTpeebCzZd2HYf8BvNkRJMWOzpJNviseYB0OuJP9U_WnUR6zgWae8v1Kot3MuXUqmD2dhwURwa3eY4mL4WWoZ1HcoiKNEAC8HWJAwy1PBY7csGKMQirtXlO1GjvcxOWGe9uHc3cr1K96Mg3tJ01pEEh07Mc7-Xsd3AzV8SNMO22SAj6zYpUEbmQ"
    }

    /**
    * Request each required permission if the app doesn't already have it.
    */
    private fun getAllPermissions() {
        val permissionsToAskFor = ArrayList<String>()
        for (permission in allPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToAskFor.add(permission)
            }
        }
        if (permissionsToAskFor.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToAskFor.toTypedArray(), 1)
        }
    }
}