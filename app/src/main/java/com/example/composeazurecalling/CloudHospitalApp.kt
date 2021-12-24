package com.example.composeazurecalling

import android.app.Application
import android.content.Context
import android.media.AudioManager
import com.example.composeazurecalling.helper.AudioSessionManager
import com.example.composeazurecalling.utils.PrefUtil

class CloudHospitalApp : Application() {

    private val lifecycleCallbacks = CHActivityLifecycleCallbacks()

    private var audioSessionManager: AudioSessionManager? = null

    override fun onCreate() {
        instance = this
        super.onCreate()

        PrefUtil.init(this)

        registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }


    fun createAudioSessionManager() {
        this.audioSessionManager = AudioSessionManager(
            applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
        )
    }
    fun getAudioSessionManager(): AudioSessionManager? {
        return audioSessionManager
    }


    companion object {
        var instance: CloudHospitalApp? = null
            private set
        val context: Context?
            get() = instance
   }
}