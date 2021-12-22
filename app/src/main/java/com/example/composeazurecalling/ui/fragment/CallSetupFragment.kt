package com.example.composeazurecalling.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.azure.android.communication.calling.CreateViewOptions
import com.azure.android.communication.calling.ScalingMode
import com.azure.android.communication.calling.VideoStreamRenderer
import com.azure.android.communication.calling.VideoStreamRendererView
import com.example.composeazurecalling.databinding.CallSetupFragmentBinding
import com.example.composeazurecalling.model.JoinCallConfig
import com.example.composeazurecalling.utils.ifLet
import com.example.composeazurecalling.viewmodel.CallSetupViewModel
import com.example.composeazurecalling.viewmodel.CommunicationCallingViewModel

class CallSetupFragment : Fragment() {

    companion object {
        fun newInstance() = CallSetupFragment()
    }

    private var rendererView: VideoStreamRenderer? = null
    private var previewVideo: VideoStreamRendererView? = null

    private val communicationCallingViewModel by activityViewModels<CommunicationCallingViewModel>()

    private lateinit var callSetupViewModel: CallSetupViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = CallSetupFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        callSetupViewModel = ViewModelProvider(this).get(CallSetupViewModel::class.java)
        binding.callSetupViewModel = callSetupViewModel
        binding.communicationCallingViewModel = communicationCallingViewModel

//        authorizationViewModel.profile.observe(viewLifecycleOwner, { profile ->
//            profile?.let {
//                callSetupViewModel.setDisplayName(it.fullname)
//            }
//        })

        val navController = this.findNavController()

//        val callType = CallSetupFragmentArgs.fromBundle(requireArguments()).CallType
//        val joinId = CallSetupFragmentArgs.fromBundle(requireArguments()).JoinId

        callSetupViewModel.isVideoChecked.observe(viewLifecycleOwner, { isVideoChecked ->
            Log.d("debug", "isVideoChecked: $isVideoChecked")
            if (isVideoChecked) {
                val localVideoStream = communicationCallingViewModel.getLocalVideoStream()
                rendererView = VideoStreamRenderer(localVideoStream, requireContext())
                previewVideo = rendererView!!.createView(CreateViewOptions(ScalingMode.CROP))
                binding.setupVideoLayout.addView(previewVideo, 0)
            } else {
                rendererView?.let {
                    it.dispose()
                    binding.setupVideoLayout.removeView(previewVideo)
                }
            }
        })

        callSetupViewModel.startCall.observe(viewLifecycleOwner, {
            if (it) {
                if (rendererView != null) {
                    rendererView!!.dispose()
                }

                ifLet(
                    callSetupViewModel.isMicChecked.value,
                    callSetupViewModel.isVideoChecked.value
                ) { (isMicChecked, isVideoChecked) ->
//                    val joinCallConfig = JoinCallConfig(joinId, isMicChecked == false, isVideoChecked == true,
//                        callSetupViewModel.displayName.value as String, callType)

//                    navController.navigate(
//                        CallSetupFragmentDirections.actionCallSetupFragmentToGroupCallFragment(joinCallConfig)
//                    )
                }
            }
        })

        communicationCallingViewModel.callState.observe(viewLifecycleOwner) { callState ->
            Log.d("debug", "callState: $callState")
        }

        return binding.root
    }

    override fun onStop() {
        super.onStop()
        if (rendererView != null) {
            rendererView!!.dispose()
        }
    }
}