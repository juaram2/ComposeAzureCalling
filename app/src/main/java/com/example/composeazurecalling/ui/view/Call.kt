package com.example.composeazurecalling.ui.view

import android.net.Uri
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.azure.android.communication.calling.CreateViewOptions
import com.azure.android.communication.calling.ScalingMode
import com.azure.android.communication.calling.VideoStreamRenderer
import com.azure.android.communication.calling.VideoStreamRendererView
import com.example.composeazurecalling.CloudHospitalApp.Companion.context
import com.example.composeazurecalling.model.JoinCallConfig
import com.example.composeazurecalling.model.JoinCallType
import com.example.composeazurecalling.utils.CallConfigNavType
import com.example.composeazurecalling.utils.ifLet
import com.example.composeazurecalling.viewmodel.CallScreenViewModel
import com.example.composeazurecalling.viewmodel.CallSetupViewModel
import com.example.composeazurecalling.viewmodel.CommunicationCallingViewModel
import com.google.gson.Gson
import java.util.*

@Composable
fun Call() {
    val navController = rememberNavController()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val callingViewModel: CommunicationCallingViewModel = viewModel()
    val groupCallVM: CallScreenViewModel = viewModel()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            Column(Modifier.verticalScroll(scrollState)) {
                CallScreen(navController, callingViewModel)
            }
        }

        composable(
            route = "groupCall/{joinCallConfig}",
            arguments = listOf(navArgument("joinCallConfig"){ type = CallConfigNavType() }
        )) {
            val joinCallConfig = requireNotNull(it.arguments).getSerializable("joinCallConfig") as JoinCallConfig
            GroupCall(joinCallConfig, callingViewModel, groupCallVM, ParticipantView(context))
        }
    }
}



@Composable
fun CallScreen(navController: NavHostController, callingVM: CommunicationCallingViewModel) {
    val context = LocalContext.current

    val callSetupVM: CallSetupViewModel = viewModel()
    var rendererView: VideoStreamRenderer? = null
    var previewVideo: VideoStreamRendererView? = null

    val displayName = callSetupVM.displayName.observeAsState().value
    var isVideoChecked by remember { mutableStateOf(false) }
    var isMicChecked by remember { mutableStateOf(false) }
    var isVolumeChecked by remember { mutableStateOf(false) }

    val nameState = remember { mutableStateOf(TextFieldValue()) }

    DisposableEffect(Unit) {
        onDispose {
            if (rendererView != null) {
                rendererView!!.dispose()
            }
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

    if (isVideoChecked) {
        AndroidView(factory = { context ->
            //Here you can construct your View
            LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 800)
            }
        }, update = { layout ->
            if (isVideoChecked) {
                Log.d("debug", "isVideoChecked true")
                val localVideoStream = callingVM.getLocalVideoStream()
                rendererView = VideoStreamRenderer(localVideoStream, context)
                rendererView?.let {
                    previewVideo = it.createView(CreateViewOptions(ScalingMode.CROP))
                    layout.addView(previewVideo, 0)
                }
            } else {
                Log.d("debug", "isVideoChecked false")
                rendererView?.let {
                    it.dispose()
                    layout.removeView(previewVideo)
                }
            }
        })
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .height(267.dp), Arrangement.SpaceAround, Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = "camera off", tint = Color.Gray)
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        TextField(
            value = nameState.value,
            onValueChange = {
                nameState.value = it
                callSetupVM.setDisplayName(it.text)
            },
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
                    UUID.fromString("9552222e-fd83-45f3-e335-08d9d181339c"),
                    !isMicChecked,
                    isVideoChecked,
                    displayName ?: "aram",
                    JoinCallType.GROUP_CALL)
                val json = Uri.encode(Gson().toJson(joinCallConfig))
                navController.navigate("groupCall/$json")
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Call")
        }
    }
}