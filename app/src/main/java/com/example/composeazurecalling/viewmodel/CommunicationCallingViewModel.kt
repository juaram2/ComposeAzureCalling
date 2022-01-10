package com.example.composeazurecalling.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.azure.android.communication.calling.*
import com.azure.android.communication.common.*
import com.example.composeazurecalling.CloudHospitalApp.Companion.context
import com.example.composeazurecalling.model.JoinCallConfig
import com.example.composeazurecalling.service.TokenService
import com.example.composeazurecalling.utils.Constants
import java9.util.concurrent.CompletableFuture
import java9.util.function.Consumer
import java.util.*
import kotlin.collections.HashMap

class CommunicationCallingViewModel: ViewModel(),
    PropertyChangedListener,
    ParticipantsUpdatedListener,
    LocalVideoStreamsUpdatedListener,
    IncomingCallListener
{
    companion object {
        val LOG_TAG = CommunicationCallingViewModel::class.java.simpleName
    }

    //region Properties
    @SuppressLint("StaticFieldLeak")
    private var _callClient: CallClient? = null
    private var _deviceManager: DeviceManager? = null
    private var _availableCameras = HashMap<CameraFacing, VideoDeviceInfo>()
    private var _initialCamera: VideoDeviceInfo? = null
    private var _localVideoStream: LocalVideoStream? = null

    val isSetup = MutableLiveData<Boolean?>(false)

    private var _joinId: UUID? = null
    private var tokenFetcher = TokenService().getCommunicationTokenAsync()
    private val _callAgent = MutableLiveData<CallAgent?>()
    val callAgent: LiveData<CallAgent?> = _callAgent

    var _call: Call? = null

    private val _callState = MutableLiveData<CallState?>().apply {
        value = CallState.NONE
    }
    val callState: LiveData<CallState?> = _callState

    private val _incomingCall = MutableLiveData<IncomingCall?>()
    val incomingCall: MutableLiveData<IncomingCall?> = _incomingCall

    private var _remoteParticipantsMap = java.util.HashMap<String, RemoteParticipant>()
    private var _displayedRemoteParticipants = ArrayList<RemoteParticipant>()

    private var _displayedParticipantsLiveData = MutableLiveData<ArrayList<RemoteParticipant>>()
    val displayedParticipantsLiveData: LiveData<ArrayList<RemoteParticipant>> = _displayedParticipantsLiveData

    private var _displayedRemoteParticipantIds = HashSet<String>()
    private var _videoStreamsUpdatedListenersMap = HashMap<String, RemoteVideoStreamsUpdatedListener>()
    private var _mutedChangedListenersMap = java.util.HashMap<String, PropertyChangedListener>()
    private var _isSpeakingChangedListenerMap = java.util.HashMap<String, PropertyChangedListener>()
    private var _participantStateChangedListenerMap =
        java.util.HashMap<String, PropertyChangedListener>()
    //endregion

    //region Public Methods
    fun setupCalling() {
        _callClient = CallClient()
        setupDeviceManager()
    }

    fun getLocalVideoStream(): LocalVideoStream?  {
        Log.d(LOG_TAG, "getLocalVideoStream")
        if (_localVideoStream == null) {
            _initialCamera?.let {
                Log.d(LOG_TAG, "_initialCamera: ${it.id}")
                return initializeLocalVideoStream(it)
            } ?: run {
                Log.d(LOG_TAG, "Camera is not initialized yet!")
                return null
            }
        } else {
            return _localVideoStream
        }
    }

    fun joinCall(joinCallConfig: JoinCallConfig) {
        _joinId = joinCallConfig.joinId
        val callLocator = GroupCallLocator(_joinId)

        _callAgent.value?.let {
            // dispose current callAgent
            // TODO: Remove listeners
            it.dispose()
        }

        createCallAgentAsync(joinCallConfig.displayName)
            ?.whenComplete { callAgent: CallAgent?, callAgentThrowable: Throwable? ->
                callAgent?.let { agent ->
                    _callAgent.postValue(agent)
                    // TODO: Add Direct call listeners
//                    agent.addOnCallsUpdatedListener(this)
                    agent.addOnIncomingCallListener(this)

                    var audioOptions =  AudioOptions()
                    audioOptions.isMuted = joinCallConfig.isMicrophoneMuted

                    if (joinCallConfig.isCameraOn) {
                        val localVideoStreams = arrayOfNulls<LocalVideoStream>(1)
                        localVideoStreams[0] = getLocalVideoStream()
                        var videoOptions = VideoOptions(localVideoStreams)
                        callWithOptions(agent, audioOptions, videoOptions, callLocator)
                    } else {
                        callWithOptions(agent, audioOptions, null, callLocator)
                    }
                }
            }
    }

    fun getJoinId(): UUID? {
        return _joinId
    }

    fun turnOnVideoAsync() {
        Log.d(LOG_TAG, "turnOnVideoAsync")
        _call?.let { call ->
            getLocalVideoStream()?.let {
                call.startVideo(context, it)
            }
        }
    }

    fun turnOffVideoAsync() {
        Log.d(LOG_TAG, "turnOffVideoAsync")
        _call?.let { call ->
            getLocalVideoStream()?.let {
                call.stopVideo(context, it)
            }
        }
    }

    fun turnOnAudioAsync() {
        Log.d(LOG_TAG, "turnOnAudioAsync")
        _call?.let{ call ->
            call.unmute(context)
        }
    }

    fun turnOffAudioAsync() {
        Log.d(LOG_TAG, "turnOffAudioAsync")
        _call?.let { call ->
            call.mute(context)
        }
    }

    fun pauseVideo() {
        Log.d(LOG_TAG, "pauseVideo")
        _call?.let { call ->
            turnOffVideoAsync()
        }
    }

    fun resumeVideo() {
        Log.d(LOG_TAG, "resumeVideo")
        _call?.let { call ->
            turnOnVideoAsync()
        }
    }

    fun getRemoteParticipantCount():Int {
        return _remoteParticipantsMap.size
    }

    fun getId(remoteParticipant: RemoteParticipant): String? {
        val identifier = remoteParticipant.identifier
        return if (identifier is PhoneNumberIdentifier) {
            identifier.phoneNumber
        } else if (identifier is MicrosoftTeamsUserIdentifier) {
            identifier.userId
        } else if (identifier is CommunicationUserIdentifier) {
            identifier.id
        } else {
            (identifier as UnknownIdentifier).id
        }
    }

    fun hangupAsync() /*:CompletableFuture*/ {
        Log.d(LOG_TAG, "hangupAsync")
        _call?.let { call ->
            call.removeOnRemoteParticipantsUpdatedListener(this::onParticipantsUpdated)
            call.hangUp(HangUpOptions())
        }
    }
    //endregion

    //region Private Methods
    private fun setupDeviceManager() {
        _callClient?.let { callClient ->
            callClient.getDeviceManager(context)
                .thenAccept(Consumer { deviceManager ->
                    Log.d(LOG_TAG, "Device Manager created")
                    _deviceManager = deviceManager
                    initializeCameras(deviceManager)
                })
        }
    }

    private fun initializeCameras(deviceManager: DeviceManager) {
        Log.d(LOG_TAG, "InitializeCameras")

        _availableCameras = HashMap<CameraFacing, VideoDeviceInfo>()

        val initialCameras = deviceManager.cameras
        Log.d(LOG_TAG, "initialCameras: $initialCameras")
        addVideoDevices(initialCameras)

        val videoDevicesUpdatedListener = VideoDevicesUpdatedListener { videoDevicesUpdatedEvent: VideoDevicesUpdatedEvent? ->
            updateVideoDevices(videoDevicesUpdatedEvent)
            initializeFrontCameraIfRequired()
        }
        deviceManager.addOnCamerasUpdatedListener(videoDevicesUpdatedListener)

    }

    private fun removeVideoDevices(removedVideoDevices: List<VideoDeviceInfo>) {
        Log.d(LOG_TAG, "Removed Cameras: " + removedVideoDevices.size)
        for (removedVideoDevice in removedVideoDevices) {
            val cameraFacingName = removedVideoDevice.cameraFacing
            _availableCameras.remove(cameraFacingName)
        }
    }

    private fun addVideoDevices(addedVideoDevices: List<VideoDeviceInfo>) {
        Log.d(LOG_TAG, "Added Cameras: " + addedVideoDevices.size)
        for (addedVideoDevice in addedVideoDevices) {
            _availableCameras[addedVideoDevice.cameraFacing] = addedVideoDevice
        }
        initializeFrontCameraIfRequired()
    }

    private fun updateVideoDevices(videoDevicesUpdatedEvent: VideoDevicesUpdatedEvent?) {
        videoDevicesUpdatedEvent?.let {
            removeVideoDevices(videoDevicesUpdatedEvent.removedVideoDevices)
            addVideoDevices(videoDevicesUpdatedEvent.addedVideoDevices)
        }
    }

    private fun initializeFrontCameraIfRequired() {
        val initialCamera: VideoDeviceInfo? = getFrontCamera()
        if (initialCamera != null) {
            _initialCamera = initialCamera
            isSetup.postValue(true)
            Log.d(LOG_TAG, "setup completed")
        }
    }

    private fun getFrontCamera(): VideoDeviceInfo? {
        return _availableCameras.get(CameraFacing.FRONT)
    }

    private fun getBackCamera(): VideoDeviceInfo? {
        return _availableCameras.get(CameraFacing.BACK)
    }

    private fun initializeLocalVideoStream(camera: VideoDeviceInfo): LocalVideoStream{
        return LocalVideoStream(camera, context)
    }

    private fun createCallAgentAsync(displayName: String?): CompletableFuture<CallAgent>? {
        val communicationTokenRefreshOptions = CommunicationTokenRefreshOptions({
            TokenService().getCommunicationTokenAsync().get()
        }, true)
        val communicationTokenCredential = CommunicationTokenCredential(
            communicationTokenRefreshOptions
        )

        val callAgentOptions = CallAgentOptions()
        callAgentOptions.displayName = displayName
        _callClient?.let { callClient ->
            return callClient.createCallAgent(
                context,
                communicationTokenCredential,
                callAgentOptions
            )
        } ?: run {
            return null
        }
    }

    private fun callWithOptions(
        callAgent: CallAgent,
        audioOptions: AudioOptions,
        videoOptions: VideoOptions?,
        groupCallLocator: JoinMeetingLocator
    ) {
        val joinCallOptions = JoinCallOptions()
        joinCallOptions.videoOptions = videoOptions
        joinCallOptions.audioOptions = audioOptions

        _call = callAgent.join(context, groupCallLocator, joinCallOptions)
        _call?.let { call ->
            call.addOnStateChangedListener(this)
            call.addOnRemoteParticipantsUpdatedListener(this)
        }
    }

    private fun addParticipants(addedParticipants: MutableList<RemoteParticipant>):Boolean {
        Log.d(LOG_TAG, "addParticipants")
        var isParticipantsAddedToDisplayedRemoteParticipants = false

        for (addedParticipant: RemoteParticipant in addedParticipants) {
            var id = getId(addedParticipant)

            if (_remoteParticipantsMap.containsKey(id)) {
                continue
            }

            id?.let {
                _remoteParticipantsMap.put(id, addedParticipant)
                bindOnVideoStreamsUpdatedListener(addedParticipant)
                bindOnIsMutedChangedListener(addedParticipant)
                bindOnIsSpeakingChangedListener(addedParticipant)
                bindOnParticipantStateChangedListener(addedParticipant)
            }
        }
        if (_remoteParticipantsMap.size > _displayedRemoteParticipants.size) {
            for (id:String in _remoteParticipantsMap.keys) {
                if (_displayedRemoteParticipants.size == Constants.DISPLAYED_REMOTE_PARTICIPANT_SIZE_LIMIT) {
                    break
                }
                if (!_displayedRemoteParticipantIds.contains(id)) {
                    _remoteParticipantsMap[id]?.let { _displayedRemoteParticipants.add(it) }
                    _displayedRemoteParticipantIds.add(id)
                    isParticipantsAddedToDisplayedRemoteParticipants = true
                }
            }
        }

        return isParticipantsAddedToDisplayedRemoteParticipants
    }

    private fun removeParticipants(removedParticipants: MutableList<RemoteParticipant>):Boolean {
        Log.d(LOG_TAG, "removeParticipants")
        var isDisplayedRemoteParticipantsChanged = false
        for (removedParticipant: RemoteParticipant in removedParticipants) {
            var removedParticipantId = getId(removedParticipant)

            unbindOnIsMutedChangedListener(removedParticipant)
            unbindOnIsSpeakingChangedListener(removedParticipant)
            unbindOnParticipantStateChangedListener(removedParticipant)

            _remoteParticipantsMap.remove(removedParticipantId)

            if (_displayedRemoteParticipantIds.contains(removedParticipantId)) {
                var indexTobeRmovedForDisplayedRemoteParticipants = -1
                for (i in 0.._displayedRemoteParticipants.size) {
                    var currentDisplayedRemoteParticipantId = getId(
                        _displayedRemoteParticipants.get(
                            i
                        )
                    )
                    if (currentDisplayedRemoteParticipantId.equals(removedParticipantId)) {
                        indexTobeRmovedForDisplayedRemoteParticipants = i
                        break
                    }
                }
                if (indexTobeRmovedForDisplayedRemoteParticipants != -1) {
                    _displayedRemoteParticipants.removeAt(
                        indexTobeRmovedForDisplayedRemoteParticipants
                    )
                    _displayedRemoteParticipantIds.remove(removedParticipantId)
                    isDisplayedRemoteParticipantsChanged = true
                }
            }
        }
        if (_remoteParticipantsMap.size > _displayedRemoteParticipants.size) {
            for (id in _remoteParticipantsMap.keys) {
                if (_displayedRemoteParticipants.size == Constants.DISPLAYED_REMOTE_PARTICIPANT_SIZE_LIMIT) {
                    break
                }
                if (!_displayedRemoteParticipantIds.contains(id)) {
                    _remoteParticipantsMap[id]?.let { _displayedRemoteParticipants.add(it) }
                    _displayedRemoteParticipantIds.add(id)
                    isDisplayedRemoteParticipantsChanged = true
                }
            }
        }
        return isDisplayedRemoteParticipantsChanged
    }

    private fun bindOnVideoStreamsUpdatedListener(remoteParticipant: RemoteParticipant) {
        Log.d(LOG_TAG, "bindOnVideoStreamsUpdatedListener")
        var username = remoteParticipant.displayName
        var id = getId(remoteParticipant)
        val remoteVideoStreamsUpdatedListener =
            RemoteVideoStreamsUpdatedListener { _: RemoteVideoStreamsEvent? ->
                if (!_displayedRemoteParticipantIds.contains(id)) {
                    return@RemoteVideoStreamsUpdatedListener
                }
                _displayedParticipantsLiveData.postValue(_displayedRemoteParticipants)
            }

        remoteParticipant.addOnVideoStreamsUpdatedListener(remoteVideoStreamsUpdatedListener)
        id?.let {
            _videoStreamsUpdatedListenersMap[id] = remoteVideoStreamsUpdatedListener
        }
    }

    private fun unbindOnVideoStreamsUpdatedListener(remoteParticipant: RemoteParticipant) {
        Log.d(LOG_TAG, "unbindOnVideoStreamsUpdatedListener")
        var removedParticipantId = getId(remoteParticipant)
        remoteParticipant.removeOnVideoStreamsUpdatedListener(
            _videoStreamsUpdatedListenersMap.remove(
                removedParticipantId
            )
        )
    }

    private fun bindOnIsMutedChangedListener(remoteParticipant: RemoteParticipant) {
        Log.d(LOG_TAG, "bindOnIsMutedChangedListener")
        var username = remoteParticipant.displayName
        var id = getId(remoteParticipant)
        val remoteIsMutedChangedListener =
            label@ PropertyChangedListener {
                Log.d(
                    LOG_TAG, String.format(
                        "Remote Participant %s addOnIsMutedChangedListener called",
                        username
                    )
                )
                if (!_displayedRemoteParticipantIds.contains(id)) {
                    return@PropertyChangedListener
                }
                _displayedParticipantsLiveData.postValue(_displayedRemoteParticipants)
            }
        remoteParticipant.addOnIsMutedChangedListener(remoteIsMutedChangedListener)
        id?.let {
            _mutedChangedListenersMap.put(id, remoteIsMutedChangedListener)
        }
    }

    private fun unbindOnIsMutedChangedListener(remoteParticipant: RemoteParticipant) {
        Log.d(LOG_TAG, "unbindOnIsMutedChangedListener")
        var removedParticipantId = getId(remoteParticipant)
        remoteParticipant.removeOnIsMutedChangedListener(
            _mutedChangedListenersMap.remove(
                removedParticipantId
            )
        )
    }

    private fun bindOnIsSpeakingChangedListener(remoteParticipant: RemoteParticipant) {
        Log.d(LOG_TAG, "bindOnIsSpeakingChangedListener")
        var username = remoteParticipant.displayName
        var id = getId(remoteParticipant)
        id?.let {
            val remoteIsSpeakingChangedListener = PropertyChangedListener {
                Log.d(
                    LOG_TAG, String.format(
                        "Remote Participant %s addOnIsMutedChangedListener called",
                        username
                    )
                )
                if (_displayedRemoteParticipantIds.contains(id)) {
                    _displayedParticipantsLiveData.postValue(_displayedRemoteParticipants)
                }
                if (_displayedRemoteParticipantIds.contains(id) || !remoteParticipant.isSpeaking) {
                    return@PropertyChangedListener
                }

                findInactiveSpeakerToSwap(remoteParticipant, id)
            }
            remoteParticipant.addOnIsSpeakingChangedListener(remoteIsSpeakingChangedListener)
            _isSpeakingChangedListenerMap.put(id, remoteIsSpeakingChangedListener)
        }
    }

    private fun unbindOnIsSpeakingChangedListener(remoteParticipant: RemoteParticipant) {
        Log.d(LOG_TAG, "unbindOnIsSpeakingChangedListener")
        var removedParticipantId = getId(remoteParticipant)
        remoteParticipant.removeOnIsSpeakingChangedListener(
            _isSpeakingChangedListenerMap.remove(
                removedParticipantId
            )
        )
    }

    private fun bindOnParticipantStateChangedListener(remoteParticipant: RemoteParticipant) {
        Log.d(LOG_TAG, "bindOnParticipantStateChangedListener")
        var username = remoteParticipant.displayName
        var id = getId(remoteParticipant)
        val remoteParticipantStateChangedListener =
            PropertyChangedListener { propertyChangedEvent: PropertyChangedEvent? ->
                Log.d(
                    LOG_TAG, String.format(
                        "Remote Participant %s addOnParticipantStateChangedListener called",
                        username
                    )
                )
            }
        remoteParticipant.addOnStateChangedListener(remoteParticipantStateChangedListener)
        id?.let {
            _participantStateChangedListenerMap.put(id, remoteParticipantStateChangedListener)
        }
    }

    private fun unbindOnParticipantStateChangedListener(remoteParticipant: RemoteParticipant) {
        Log.d(LOG_TAG, "unbindOnParticipantStateChangedListener")
        var removedParticipantId = getId(remoteParticipant)
        remoteParticipant.removeOnStateChangedListener(
            _participantStateChangedListenerMap.remove(
                removedParticipantId
            )
        )
    }

    private fun findInactiveSpeakerToSwap(remoteParticipant: RemoteParticipant, id: String) {
        Log.d(LOG_TAG, "findInactiveSpeakerToSwap")
        for (i in 0.._displayedRemoteParticipants.size) {
            var displayedRemoteParticipant:RemoteParticipant = _displayedRemoteParticipants.get(i)
            if (!displayedRemoteParticipant.isSpeaking) {
                var originId = getId(displayedRemoteParticipant)
                _displayedRemoteParticipantIds.remove(originId)
                _displayedRemoteParticipants[i] = remoteParticipant
                _displayedRemoteParticipantIds.add(id)
                _displayedParticipantsLiveData.postValue(_displayedRemoteParticipants)
                break
            }
        }
    }
    //endregion

    //region Delegates

    // CallsUpdatedListener

    // IncomingCallListener
    override fun onIncomingCall(incomingCall: IncomingCall?) {
        incomingCall?.let {
            Log.d(LOG_TAG, "incomingCall :: ${it.callerInfo}")
            it.addOnCallEndedListener(this)
            _incomingCall.postValue(it)
        }
    }

    // OnStateChangedListeners, OnCallEndedListener
    override fun onPropertyChanged(propertyChangedEvent: PropertyChangedEvent?) {
        Log.d(LOG_TAG, "onPropertyChanged :: ${propertyChangedEvent.toString()}")
        try {
            _call?.let { call ->
                _callState.postValue(call.state)
                Log.d(LOG_TAG, "CallState: ${call.state}")
                when (call.state) {
                    CallState.CONNECTING -> {
                        Log.d(LOG_TAG, "CallState: Connecting")
                    }
                    CallState.RINGING -> {
                        Log.d(LOG_TAG, "CallState: Ringing")
                    }
                    CallState.CONNECTED -> {
                        Log.d(LOG_TAG, "CallState: Connected")
                        call.remoteParticipants?.let {
                            Log.d(LOG_TAG, "remoteParticipants: ${it.size}")
                            addParticipants(it)
                            _displayedParticipantsLiveData.postValue(_displayedRemoteParticipants)
                        }
                    }
                    CallState.DISCONNECTING -> {
                        Log.d(LOG_TAG, "CallState: Disconnecting")
                    }
                    CallState.DISCONNECTED -> {
                        Log.d(LOG_TAG, "CallState: Disconnected")
                        call.removeOnStateChangedListener(this)
                        call.removeOnRemoteParticipantsUpdatedListener(this)
                        call.removeOnLocalVideoStreamsUpdatedListener(this)
                        _call = null
                    }
                    else -> {
                        return
                    }
                }
            } ?: run {
                _incomingCall.value?.let {
                    Log.d(LOG_TAG, "IncomingCall disconnected}")
                    it.removeOnCallEndedListener(this)
                    _incomingCall.postValue(null)
                }
            }
        } catch (ex: java.lang.Exception) {
            Log.e(LOG_TAG, "onPropertyChanged: " + ex.message)
        }
    }

    // ParticipantsUpdatedListener
    override fun onParticipantsUpdated(participantsUpdatedEvent: ParticipantsUpdatedEvent?) {
        var doUpdate = false
        if (participantsUpdatedEvent != null) {
            if (!participantsUpdatedEvent.removedParticipants.isEmpty()) {
                doUpdate = doUpdate or removeParticipants(participantsUpdatedEvent.removedParticipants)
            }
        }
        if (participantsUpdatedEvent != null) {
            if (!participantsUpdatedEvent.addedParticipants.isEmpty()) {
                doUpdate = doUpdate or addParticipants(participantsUpdatedEvent.addedParticipants)
            }
        }
        if (doUpdate) {
            _displayedParticipantsLiveData.postValue(_displayedRemoteParticipants)
        }
    }

    // LocalVideoStreamsUpdatedListener
    override fun onLocalVideoStreamsUpdated(localVideoStreamsUpdatedEvent: LocalVideoStreamsUpdatedEvent?) {
        Log.d(
            LOG_TAG,
            "onLocalVideoStreamsChanged addedStreams: ${localVideoStreamsUpdatedEvent?.addedStreams?.size}"
        )
        Log.d(
            LOG_TAG,
            "onLocalVideoStreamsChanged removedStreams: ${localVideoStreamsUpdatedEvent?.removedStreams?.size}"
        )
    }

    //endregion
}