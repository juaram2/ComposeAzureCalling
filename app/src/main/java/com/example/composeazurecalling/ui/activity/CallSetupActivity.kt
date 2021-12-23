package com.example.composeazurecalling.ui.activity

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.composeazurecalling.helper.AudioSessionManager
import com.example.composeazurecalling.model.JoinCallType
import com.example.composeazurecalling.ui.theme.ComposeAzureCallingTheme
import com.example.composeazurecalling.ui.view.Call
import com.example.composeazurecalling.viewmodel.CallSetupViewModel
import com.example.composeazurecalling.viewmodel.CommunicationCallingViewModel
import java.util.*

class CallSetupActivity : AppCompatActivity() {
    private val callVM by viewModels<CallSetupViewModel>()
//    private val authorizationVM by viewModels<AuthorizationVM>()
    private val communicationCallingVM by viewModels<CommunicationCallingViewModel>()

    private var audioSessionManager: AudioSessionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        communicationCallingVM.setupCalling(this.applicationContext)
        createAudioSessionManager()

        val callType = intent.getSerializableExtra("callType") as JoinCallType
        val joinId = intent.getSerializableExtra("joinId") as UUID

        setContent {
            ComposeAzureCallingTheme() {
                joinId?.let {
                    Call(callType = callType, joinId = it)
                }
            }
        }
    }

    private fun createAudioSessionManager() {
        this.audioSessionManager = AudioSessionManager(
            applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
        )
    }
}