package com.cattailsw.mediaplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.cattailsw.mediaplayer.ui.theme.CMediaPlayerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.state.collectLatest {
                when(it) {
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
                    is MainState.LaunchMedia -> TODO()
                }
            }
        }

        setContent {
            MainScreen({
                viewModel.openLocalFileBrowser()
            }, {
                viewModel.launchMedia(Uri.parse("random string"))
            })
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
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CMediaPlayerTheme {
        MainScreen({}, {})
    }
}