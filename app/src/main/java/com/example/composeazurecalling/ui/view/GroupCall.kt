package com.example.composeazurecalling.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.composeazurecalling.model.JoinCallConfig

@Composable
fun GroupCall(joinCallConfig: JoinCallConfig) {

    var isVideoChecked = joinCallConfig.isCameraOn
    var isMicChecked = !joinCallConfig.isMicrophoneMuted
    var inviteState by remember { mutableStateOf(false) }
    var callEndState by remember { mutableStateOf(false) }

    Column() {
        ConstraintLayout {
            val (button, text) = createRefs()

            Button(
                onClick = { /* Do something */ },
                modifier = Modifier.constrainAs(button) {
                    top.linkTo(parent.top, margin = 16.dp)
                }
            ) {
                Text("Button")
            }

            Text("Text", Modifier.constrainAs(text) {
                top.linkTo(button.bottom, margin = 16.dp)
            })
        }

        Text(text = "participants: ${joinCallConfig.displayName}")

        // TODO: Camera grid

        // TODO: Local camera

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
            IconButton(onClick = { inviteState = !inviteState }) {
                Icon(imageVector = Icons.Default.GroupAdd, contentDescription = "person add")
            }
            IconButton(onClick = { callEndState = !callEndState}) {
                Icon(imageVector = Icons.Default.CallEnd, contentDescription = "call end", tint = Color.Red)
            }
        }
    }
}