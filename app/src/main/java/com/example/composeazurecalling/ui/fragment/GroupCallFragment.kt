package com.example.composeazurecalling.ui.fragment

import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.InCallService
import android.util.Log
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.azure.android.communication.calling.*
import com.example.composeazurecalling.databinding.GroupCallFragmentBinding
import com.example.composeazurecalling.model.JoinCallConfig
import com.example.composeazurecalling.ui.view.ParticipantView
import com.example.composeazurecalling.viewmodel.CommunicationCallingViewModel
import com.example.composeazurecalling.viewmodel.GroupCallViewModel
import java.util.*

class GroupCallFragment : Fragment() {

    //private val callingManager by activityViewModels<CallingManager>()
    private val communicationCallingViewModel by activityViewModels<CommunicationCallingViewModel>() //communicationCallingViewModel

    private lateinit var groupCallViewModel : GroupCallViewModel

    private lateinit var joinCallConfig: JoinCallConfig
    private val MIN_TIME_BETWEEN_PARTICIPANT_VIEW_UPDATES = 2500
    private val handler = Handler(Looper.getMainLooper())
    private var inCallServiceIntent: Intent? = null
    private lateinit var gridLayout: GridLayout
    private lateinit var videoImageButton: ImageButton
    private lateinit var audioImageButton: ImageButton
    private lateinit var callHangupOverlay: LinearLayout
    private var infoHeaderView: View? = null
    private var timer: Timer? = null
    private var localParticipantViewGridIndex: Int? = null
    private var participantIdIndexPathMap = HashMap<String, Int>()
    private var participantViewList = ArrayList<ParticipantView>()
    private lateinit var localParticipantView: ParticipantView
    private lateinit var localVideoViewContainer: ConstraintLayout

    @Volatile
    private var viewUpdatePending = false

    @Volatile
    private var lastViewUpdateTimestamp: Long = 0
    private var callHangUpOverlaid = false
    private lateinit var callHangupConfirmButton: Button
    private var participantCountTextView: TextView? = null

    companion object {
        fun newInstance() = GroupCallFragment()
        private val LOG_TAG: String = GroupCallFragment::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.let{
            it.volumeControlStream = AudioManager.STREAM_VOICE_CALL
            val actionBar: android.app.ActionBar? = it.actionBar
            actionBar?.hide()
            it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            localParticipantView = ParticipantView(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        // if the app is already in landscape mode, this check will hide status bar
        setStatusBarVisibility()

        var binding = GroupCallFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        groupCallViewModel = ViewModelProvider(this).get(GroupCallViewModel::class.java)
        binding.groupCallViewModel = groupCallViewModel

        binding.callVideo.isEnabled = false
        binding.callAudio.isEnabled = false

        gridLayout = binding.groupCallTable
        videoImageButton = binding.callVideo
        audioImageButton = binding.callAudio
        callHangupOverlay = binding.callHangupOverlay
        callHangupConfirmButton = binding.callHangupConfirm
        infoHeaderView = binding.infoHeader
        participantCountTextView = binding.participantTextView
        localVideoViewContainer = binding.yourCameraHolder
        callHangupOverlay = binding.callHangupOverlay

        binding.callShare.setOnClickListener { openShareDialogue() }
        binding.callHangup.setOnClickListener { openHangupDialog() }
        binding.callHangupConfirm.setOnClickListener{ hangup() }
        binding.callHangupCancel.setOnClickListener {closeHangupDialog() }

        binding.callVideo.setOnClickListener {
            communicationCallingViewModel.cameraOn.value?.let{
                if(!it) {
                    communicationCallingViewModel.turnOnVideoAsync()
                } else {
                    communicationCallingViewModel.turnOffVideoAsync()
                }
            }
        }

        binding.callAudio.setOnClickListener {
            communicationCallingViewModel.micOn.value?.let {
                if(!it) {
                    communicationCallingViewModel.turnOnAudioAsync()
                } else {
                    communicationCallingViewModel.turnOffAudioAsync()
                }
            }
        }

        communicationCallingViewModel.displayedParticipantsLiveData.observe(viewLifecycleOwner) {
            val prevParticipantViewList = ArrayList<ParticipantView>()
            prevParticipantViewList.addAll(participantViewList)
            val preParticipantIdIndexPathMap = hashMapOf<String, Int>()
            preParticipantIdIndexPathMap.putAll(participantIdIndexPathMap)
            participantViewList.clear()
            participantIdIndexPathMap.clear()

            it?.let { participants ->
                val displayedRemoteParticipants: ArrayList<RemoteParticipant> = participants

                var indexForNewParticipantViewList = 0
                for (i in displayedRemoteParticipants.indices) {
                    val remoteParticipant = displayedRemoteParticipants[i]
                    val participantState = remoteParticipant.state
                    if (ParticipantState.DISCONNECTED == participantState) {
                        continue
                    }
                    val id: String = communicationCallingViewModel.getId(remoteParticipant).toString()
                    var pv: ParticipantView? = null
                    if (preParticipantIdIndexPathMap.containsKey(id)) {
                        val prevIndex = preParticipantIdIndexPathMap[id]!!
                        pv = prevParticipantViewList[prevIndex]
                        val remoteVideoStream = getFirstVideoStream(remoteParticipant)
                        pv.setVideoStream(remoteVideoStream)
                    } else {
                        activity?.let {
                            pv = ParticipantView(it).apply {
                                this.setDisplayName(remoteParticipant.displayName)
                                val remoteVideoStream = getFirstVideoStream(remoteParticipant)
                                this.setVideoStream(remoteVideoStream)
                            }
                        }
                    }
                    pv?.let { view ->
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
                if (participantViewList.size == 1) {
                    if (localParticipantViewGridIndex != null) {
                        localParticipantViewGridIndex = null
                    }
                    setLocalParticipantView()
                } else {
                    if (localParticipantViewGridIndex == null) {
                        detachFromParentView(localParticipantView)
                    }
                    appendLocalParticipantView()
                }
                gridLayout.post(Runnable {
                    if (prevParticipantViewList.size > 1 && participantViewList.size <= 1
                        || prevParticipantViewList.size <= 1 && participantViewList.size > 1
                    ) {
                        setupGridLayout()
                    }
//                    updateGridLayoutViews()
                })
            }
        }

        communicationCallingViewModel.callState.observe(viewLifecycleOwner, { callState ->
            Log.d(LOG_TAG, "callState : ${callState}")
            if (callState == CallState.CONNECTED) {
                binding.callAudio.isEnabled = true
                binding.callVideo.isEnabled = true
                initializeCallNotification()
                initParticipantViews()
                showParticipantHeaderNotification()
            }
        })

        communicationCallingViewModel.cameraOn.observe(viewLifecycleOwner, {
            if(it != null) {
                Log.d(LOG_TAG, "cameraOn : ${it}")
                if(it) {
                    localParticipantView.setVideoStream(communicationCallingViewModel.getLocalVideoStream())
                    localParticipantView.setVideoDisplayed(it)
                    binding.yourCameraHolder.visibility = if (localParticipantViewGridIndex == null && !it) View.INVISIBLE else View.VISIBLE
                    videoImageButton.isSelected = true
                } else {
                    localParticipantView.setVideoStream(null as LocalVideoStream?)
                    localParticipantView.setVideoDisplayed(it)
                    binding.yourCameraHolder.visibility = if (localParticipantViewGridIndex == null && !it) View.INVISIBLE else View.VISIBLE
                    videoImageButton.isSelected = false
                }
            }
        })

        communicationCallingViewModel.micOn.observe(viewLifecycleOwner, {
            if(it != null) {
                Log.d(LOG_TAG, "micOn : ${it}")
                if(it) { // 마이크가 켜져 있다면
                    audioImageButton.isSelected = true
                    localParticipantView.setIsMuted(false)
                } else { // 마이크가 꺼져 있다
                    audioImageButton.isSelected = false
                    localParticipantView.setIsMuted(true)
                }
            }
        })

        groupCallViewModel.configureChanged.observe(viewLifecycleOwner) {
            Log.d(LOG_TAG, "configureChanged : ${it}")
            setStatusBarVisibility()
            gridLayout.setOnTouchListener(View.OnTouchListener { v: View?, event: MotionEvent ->
                if (event.action == MotionEvent.ACTION_UP) {
                    toggleParticipantHeaderNotification()
                }
                false
            })

            binding.callHangupOverlay.setOnTouchListener(View.OnTouchListener { v: View?, event: MotionEvent? ->
                Log.d(LOG_TAG, "callHangupOverlay")
                closeHangupDialog()
                true
            })

            gridLayout.post(Runnable { loadGridLayoutViews() })
            if (localParticipantViewGridIndex == null) {
                setLocalParticipantView()
            }
        }

        communicationCallingViewModel.displayedParticipantsLiveData.observe(viewLifecycleOwner) {
            handler.post(Runnable {
                if (viewUpdatePending) {
                } else {
                    viewUpdatePending = true
                    val now = System.currentTimeMillis()
                    val timeElapsed: Long = now - lastViewUpdateTimestamp
                    handler.postDelayed({
                        groupCallViewModel.setConfigureChanged(true)
                        setParticipantCountToFloatingHeader(communicationCallingViewModel.getRemoteParticipantCount())
                        lastViewUpdateTimestamp = System.currentTimeMillis()
                        viewUpdatePending = false
                    }, (MIN_TIME_BETWEEN_PARTICIPANT_VIEW_UPDATES - timeElapsed).coerceAtLeast(0)
                    )
                }
            })
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /* get Join Call Config */
        Log.d(LOG_TAG, "onViewCreated")
//        joinCallConfig = GroupCallFragmentArgs.fromBundle(requireArguments()).jOINCALLCONFIG
        setLayoutComponentState(joinCallConfig.isMicrophoneMuted, joinCallConfig.isCameraOn, this.callHangUpOverlaid)
        communicationCallingViewModel.joinCall(joinCallConfig)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig!!)
        groupCallViewModel.setConfigureChanged(true)
    }

    private fun setStatusBarVisibility() {
        activity?.let{
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                val decorView: View = it.window.decorView
                // Hide Status Bar.
                val uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                decorView.systemUiVisibility = uiOptions
            } else {
                val decorView: View = it.window.decorView
                // Show Status Bar.
                val uiOptions = View.SYSTEM_UI_FLAG_VISIBLE
                decorView.systemUiVisibility = uiOptions
            }
        }
    }

    override fun onPause() {
        Log.d(LOG_TAG, "onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(LOG_TAG, "onStop")
        communicationCallingViewModel.pauseVideo()
        super.onStop()
    }

    override fun onResume() {
        Log.d(LOG_TAG, "onResume")
        communicationCallingViewModel.resumeVideo()
        super.onResume()
    }


    override fun onDestroy() {
        Log.d(LOG_TAG, "onDestroy")
        communicationCallingViewModel.displayedParticipantsLiveData.removeObservers(this)
        if (localParticipantView != null) {
            localParticipantView.cleanUpVideoRendering()
            detachFromParentView(localParticipantView)
        }
        super.onDestroy()
    }

    private fun initializeCallNotification() {
        activity?.let{
            inCallServiceIntent = Intent(it, InCallService::class.java)
            it.startService(inCallServiceIntent)
        }
    }

    private fun initParticipantViews() {
        // load local participant's view
        localParticipantView.setDisplayName(joinCallConfig.displayName + " (Me)")
        localParticipantView.setIsMuted(joinCallConfig.isMicrophoneMuted)
        localParticipantView.setVideoDisplayed(joinCallConfig.isCameraOn)

        val localVideoStream = communicationCallingViewModel.getLocalVideoStream()
        localParticipantView.setVideoStream(localVideoStream)

        // finalize the view data
        if (participantViewList.size == 1) {
            setLocalParticipantView()
        } else {
            appendLocalParticipantView()
        }
        gridLayout.post(Runnable { loadGridLayoutViews() })
    }

    private fun showParticipantHeaderNotification() {
        //updateParticipantNotificationCount()
        setParticipantCountToFloatingHeader(communicationCallingViewModel.getRemoteParticipantCount())
        setFloatingHeaderVisibility(View.VISIBLE)
        infoHeaderView?.bringToFront()
        initializeTimer()
    }

    private fun toggleParticipantHeaderNotification() {
        if (infoHeaderView?.visibility == View.VISIBLE) {
            recycleTimer()
            setFloatingHeaderVisibility(View.GONE)
        } else {
            showParticipantHeaderNotification()
        }
    }

    private fun initializeTimer() {
        recycleTimer()
        timer = Timer()
        this.timer?.let {
            it.schedule(object : TimerTask() {
                override fun run() {
                    setFloatingHeaderVisibility(View.GONE)
                }
            }, 5000)
        }
    }

    private fun recycleTimer() {
        timer?.let {
            it.cancel()
        }
        timer = null
    }

    private fun setParticipantCountToFloatingHeader(text: Int) {
        activity?.let{
            it.runOnUiThread(
                Runnable {
                    participantCountTextView?.let { tv ->
                        tv.text = (text + 1).toString()
                    }
                }
            )
        }
    }

    private fun setFloatingHeaderVisibility(visibility: Int) {
        activity?.let{
            it.runOnUiThread(Runnable {
                infoHeaderView?.let { view ->
                    view.visibility = visibility
                }
            })
        }

    }

    private fun openShareDialogue() {
        Log.d(LOG_TAG, "Share button clicked!")
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, communicationCallingViewModel.getJoinId())
        sendIntent.putExtra(Intent.EXTRA_TITLE, "Group Call ID")
        sendIntent.type = "text/plain"
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun setLayoutComponentState(
        isMicrophoneMuted: Boolean,
        isCameraOn: Boolean,
        isCallHangUpOverLaid: Boolean
    ) {
        audioImageButton.isSelected = !isMicrophoneMuted
        videoImageButton.isSelected = isCameraOn
        callHangupOverlay.visibility = if (isCallHangUpOverLaid) View.VISIBLE else View.INVISIBLE
    }

    private fun openHangupDialog() {
        if (!callHangUpOverlaid) {
            callHangUpOverlaid = true
            callHangupOverlay.visibility = View.VISIBLE
        }
    }

    private fun closeHangupDialog() {
        if (callHangUpOverlaid) {
            callHangUpOverlaid = false
            callHangupOverlay.visibility = View.GONE
        }
    }

    private fun hangup() {
        Log.d(LOG_TAG, "Hangup button clicked!")
        if (localParticipantView != null) {
            localParticipantView.cleanUpVideoRendering()
            detachFromParentView(localParticipantView)
        }
        activity?.let{
            inCallServiceIntent?.let { intent ->
                it.stopService(intent)
            }
        }

        callHangupConfirmButton.isEnabled = false
        communicationCallingViewModel.hangupAsync()

        findNavController().popBackStack()
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

    private fun setupGridLayout() {
        gridLayout.removeAllViews()
        if (participantViewList.size <= 1) {
            gridLayout.setRowCount(1)
            gridLayout.setColumnCount(1)
            gridLayout.addView(
                createCellForGridLayout(
                    gridLayout.getMeasuredWidth(),
                    gridLayout.getMeasuredHeight()
                )
            )
        } else {
            gridLayout.setRowCount(2)
            gridLayout.setColumnCount(2)
            for (i in 0..3) {
                gridLayout.addView(
                    createCellForGridLayout(
                        gridLayout.getMeasuredWidth() / 2,
                        gridLayout.getMeasuredHeight() / 2
                    )
                )
            }
        }
    }

    private fun loadGridLayoutViews() {
        setupGridLayout()
        for (i in participantViewList.indices) {
            val participantView: ParticipantView = participantViewList[i]
            detachFromParentView(participantView)
            (gridLayout.getChildAt(i) as LinearLayout).addView(participantView, 0)
        }
    }

    private fun setLocalParticipantView() {
        Log.d(LOG_TAG, "setLocalParticipantView")
        detachFromParentView(localParticipantView)
        communicationCallingViewModel.cameraOn.value?.let { on ->
            if(on)
                localVideoViewContainer.visibility = View.VISIBLE
            else
                localVideoViewContainer.visibility = View.INVISIBLE
        }
        localParticipantView.setDisplayNameVisible(false)
        localVideoViewContainer.addView(localParticipantView)
        localVideoViewContainer.bringToFront()
        localVideoViewContainer.bringToFront()
    }

    private fun appendLocalParticipantView() {
        localParticipantViewGridIndex = participantViewList.size
        localParticipantView.setDisplayNameVisible(true)
        communicationCallingViewModel.cameraOn.value?.let {
            localParticipantView.setVideoDisplayed(it)
        }
        participantViewList.add(localParticipantView)
        localVideoViewContainer.visibility = View.INVISIBLE
    }

    private fun updateGridLayoutViews() {
        for (i in 0 until gridLayout.getChildCount()) {
            val wrap = gridLayout.getChildAt(i) as LinearLayout
            val preParticipantView: ParticipantView = wrap.getChildAt(0) as ParticipantView
            if (i >= participantViewList.size) {
                wrap.removeAllViews()
            } else {
                val newParticipantView: ParticipantView = participantViewList.get(i)
                if (preParticipantView !== newParticipantView) {
                    detachFromParentView(newParticipantView)
                    wrap.removeAllViews()
                    wrap.addView(newParticipantView, 0)
                }
            }
        }
    }

    private fun createCellForGridLayout(width: Int, height: Int): LinearLayout? {
        activity?.let {
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
}