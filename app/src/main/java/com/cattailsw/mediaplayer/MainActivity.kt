package com.cattailsw.mediaplayer

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDeepLink
import androidx.navigation.compose.rememberNavController
import com.cattailsw.mediaplayer.ui.theme.CMediaPlayerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


private const val TAG = "MainActivity"

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
            viewModel.state.collectLatest {
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
                        // no op
                    }
                }
            }
        }

        setContent {
            val navController = rememberNavController()

            MainNavGraph(
                exoHolder = exoHolder, extDeepLink = deepLink, navController = navController,
                mainOpenAction = { viewModel.openLocalFileBrowser() },
                exoScreenBackAction = {
                    exoHolder.stop()
                    viewModel.backToMain()
                    navController.navigateUp()
                },
            )

            val state = viewModel.state.collectAsState()

            when (state.value) {
                MainState.Empty -> {
                    // this causes external media to end up on main first
                    //navController.navigate(PlayerDestinations.HOME)
                }

                is MainState.LaunchMedia -> {
                    val uri = (state.value as MainState.LaunchMedia).uri
                    exoHolder.replaceItem(uri)
                    Log.d(TAG, "got media intent for ${(state.value as MainState.LaunchMedia).uri}")
                    navController.navigate(PlayerDestinations.PLAYER)
                }

                else -> {
                    // noop as we might be handling a VIEW intent
                }
            }

            // keep screen on when player is in play state.
            val playerState = exoHolder.playerState.collectAsState()
            when (playerState.value) {
                PlayerState.Idle -> {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }

                PlayerState.Playing -> {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CMediaPlayerTheme {
        MainScreen({}, {})
    }
}
