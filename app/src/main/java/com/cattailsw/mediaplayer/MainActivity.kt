package com.cattailsw.mediaplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.cattailsw.mediaplayer.ui.theme.CMediaPlayerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val exoHolder: ExoHolderVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exoHolder.initPlayer(applicationContext)

        lifecycleScope.launch {
            // keep screen on when player is in play state.
            exoHolder.playerState.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
                .collectLatest {
                    when (it) {
                        PlayerState.Idle -> {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }

                        PlayerState.Playing -> {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                }
        }

        setContent {
            // TODO: support non-GMS devices by checking if this is not available and fall back
            // to SAF document opening?
            val pickMedia = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia(),
                onResult = { uri ->
                    viewModel.handleResult(uri)
                }
            )

            val navController = rememberNavController()

            MainNavGraph(
                exoHolder = exoHolder, extDeepLink = navDeepLink {
                    mimeType = "video/*"
                    action = Intent.ACTION_VIEW
                },
                navController = navController,
                mainOpenAction = { viewModel.openLocalFileBrowser() },
                exoScreenBackAction = {
                    exoHolder.stop()
                    // this crashes if we are rendering external media, need to figure out why
                    navController.navigateUp()
                },
            )

            val state = viewModel.state.collectAsState()

            when (state.value) {
                MainState.Empty -> {
                    // this causes external media to end up on main first
                    // navController.navigate("main")
                }

                is MainState.OpenFile -> {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                }

                is MainState.LaunchMedia -> {
                    val uri = (state.value as MainState.LaunchMedia).uri
                    exoHolder.replaceItem(uri)
                    Log.d(TAG, "got media intent for ${(state.value as MainState.LaunchMedia).uri}")
                    navController.navigate(PlayerDestinations.LOCAL_MEDIA)
                }

                MainState.ErrorOpen -> {
                    Toast.makeText(this@MainActivity, "open failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    playbackHistoryItems: StateFlow<List<PlaybackHistory>>,
    openLocal: () -> Unit,
    launch: () -> Unit
) {

    CMediaPlayerTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
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

                PlaybackHistory(playbackHistoryItems)
            }
        }
    }

}

@Composable
fun PlaybackHistory(
    playbackHistory: StateFlow<List<PlaybackHistory>>,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val historyItems: List<PlaybackHistory> by playbackHistory.collectAsState()

    // TODO add playback history view here; this should be a lazycolumn showing thumbnails from most
    // recent playback
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.background(color=MaterialTheme.colorScheme.secondary)
    ) {
        Text("Playback History")
        LazyColumn {
            items(historyItems) { item ->
                Text("item: ${item.uri}")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val list = remember { MutableStateFlow(listOf<PlaybackHistory>(
        PlaybackHistory(Uri.parse("test"), 1L, 0),
        PlaybackHistory(Uri.parse("test2"), 2L, 1),
        PlaybackHistory(Uri.parse("test3"), 3L, 0)
    )) }

    CMediaPlayerTheme {
        MainScreen(list, {}, {})
    }
}
