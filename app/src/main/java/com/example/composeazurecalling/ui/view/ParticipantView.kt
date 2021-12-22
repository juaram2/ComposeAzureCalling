package com.example.composeazurecalling.ui.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.azure.android.communication.calling.*
import com.example.composeazurecalling.R


class ParticipantView(context: Context) : RelativeLayout(context) {
    // participant view properties
    private var renderer: VideoStreamRenderer? = null
    private var rendererView: VideoStreamRendererView? = null
    private var videoStreamId: String? = null

    // layout properties
    private val title: TextView
    private val defaultAvatar: ImageView
    private val videoContainer: ConstraintLayout
    private val activeSpeakerFrame: FrameLayout
    fun setVideoStream(remoteVideoStream: RemoteVideoStream?) {
        if (remoteVideoStream == null) {
            cleanUpVideoRendering()
            setVideoDisplayed(false)
            return
        }
        val newVideoStreamId = "RemoteVideoStream:" + remoteVideoStream.id
        if (newVideoStreamId == videoStreamId) {
            return
        }
        try {
            val videoRenderer =
                VideoStreamRenderer(remoteVideoStream, context)
            setVideoRenderer(videoRenderer)
            videoStreamId = newVideoStreamId
        } catch (e: CallingCommunicationException) {
            e.printStackTrace()
        }
    }

    fun setVideoStream(localVideoStream: LocalVideoStream?) {
        if (localVideoStream == null) {
            cleanUpVideoRendering()
            return
        }
        val newVideoStreamId =
            "LocalVideoStream:" + localVideoStream.source.id
        if (newVideoStreamId == videoStreamId) {
            return
        }
        try {
            val videoRenderer =
                VideoStreamRenderer(localVideoStream, context)
            setVideoRenderer(videoRenderer)
            videoStreamId = newVideoStreamId
        } catch (e: CallingCommunicationException) {
            e.printStackTrace()
        }
    }

    fun setDisplayName(displayName: String?) {
        title.text = displayName
    }

    fun setIsMuted(isMuted: Boolean) {
        val drawable = if (isMuted) ContextCompat.getDrawable(
            context,
            R.drawable.ic_fluent_mic_off_16_filled
        ) else null
        title.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
    }

    fun setIsSpeaking(isSpeaking: Boolean) {
        activeSpeakerFrame.visibility = if (isSpeaking) View.VISIBLE else View.INVISIBLE
    }

    fun setDisplayNameVisible(isDisplayNameVisible: Boolean) {
        title.visibility = if (isDisplayNameVisible) View.VISIBLE else View.INVISIBLE
    }

    fun setVideoDisplayed(isDisplayVideo: Boolean) {
        defaultAvatar.visibility = if (isDisplayVideo) View.INVISIBLE else View.VISIBLE
    }

    private fun setVideoRenderer(videoRenderer: VideoStreamRenderer) {
        renderer = videoRenderer
        rendererView = videoRenderer.createView(CreateViewOptions(ScalingMode.CROP))
        attachRendererView(rendererView)
    }

    private fun attachRendererView(rendererView: VideoStreamRendererView?) {
        this.rendererView = rendererView
        if (rendererView != null) {
            defaultAvatar.visibility = View.GONE
            detachFromParentView(rendererView)
            videoContainer.addView(rendererView, 0)
        } else {
            defaultAvatar.visibility = View.VISIBLE
        }
    }

    fun cleanUpVideoRendering() {
        disposeRenderView(rendererView)
        disposeRenderer(renderer)
        videoStreamId = null
    }

    private fun disposeRenderer(renderer: VideoStreamRenderer?) {
        renderer?.dispose()
    }

    private fun disposeRenderView(rendererView: VideoStreamRendererView?) {
        detachFromParentView(rendererView)
        rendererView?.dispose()
    }

    private fun detachFromParentView(rendererView: VideoStreamRendererView?) {
        if (rendererView != null && rendererView.parent != null) {
            (rendererView.parent as ViewGroup).removeView(rendererView)
        }
    }

    init {
        View.inflate(context, R.layout.participant_view, this)
        title = findViewById(R.id.display_name)
        defaultAvatar = findViewById(R.id.default_avatar)
        videoContainer = findViewById(R.id.video_container)
        activeSpeakerFrame = findViewById(R.id.active_speaker_frame)
    }
}