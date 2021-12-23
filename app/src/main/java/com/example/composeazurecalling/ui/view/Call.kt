package com.example.composeazurecalling.ui.view

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import com.azure.android.communication.calling.CreateViewOptions
import com.azure.android.communication.calling.ScalingMode
import com.azure.android.communication.calling.VideoStreamRenderer
import com.azure.android.communication.calling.VideoStreamRendererView
import com.example.composeazurecalling.model.JoinCallConfig
import com.example.composeazurecalling.model.JoinCallType
import com.example.composeazurecalling.ui.activity.CallScreenActivity
import com.example.composeazurecalling.utils.ifLet
import com.example.composeazurecalling.viewmodel.CallSetupViewModel
import com.example.composeazurecalling.viewmodel.CommunicationCallingViewModel
import com.google.gson.Gson
import kotlinx.android.parcel.Parcelize
import java.util.*

@Composable
fun Call() {
    val scrollState = rememberScrollState()
//    val authorizationVM: AuthorizationVM = viewModel()
//    val loading = authorizationVM.loading.observeAsState().value
//    val profile = authorizationVM.profile.observeAsState().value

//    LaunchedEffect(key1 = Unit) {
//        authorizationVM.getUserProfile()
//    }
//
//    if (loading == true) {
//        LoadingBar()
//    } else {
        Column(Modifier.verticalScroll(scrollState)) {
            CallScreen()
        }
//    }
}

@Composable
fun CallScreen() {
    val context = LocalContext.current

    val callingVM: CommunicationCallingViewModel = viewModel()
    val callSetupVM: CallSetupViewModel = viewModel()
    var rendererView: VideoStreamRenderer? = null
    var previewVideo: VideoStreamRendererView? = null

//    if (profile != null) {
//        callSetupVM.setDisplayName(profile.fullname)
//    }

    val displayName = callSetupVM.displayName.observeAsState().value
//    val isMicChecked = viewModel.isMicChecked.observeAsState().value
    val isVideoCheck = callSetupVM.isVideoChecked.observeAsState().value
    val startCall = callSetupVM.startCall.observeAsState().value
    var isVideoChecked by remember { mutableStateOf(false) }
    var isMicChecked by remember { mutableStateOf(false) }
    var isVolumeChecked by remember { mutableStateOf(false) }

    val nameState = remember { mutableStateOf(TextFieldValue()) }

    val groupCallLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        it.data?.let { data ->
            val joinCallConfig = data.getSerializableExtra("joinCallConfig") as JoinCallConfig
            Log.d("debug", "joinCallConfig: ${joinCallConfig.displayName}")
            Log.d("debug", "joinCallConfig: ${joinCallConfig.joinId}")
            Log.d("debug", "joinCallConfig: ${joinCallConfig.isCameraOn}")
            Log.d("debug", "joinCallConfig: ${joinCallConfig.isMicrophoneMuted}")
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        IconButton(onClick = { isVideoChecked = !isVideoChecked }) {
            if (isVideoChecked) {
                Icon(imageVector = Icons.Default.Videocam, contentDescription = "video on")
            } else {
                Icon(imageVector = Icons.Default.VideocamOff, contentDescription = "video off")
            }
        }
        IconButton(onClick = { isMicChecked = !isMicChecked }) {
            if (isMicChecked) {
                Icon(imageVector = Icons.Default.Mic, contentDescription = "mic on")
            } else {
                Icon(imageVector = Icons.Default.MicOff, contentDescription = "mic off")
            }
        }
        IconButton(onClick = { isVolumeChecked = !isVolumeChecked }) {
            if (isVolumeChecked) {
                Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "volume on")
            } else {
                Icon(imageVector = Icons.Default.VolumeOff, contentDescription = "volume off")
            }
        }
    }

    AndroidView(factory = { context ->
        //Here you can construct your View
        LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 800)
        }
    }, update = { layout ->
        if (isVideoChecked) {
            Log.d("debug", "isVideoChecked true")
            val localVideoStream = callingVM.getLocalVideoStream(context)
            rendererView = VideoStreamRenderer(localVideoStream, context)
            rendererView?.let {
                previewVideo = it.createView(CreateViewOptions(ScalingMode.CROP))
                layout.addView(previewVideo)
            }
        } else {
            Log.d("debug", "isVideoChecked false")
            rendererView?.let {
                it.dispose()
                layout.removeView(previewVideo)
            }
        }
    })

    Column(
        Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        TextField(
            value = nameState.value,
            onValueChange = { nameState.value = it },
            label = { Text("Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp))

        Button(onClick = {
            Log.d("debug", "start Call")
            if (rendererView != null) {
                rendererView!!.dispose()
            }

            ifLet(isMicChecked, isVideoChecked) { (isMicChecked, isVideoChecked) ->
                val joinCallConfig = JoinCallConfig(
                    UUID.fromString("671cc6c0-63e2-450d-067f-08d9c5d76708"),
                    !isMicChecked,
                    isVideoChecked,
                    displayName ?: "aram",
                    JoinCallType.GROUP_CALL)
                val intent = Intent(context, CallScreenActivity::class.java)
                intent.putExtra("joinCallConfig", joinCallConfig)
                groupCallLauncher.launch(intent)
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Call")
        }
    }
}

@Parcelize
data class CallNavType(
    val joinId: UUID,
    val isMicChecked: Boolean,
    val isVideoChecked: Boolean,
    val displayName: String,
    val callType: JoinCallType
) : Parcelable

class CallParamType : NavType<CallNavType>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): CallNavType? {
        return bundle.getParcelable(key)
    }

    override fun parseValue(value: String): CallNavType {
        return Gson().fromJson(value, CallNavType::class.java)
    }

    override fun put(bundle: Bundle, key: String, value: CallNavType) {
        bundle.putParcelable(key, value)
    }
}