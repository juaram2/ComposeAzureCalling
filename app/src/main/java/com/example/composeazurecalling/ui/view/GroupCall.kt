package com.example.composeazurecalling.ui.view

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.telecom.InCallService
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.azure.android.communication.calling.*
import com.example.composeazurecalling.CloudHospitalApp.Companion.context
import com.example.composeazurecalling.model.JoinCallConfig
import com.example.composeazurecalling.viewmodel.CallScreenViewModel
import com.example.composeazurecalling.viewmodel.CommunicationCallingViewModel
import java.util.*


private val MIN_TIME_BETWEEN_PARTICIPANT_VIEW_UPDATES = 2500
private val handler = Handler(Looper.getMainLooper())
private var inCallServiceIntent: Intent? = null
private var timer: Timer? = null
private var localParticipantViewGridIndex: Int? = null
private var participantIdIndexPathMap = HashMap<String, Int>()
private var participantViewList = ArrayList<ParticipantView>()

@Volatile
private var viewUpdatePending = false

@Volatile
private var lastViewUpdateTimestamp: Long = 0
private var callHangUpOverlaid = false

fun Context.findActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun GroupCall(
    joinCallConfig: JoinCallConfig,
    callingVM: CommunicationCallingViewModel,
    groupCallVM: CallScreenViewModel,
    localParticipantView: ParticipantView
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context?.findActivity()

    val isVideoChecked = remember { mutableStateOf(joinCallConfig.isCameraOn) }
    val isMicChecked = remember { mutableStateOf(!joinCallConfig.isMicrophoneMuted) }
    var inviteState by remember { mutableStateOf(false) }
    var callEndState by remember { mutableStateOf(false) }

    val displayedParticipants = callingVM.displayedParticipantsLiveData.observeAsState().value
    val configuredChanged = groupCallVM.configureChanged.observeAsState().value
    val callState = callingVM.callState.observeAsState().value
    val cameraOn = callingVM.cameraOn.observeAsState().value
    val micOn = callingVM.micOn.observeAsState().value
    val count = callingVM._call?.remoteParticipants?.size

    LaunchedEffect(Unit) {
        activity?.let {
            it.volumeControlStream = AudioManager.STREAM_VOICE_CALL
        }

        callingVM.joinCall(joinCallConfig)

        groupCallVM.setConfigureChanged(true)

        if (!joinCallConfig.isMicrophoneMuted) {
            callingVM.turnOnAudioAsync()
        } else {
            callingVM.turnOffAudioAsync()
        }

        if (joinCallConfig.isCameraOn) {
            callingVM.turnOnVideoAsync()
        } else {
            callingVM.turnOffVideoAsync()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    callingVM.resumeVideo()
                }
                Lifecycle.Event.ON_STOP -> {
                    callingVM.pauseVideo()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    callingVM.displayedParticipantsLiveData.removeObservers(lifecycleOwner)
                    localParticipantView.cleanUpVideoRendering()
                    detachFromParentView(localParticipantView)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    displayedParticipants.let {
        handler.post(Runnable {
            if (!viewUpdatePending) {
                viewUpdatePending = true
                val now = System.currentTimeMillis()
                val timeElapsed: Long = now - lastViewUpdateTimestamp
                handler.postDelayed({
                    groupCallVM.setConfigureChanged(true)
                    lastViewUpdateTimestamp = System.currentTimeMillis()
                    viewUpdatePending = false
                }, (MIN_TIME_BETWEEN_PARTICIPANT_VIEW_UPDATES - timeElapsed).coerceAtLeast(0))
            }
        })
    }

    if(cameraOn == true) {
        val localStream = callingVM.getLocalVideoStream()
        localParticipantView.setVideoStream(localStream)
        localParticipantView.setVideoDisplayed(cameraOn)
    } else {
        localParticipantView.setVideoStream(null as LocalVideoStream?)
        localParticipantView.setVideoDisplayed(false)
    }

    if (micOn == true) { // 마이크가 켜져 있다면
        localParticipantView.setIsMuted(false)
    } else { // 마이크가 꺼져 있다
        localParticipantView.setIsMuted(true)
    }

    Column {
        configuredChanged?.let {
            Log.d("GroupCall", "configureChanged : $it")

            ConstraintLayout {
                val (grid, local, text, buttons) = createRefs()

                // TODO: Camera grid
                AndroidView(factory = { context ->
                    GridLayout(context).apply {
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    }
                }, modifier = Modifier.constrainAs(grid) {
                    top.linkTo(parent.top, margin = 16.dp)
                }, update = { gridLayout ->
                    if (callState == CallState.CONNECTED) {
                        gridLayout.post(Runnable { loadGridLayoutViews(gridLayout) })
                    }

                    displayedParticipants?.let {
                        participantsNotNull(callingVM, it, gridLayout)
                    }

                    configuredChanged?.let {
                        Log.d("debug", "configure changed!")
                        gridLayout.post(Runnable { loadGridLayoutViews(gridLayout) })
                    }
                })

                // TODO: Local camera
                if (isVideoChecked.value) {
                    AndroidView(factory = { context ->
                        ConstraintLayout(context).apply {
                            layoutParams = LinearLayout.LayoutParams(300, 400)
                        }
                    }, modifier = Modifier.constrainAs(local) {
                        top.linkTo(parent.top, margin = 16.dp)
                    }, update = { layout ->
                        callState.let { state ->
                            Log.d("GroupCall", "callState : $callState")
                            if (state == CallState.CONNECTED) {
                                initializeCallNotification(context)
                                initParticipantViews(context, callingVM, joinCallConfig, localParticipantView, layout)
                            }
                        }

                        layout.removeView(localParticipantView)

                        if (localParticipantViewGridIndex == null) {
                            setLocalParticipantView(localParticipantView, layout)
                        }

                        if (participantViewList.size == 1) {
                            if (localParticipantViewGridIndex != null) {
                                localParticipantViewGridIndex = null
                            }
                            detachFromParentView(localParticipantView)

                            localParticipantView.setDisplayNameVisible(false)
                            layout.addView(localParticipantView)
                            layout.bringToFront()
                        } else {
                            if (localParticipantViewGridIndex == null) {
                                detachFromParentView(localParticipantView)
                            }
                            appendLocalParticipantView(localParticipantView, callingVM)
                        }
                    })
                }

                Text(text = "participants: $count",
                    modifier = Modifier.constrainAs(text) { top.linkTo(parent.top, margin = 16.dp) })

                Row(
                    Modifier
                        .fillMaxWidth()
                        .constrainAs(buttons) { top.linkTo(parent.top, margin = 16.dp) },
                    horizontalArrangement = Arrangement.SpaceAround)
                {
                    IconButton(onClick = {
                        isVideoChecked.value = !isVideoChecked.value

                        if (isVideoChecked.value) {
                            callingVM.turnOffVideoAsync()
                        } else {
                            callingVM.turnOnVideoAsync()
                        }
                    }) {
                        if (isVideoChecked.value) {
                            Icon(imageVector = Icons.Default.Videocam, contentDescription = "video on")
                        } else {
                            Icon(imageVector = Icons.Default.VideocamOff, contentDescription = "video off")
                        }
                    }
                    IconButton(onClick = {
                        isMicChecked.value = !isMicChecked.value

                        if (isMicChecked.value) {
                            callingVM.turnOffAudioAsync()
                        } else {
                            callingVM.turnOnAudioAsync()
                        }
                    }) {
                        if (isMicChecked.value) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = "mic on")
                        } else {
                            Icon(imageVector = Icons.Default.MicOff, contentDescription = "mic off")
                        }
                    }
                    IconButton(onClick = {
                        inviteState = !inviteState
                        openShareDialogue(callingVM)
                    }) {
                        Icon(imageVector = Icons.Default.GroupAdd, contentDescription = "person add")
                    }
                    IconButton(onClick = { hangup(callingVM, localParticipantView) }) {
                        Icon(imageVector = Icons.Default.CallEnd, contentDescription = "call end", tint = Color.Red)
                    }
                }
            }
        }
    }
}

fun participantsNotNull(
    callingVM: CommunicationCallingViewModel,
    participants: ArrayList<RemoteParticipant>,
    gridLayout: GridLayout
) {
    context?.let {
        Log.d("debug", "displayedParticipants: $participants")
        val prevParticipantViewList = ArrayList<ParticipantView>()
        prevParticipantViewList.addAll(participantViewList)
        val preParticipantIdIndexPathMap = hashMapOf<String, Int>()
        preParticipantIdIndexPathMap.putAll(participantIdIndexPathMap)
        participantViewList.clear()
        participantIdIndexPathMap.clear()

        val displayedRemoteParticipants: ArrayList<RemoteParticipant> = participants

        var indexForNewParticipantViewList = 0
        for (i in displayedRemoteParticipants.indices) {
            val remoteParticipant = displayedRemoteParticipants[i]
            val participantState = remoteParticipant.state
            if (ParticipantState.DISCONNECTED == participantState) {
                continue
            }
            val id: String = callingVM.getId(remoteParticipant).toString()
            var pv: ParticipantView?
            if (preParticipantIdIndexPathMap.containsKey(id)) {
                val prevIndex = preParticipantIdIndexPathMap[id]!!
                pv = prevParticipantViewList[prevIndex]
                val remoteVideoStream = getFirstVideoStream(remoteParticipant)
                pv.setVideoStream(remoteVideoStream)
            } else {
                pv = ParticipantView(it).apply {
                    this.setDisplayName(remoteParticipant.displayName)
                    val remoteVideoStream = getFirstVideoStream(remoteParticipant)
                    this.setVideoStream(remoteVideoStream)
                }
            }
            pv.let { view ->
                view.setIsMuted(remoteParticipant.isMuted)
                view.setIsSpeaking(remoteParticipant.isSpeaking)
                // update the participantIdIndexPathMap, participantViewList and participantsRenderList
                participantIdIndexPathMap[id] = indexForNewParticipantViewList++
                participantViewList.add(view)
            }
        }
        for (id in preParticipantIdIndexPathMap.keys) {
            if (participantIdIndexPathMap.containsKey(id)) {
                continue
            }
            val discardedParticipantView: ParticipantView =
                prevParticipantViewList[preParticipantIdIndexPathMap[id]!!]
            discardedParticipantView.cleanUpVideoRendering()
        }
        gridLayout.post(Runnable {
            if (prevParticipantViewList.size > 1 && participantViewList.size <= 1
                || prevParticipantViewList.size <= 1 && participantViewList.size > 1
            ) {
                setupGridLayout(gridLayout)
            }
            //            updateGridLayoutViews(gridLayout)
        })
    }
}

private fun initializeCallNotification(context: Context) {
    context.let{
        inCallServiceIntent = Intent(it, InCallService::class.java)
        it.startService(inCallServiceIntent)
    }
}

private fun initParticipantViews(context: Context, callingVM: CommunicationCallingViewModel, joinCallConfig: JoinCallConfig, localParticipantView: ParticipantView, layout: ConstraintLayout) {
    // load local participant's view
    localParticipantView.setDisplayName(joinCallConfig.displayName + " (Me)")
    localParticipantView.setIsMuted(joinCallConfig.isMicrophoneMuted)
    localParticipantView.setVideoDisplayed(joinCallConfig.isCameraOn)

    val localVideoStream = callingVM.getLocalVideoStream()
    localParticipantView.setVideoStream(localVideoStream)

    // finalize the view data
    if (participantViewList.size == 1) {
        setLocalParticipantView(localParticipantView, layout)
    } else {
        appendLocalParticipantView(localParticipantView, callingVM)
    }
//    gridLayout.post(Runnable { loadGridLayoutViews() })
}

private fun setupGridLayout(gridLayout: GridLayout) {
    gridLayout.removeAllViews()
    if (participantViewList.size <= 1) {
        gridLayout.rowCount = 1
        gridLayout.columnCount = 1
        gridLayout.addView(
            createCellForGridLayout(
                gridLayout.measuredWidth,
                gridLayout.measuredHeight
            )
        )
    } else {
        gridLayout.rowCount = 2
        gridLayout.columnCount = 2
        for (i in 0..3) {
            gridLayout.addView(
                createCellForGridLayout(
                    gridLayout.measuredWidth / 2,
                    gridLayout.measuredHeight / 2
                )
            )
        }
    }
}

private fun loadGridLayoutViews(gridLayout: GridLayout) {
    setupGridLayout(gridLayout)
    for (i in participantViewList.indices) {
        val participantView: ParticipantView = participantViewList[i]
        detachFromParentView(participantView)
        (gridLayout.getChildAt(i) as LinearLayout).addView(participantView, 0)
    }
}

private fun setLocalParticipantView(localParticipantView: ParticipantView, localVideoViewContainer: ConstraintLayout) {
    Log.d("LOG_TAG", "setLocalParticipantView")
    detachFromParentView(localParticipantView)
    localParticipantView.setDisplayNameVisible(false)
    localVideoViewContainer.addView(localParticipantView)
    localVideoViewContainer.bringToFront()
}

private fun openShareDialogue(callingVM: CommunicationCallingViewModel) {
    Log.d("GroupCall", "Share button clicked!")
    val sendIntent = Intent()
    sendIntent.action = Intent.ACTION_SEND
    sendIntent.putExtra(Intent.EXTRA_TEXT, callingVM.getJoinId())
    sendIntent.putExtra(Intent.EXTRA_TITLE, "Group Call ID")
    sendIntent.type = "text/plain"
    val shareIntent = Intent.createChooser(sendIntent, null)
    context?.startActivity(shareIntent)
}

private fun hangup(callingVM: CommunicationCallingViewModel, localParticipantView: ParticipantView) {
    Log.d("GroupCall", "Hangup button clicked!")
    if (localParticipantView != null) {
        localParticipantView.cleanUpVideoRendering()
        detachFromParentView(localParticipantView)
    }
    context?.let {
        inCallServiceIntent?.let { intent ->
            it.stopService(intent)
        }
    }

//    callHangupConfirmButton.isEnabled = false
    callingVM.hangupAsync()

//    findNavController().popBackStack()
}

private fun detachFromParentView(view: View?) {
    if (view != null && view.parent != null) {
        (view.parent as ViewGroup).removeView(view)
    }
}

private fun getFirstVideoStream(remoteParticipant: RemoteParticipant): RemoteVideoStream? {
    return if (remoteParticipant.videoStreams.size > 0) {
        remoteParticipant.videoStreams[0]
    } else null
}


private fun appendLocalParticipantView(localParticipantView: ParticipantView, callingVM: CommunicationCallingViewModel) {
    localParticipantViewGridIndex = participantViewList.size
    localParticipantView.setDisplayNameVisible(true)
    callingVM.cameraOn.value?.let {
        localParticipantView.setVideoDisplayed(it)
    }
    participantViewList.add(localParticipantView)
}

private fun createCellForGridLayout(width: Int, height: Int): LinearLayout? {
    context.let {
        val cell = LinearLayout(it)
        val params =
            GridLayout.LayoutParams()
        params.width = width
        params.height = height
        cell.layoutParams = params
        return cell
    }
    return null
}