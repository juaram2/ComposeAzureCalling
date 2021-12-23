package com.example.composeazurecalling.helper

import android.media.AudioManager

class AudioSessionManager(private val audioManager: AudioManager) {

    val isSpeakerphoneOn: Boolean
        get() = audioManager.isSpeakerphoneOn

    private fun setSpeakerPhoneStatus(status: Boolean) {
        audioManager.isSpeakerphoneOn = status
    }
}