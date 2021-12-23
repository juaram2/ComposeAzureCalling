package com.example.composeazurecalling

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.example.composeazurecalling.helper.AudioSessionManager
import com.example.composeazurecalling.model.JoinCallType
import com.example.composeazurecalling.ui.activity.CallSetupActivity
import com.example.composeazurecalling.ui.theme.ComposeAzureCallingTheme
import com.example.composeazurecalling.ui.view.Call
import com.example.composeazurecalling.utils.ActivityLifecycleCallbacks
import com.example.composeazurecalling.utils.PrefUtil
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
    private val lifecycleCallbacks = ActivityLifecycleCallbacks()

    private val communicationCallingVM by viewModels<CommunicationCallingViewModel>()

    private var audioSessionManager: AudioSessionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PrefUtil.init(this)
        getAllPermissions()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            registerActivityLifecycleCallbacks(lifecycleCallbacks)
        }

        communicationCallingVM.setupCalling(this.applicationContext)
        createAudioSessionManager()

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

    private fun createAudioSessionManager() {
        this.audioSessionManager = AudioSessionManager(
            applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
        )
    }
}

@Composable
fun Main() {
    val context = LocalContext.current

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = {
                context.startActivity(Intent(context, CallSetupActivity::class.java))
//                val intent = Intent(context, CallSetupActivity::class.java)
//                intent.putExtra("callType", JoinCallType.GROUP_CALL)
//                intent.putExtra("joinId", "29858349-bbd5-4c0d-067e-08d9c5d76708")
//                callLauncher.launch(intent)
            },
            modifier = Modifier.align(alignment = Alignment.CenterVertically)
        ) {
            Text("Call")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ComposeAzureCallingTheme {
        Main()
    }
}