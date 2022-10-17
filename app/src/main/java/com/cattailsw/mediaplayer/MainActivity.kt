package com.cattailsw.mediaplayer

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavController.Companion.KEY_DEEP_LINK_INTENT
import androidx.navigation.NavDeepLink
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cattailsw.mediaplayer.ui.theme.CMediaPlayerTheme
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.util.EventLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class ExoHolderVM() : ViewModel() {
    var _exoPlayer: ExoPlayer? = null

    val player: Player
        get() = requireNotNull(_exoPlayer)

    fun initPlayer(context: Context) {
        val player = ExoPlayer.Builder(context.applicationContext).build()

        player.addAnalyticsListener(EventLogger())

        _exoPlayer = player
    }

    // we currently only support opening one item
    fun replaceItem(uri: Uri, mime:String? = null) {
        val builder = MediaItem.Builder().setUri(uri)
        if (mime != null) {
            builder.setMimeType(mime)
        }

        val mediaItem = builder.build()

        _exoPlayer?.let { player ->
            player.clearMediaItems()
            player.addMediaItem(mediaItem)
            player.prepare()
        }
    }

    fun pause() {
        _exoPlayer?.pause()
    }

    fun releasePlayer() {
        _exoPlayer?.apply {
            release()
        }
        _exoPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val exoHolder: ExoHolderVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exoHolder.initPlayer(applicationContext)
        val deepLink: NavDeepLink = NavDeepLink.Builder().setAction(ACTION_VIEW)
            .setMimeType("video/*")
            .build()

        val playerNamedNavArgument: NamedNavArgument = navArgument("playerViewArg") {
            type = NavType.StringType
            nullable = false
        }

        lifecycleScope.launch {
            viewModel.state.collectLatest {
                when (it) {
                    MainState.Empty -> {
                        // noop
                    }
                    is MainState.OpenFile -> {
                        with(Dispatchers.Main) {
                            startActivityForResult(it.intentToLaunch, 42)
                        }
                    }
                    MainState.ErrorOpen -> {
                        Toast.makeText(this@MainActivity, "open failed", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        // nooop
                    }
                }
            }
        }

        setContent {
            val navController = rememberNavController()

            NavHost(navController, startDestination = "main") {
                composable("main") {
                    MainScreen(
                        {
                            viewModel.openLocalFileBrowser()
                        }, {
                            navController.navigate("media")
                        })
                }
                composable(
                    route = "mediaExternal",
                    deepLinks = listOf(deepLink)
                ) {
                    val origIntent: Intent? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            it.arguments?.getParcelable(KEY_DEEP_LINK_INTENT, Intent::class.java)
                        } else {
                            it.arguments?.getParcelable<Intent>(KEY_DEEP_LINK_INTENT)
                        }

                    if (origIntent != null) {
                        val dataUri: Uri = requireNotNull(origIntent.data)
                        exoHolder.replaceItem(dataUri)
                        ExoPlayerScreen(
                            player = exoHolder.player,
                            // In order to support rotate we probably don't want to call release
                            // unless we are really exiting?
                            onDisposeAction = exoHolder::releasePlayer
                        )
                    } else {
                        //ExoPlayerScreen()
                    }
                }
                composable(
                    route = "localMedia",
                ) {
                    ExoPlayerScreen(
                        player = exoHolder.player,
                        // In order to support rotate we probably don't want to call release
                        // unless we are really exiting?
                        onDisposeAction = exoHolder::releasePlayer
                    )
                }
            }

            val state = viewModel.state.collectAsState()

            when(state.value) {
                is MainState.LaunchMedia -> {
                    val uri = (state.value as MainState.LaunchMedia).uri
                    exoHolder.replaceItem(uri)
                    Log.d("XXXX", "got media intent for ${(state.value as MainState.LaunchMedia).uri}")
                    navController.navigate("localMedia")
                }
                else -> {
                    // noop as we might be handling a VIEW intent
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42) {
            viewModel.handleFileOpenResult(resultCode, data)
        }
    }
}

@Composable
fun MainScreen(
    openLocal: () -> Unit,
    launch: () -> Unit
) {
    CMediaPlayerTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Button(openLocal) {
                    Text("Open Local File")
                }

                Button(
                    launch
                ) {
                    Text("launch player")
                }
            }
        }
    }

}

@Composable
fun ExoPlayerScreen(
    player: Player,
    modifier: Modifier = Modifier,
    onDisposeAction: () -> Unit = {},
) {
    val context = LocalContext.current

    val playWhenReady by remember { mutableStateOf(true) }

    player.playWhenReady = playWhenReady

    CMediaPlayerTheme(darkTheme = true) {
        Surface {
            Box(modifier = modifier.fillMaxSize()) {
                DisposableEffect(key1 = Unit, effect = {
                    // might not want this
                    onDispose {
                        onDisposeAction()
                    }
                })
                AndroidView(
                    factory = {
                        StyledPlayerView(context).apply {
                            this.player = player
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CMediaPlayerTheme {
        MainScreen({}, {})
    }
}