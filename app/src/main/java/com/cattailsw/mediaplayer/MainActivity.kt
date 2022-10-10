package com.cattailsw.mediaplayer

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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavController
import androidx.navigation.NavController.Companion.KEY_DEEP_LINK_INTENT
import androidx.navigation.NavDeepLink
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.findNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.cattailsw.mediaplayer.ui.theme.CMediaPlayerTheme
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO get intent and check for Uri
        if (intent.action?.compareTo(Intent.ACTION_VIEW) == 0) {
            val uri = intent.data
            Log.d("XXXX", "got intent with data $uri")
            Log.d("XXXX", "--> intent is ${intent.toString()}")
        }

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
                    is MainState.LaunchMedia -> {
                        val uri = it.uri
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
                        val dataUri = origIntent.data
                        ExoPlayerScreen(mediaUri = dataUri)
                    } else {
                        ExoPlayerScreen()
                    }
                }
                composable(
                    route = "localMedia",
                    arguments = listOf(playerNamedNavArgument),
                ) {
                    println(it.arguments)
                    ExoPlayerScreen()
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
    modifier: Modifier = Modifier,
    mediaUri: Uri? = null,
) {
    val context = LocalContext.current
    val mp3Url = mediaUri
        ?: Uri.parse("https://storage.googleapis.com/exoplayer-test-media-0/Jazz_In_Paris.mp3")
    val mediaItem = MediaItem.fromUri(mp3Url)
    val playWhenReady by remember { mutableStateOf(true) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {

            setMediaItem(mediaItem)
            prepare()
            this.playWhenReady = playWhenReady
        }
    }

    Box(modifier = modifier) {
        DisposableEffect(key1 = Unit, effect = {
            onDispose { exoPlayer.release() }
        })
        AndroidView(factory = {
            StyledPlayerView(context).apply {
                this.player = exoPlayer
            }
        })
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CMediaPlayerTheme {
        MainScreen({}, {})
    }
}