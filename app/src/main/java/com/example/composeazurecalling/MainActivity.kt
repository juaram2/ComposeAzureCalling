package com.example.composeazurecalling

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.core.app.ActivityCompat
import com.example.composeazurecalling.ui.theme.ComposeAzureCallingTheme
import com.example.composeazurecalling.ui.view.Call
import com.example.composeazurecalling.viewmodel.AuthenticationViewModel
import com.example.composeazurecalling.viewmodel.CommunicationCallingViewModel
import java.util.*

class MainActivity : ComponentActivity() {
    private val allPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
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
        communicationCallingVM.setupCalling()
        CloudHospitalApp.instance?.createAudioSessionManager()

        setContent {
            ComposeAzureCallingTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Call()
                }
            }
        }
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