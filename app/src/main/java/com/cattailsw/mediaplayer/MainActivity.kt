package com.cattailsw.mediaplayer

import android.content.Intent.ACTION_VIEW
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDeepLink
import androidx.navigation.compose.rememberNavController
import com.cattailsw.mediaplayer.ui.theme.CMediaPlayerTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val exoHolder: ExoHolderVM by viewModels()

    private fun toggleFullScreen(isFullScreen: Boolean) {
        val rootView: View? = findViewById(android.R.id.content)
        if (rootView != null) {
            val controller = WindowInsetsControllerCompat(window,rootView)
            if (isFullScreen) {
                controller.hide(WindowInsetsCompat.Type.statusBars())
                controller.hide(WindowInsetsCompat.Type.navigationBars())
            } else {
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        exoHolder.initPlayer(applicationContext)
        val deepLink: NavDeepLink = NavDeepLink.Builder().setAction(ACTION_VIEW)
            .setMimeType("video/*")
            .build()

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
                exoHolder = exoHolder, extDeepLink = deepLink, navController = navController,
                mainOpenAction = { viewModel.openLocalFileBrowser() },
                exoScreenBackAction = {
                    exoHolder.stop()
                    // this crashes if we are rendering external media, need to figure out why
                    navController.navigateUp()
                },
                onFullScreenToggle = ::toggleFullScreen
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
