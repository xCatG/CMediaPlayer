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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
    private var _exoPlayer: ExoPlayer? = null
    private var currentMediaItem: MediaItem? = null

    val player: Player
        get() = requireNotNull(_exoPlayer)

    fun initPlayer(context: Context) {
        if (_exoPlayer == null) {
            val player = ExoPlayer.Builder(context.applicationContext).build()
            player.addAnalyticsListener(EventLogger())
            _exoPlayer = player
        }
    }

    // we currently only support opening one item
    fun replaceItem(uri: Uri, mime:String? = null) {
        val builder = MediaItem.Builder().setUri(uri)
        if (mime != null) {
            builder.setMimeType(mime)
        }

        val mediaItem = builder.build()
        if (currentMediaItem != mediaItem) {

            _exoPlayer?.let { player ->
                player.clearMediaItems()
                player.addMediaItem(mediaItem)
                player.prepare()
            }
            currentMediaItem = mediaItem
        } else {
            Log.d("ExoHolderVM", "got the same media item, ignoring replace call")
        }
    }

    fun stop() {
        _exoPlayer?.stop()
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

        lifecycleScope.launch {
            viewModel._state.collectLatest {
                when (it) {
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
                            onBack = {
                                exoHolder.releasePlayer() // probably unnecessary but just in case
                                navController.navigateUp()
                            }
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
                        onBack = {
                            exoHolder.stop()
                            navController.navigateUp()
                        }
                    )
                }
                composable(route = "media") {
                    val dataUri = Uri.parse("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4")
                    exoHolder.replaceItem(dataUri)
                    ExoPlayerScreen(
                        player = exoHolder.player,
                        onBack = {
                            exoHolder.stop()
                            navController.navigateUp()
                                 },
                    )
                }
            }

            val state = viewModel._state.collectAsState()

            when(state.value) {
                MainState.Empty -> {
                    // this causes external media to end up on main first
                    // navController.navigate("main")
                }
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
    onBack: () -> Unit = {},
    onDisposeAction: () -> Unit = {},
) {
    val context = LocalContext.current

    val playWhenReady by rememberSaveable { mutableStateOf(true) }

    player.playWhenReady = playWhenReady

    CMediaPlayerTheme(darkTheme = true) {
        Surface {
            Box(modifier = modifier.fillMaxSize()) {
                DisposableEffect(key1 = Unit, effect = {
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
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                }
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