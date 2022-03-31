package com.cattailsw.mediaplayer

import android.net.Uri
import android.os.Bundle
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
import com.cattailsw.mediaplayer.ui.theme.CMediaPlayerTheme

class MainActivity : ComponentActivity() {

    val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen({
                viewModel.openLocalFileBrowser()
            }, {
                viewModel.launchMedia(Uri.parse("random string"))
            })
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