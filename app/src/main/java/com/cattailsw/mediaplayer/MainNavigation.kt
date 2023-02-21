package com.cattailsw.mediaplayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

object PlayerDestinations {
const val HOME = "main"
const val EXT_MEDIA = "mediaExternal"
    const val LOCAL_MEDIA = "localMedia"
    const val DBG_MEDIA = "media"
}

class MainNavigationAction(navController: NavController) {

}

@Composable
fun MainNavGraph(

modifier: Modifier = Modifier,
navController: NavController = rememberNavController(),
startDestination: String = PlayerDestinations.HOME,
) {

}