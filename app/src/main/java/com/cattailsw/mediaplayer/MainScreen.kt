package com.cattailsw.mediaplayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cattailsw.mediaplayer.ui.theme.CMediaPlayerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    openLocal: () -> Unit,
    launch: () -> Unit
) {
    CMediaPlayerTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "I am a Video Player app",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            modifier = Modifier.safeDrawingPadding()
        ) { innerPadding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(innerPadding).fillMaxWidth()
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
