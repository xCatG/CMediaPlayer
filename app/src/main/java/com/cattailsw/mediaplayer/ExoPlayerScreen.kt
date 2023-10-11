package com.cattailsw.mediaplayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.cattailsw.mediaplayer.ui.theme.CMediaPlayerTheme


@Composable
fun ExoPlayerScreen(
    player: Player,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onDisposeAction: () -> Unit = {},
    onToggleFullScreen: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current

    val playWhenReady by rememberSaveable { mutableStateOf(true) }
    var isFullScreen by rememberSaveable {
        mutableStateOf(true)
    }

    player.playWhenReady = playWhenReady

    CMediaPlayerTheme(darkTheme = true) {
        Surface(modifier = modifier.fillMaxSize()) {
            DisposableEffect(key1 = Unit, effect = {
                onDispose {
                    onDisposeAction()
                }
            })
            /**
             * TODO
             *
             * create playback control UI in compose
             *
             * control system UI show/hide
             * make systemUI show/hide following playback control?
             *
             *
             */
            AndroidView(
                factory = {
                    PlayerView(context).apply {
                        this.player = player
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            Column(
                modifier = Modifier.padding(
                    top = if (isFullScreen) {
                        40.dp
                    } else {
                        80.dp
                    },
                    bottom = if (isFullScreen) {
                        40.dp
                    } else {
                        80.dp
                    }
                )
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                IconButton(onClick = {
                    isFullScreen = !isFullScreen
                    onToggleFullScreen(isFullScreen)
                }) {
                    Icon(
                        imageVector = if (isFullScreen) {
                            Icons.Filled.KeyboardArrowDown
                        } else {
                            Icons.Filled.KeyboardArrowUp
                        }, contentDescription = "full screen toggle"
                    )
                }
            }
        }
    }
}