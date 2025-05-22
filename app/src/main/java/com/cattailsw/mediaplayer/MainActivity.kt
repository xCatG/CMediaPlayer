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
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import java.util.concurrent.TimeUnit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.cattailsw.mediaplayer.data.AppDatabase // Import AppDatabase
import com.cattailsw.mediaplayer.data.PlaybackHistory
import com.cattailsw.mediaplayer.ui.theme.CMediaPlayerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.net.toUri


private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            application,
            AppDatabase.getDatabase(application).playbackHistoryDao(),
            exoHolder // Pass ExoHolderVM instance
        )
    }
    private val exoHolder: ExoHolderVM by viewModels() // exoHolder needs to be initialized before viewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

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
            val mainViewModelState = viewModel.state.collectAsState() // Renamed to avoid conflict
            val playbackHistoryItems = viewModel.playbackHistoryFlow

            MainNavGraph(
                exoHolder = exoHolder,
                mainViewModel = viewModel, // Pass the viewModel
                extDeepLink = navDeepLink {
                    mimeType = "video/*"
                    action = Intent.ACTION_VIEW
                },
                navController = navController,
                // mainOpenAction = { viewModel.openLocalFileBrowser() }, // This will be handled by MainScreen now
                exoScreenBackAction = {
                    exoHolder.stop()
                    navController.navigateUp()
                },
                playbackHistoryItems = playbackHistoryItems // Pass the flow here
            )

            val state = mainViewModelState // Use the renamed state

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
    // launchPlayer: () -> Unit, // launchPlayer is not used directly here anymore
    onHistoryItemClick: (Uri) -> Unit // To handle item clicks
) {

    CMediaPlayerTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),//.safeDrawingPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 16.dp, start = 8.dp, end = 8.dp, bottom = 0.dp)
                    .systemBarsPadding().fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Button(onClick = openLocal) { // Modified to use onClick lambda
                    Text("Open Local File")
                }

                // Button to launch player is removed as it's not directly used in MainScreen now
                // It's part of the navigation flow when an item is selected or opened.

                PlaybackHistory(
                    playbackHistory = playbackHistoryItems,
                    onItemClick = onHistoryItemClick
                )
            }
        }
    }

}

@Composable
fun PlaybackHistory(
    playbackHistory: StateFlow<List<PlaybackHistory>>,
    modifier: Modifier = Modifier,
    onItemClick: (uri: Uri) -> Unit
) {
    val historyItems: List<PlaybackHistory> by playbackHistory.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.background(color=MaterialTheme.colorScheme.primaryContainer).fillMaxSize()
    ) {
        Text("Playback History", style=MaterialTheme.typography.titleLarge)
        if (historyItems.isEmpty()) {
            Text("No Playback History", style=MaterialTheme.typography.bodyMedium, modifier=Modifier.padding(16.dp))
        } else {
            LazyColumn {
                items(historyItems) { item ->
                    HistoryItem(
                        item = item,
                        modifier = Modifier.clickable(onClick = { onItemClick(item.uri) })
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    item: PlaybackHistory,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.thumbnailPath?.let { File(it) })
                .crossfade(true)
                .build(),
            placeholder = painterResource(R.drawable.ic_default_thumbnail_placeholder),
            error = painterResource(R.drawable.ic_default_thumbnail_placeholder),
            contentDescription = "Video thumbnail",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.3f) // Adjusted width for better thumbnail visibility
                .aspectRatio(16f / 9f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Column(modifier = Modifier
            .padding(start = 8.dp)
            .fillMaxWidth()) {
            Text(
                text = if (!item.title.isNullOrBlank()) item.title else item.uri.pathSegments.lastOrNull() ?: item.uri.toString(), // Fallback to last path segment or full URI
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2 // Allow for longer titles
            )
            if (!item.artist.isNullOrBlank()) {
                Text(
                    text = item.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            val durationString = formatDuration(item.duration)
            if (durationString.isNotBlank()) {
                Text(
                    text = "Duration: $durationString",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "Played: ${item.playbackCount} times",
                style = MaterialTheme.typography.bodySmall
            )
             Text(
                // Displaying lastTimestamp in a more readable way could be added later
                text = "Last Access: ${item.lastTimestamp}", // Simplified for now
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

fun formatDuration(millis: Long?): String {
    if (millis == null || millis <= 0) return "" // Return empty or "--:--" if you prefer
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val previewHistoryItems = listOf(
        PlaybackHistory(
            uri = "preview_uri_1".toUri(),
            lastTimestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1),
            playbackCount = 2,
            title = "Big Buck Bunny - A very long title that might wrap to two lines",
            duration = TimeUnit.MINUTES.toMillis(9) + TimeUnit.SECONDS.toMillis(56),
            artist = "Blender Foundation",
            thumbnailPath = null // Actual file path won't render in @Preview easily, Coil will show error/placeholder
        ),
        PlaybackHistory(
            uri = "preview_uri_2/video.mp4".toUri(), // Example with path segment
            lastTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2),
            playbackCount = 1,
            title = "Elephants Dream",
            duration = TimeUnit.MINUTES.toMillis(10) + TimeUnit.SECONDS.toMillis(53),
            artist = "Blender Foundation",
            thumbnailPath = null
        ),
        PlaybackHistory(
            uri = "preview_uri_3".toUri(),
            lastTimestamp = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30),
            playbackCount = 5,
            title = null, // Test title fallback to URI's last path segment
            duration = 0,  // Test blank duration
            artist = "Another Artist",
            thumbnailPath = null
        )
    )
    val list = remember { MutableStateFlow(previewHistoryItems) }

    CMediaPlayerTheme {
        // MainScreen now expects onHistoryItemClick
        MainScreen(list, {}, onItemClick = {})
    }
}
