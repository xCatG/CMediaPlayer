package com.cattailsw.mediaplayer

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavController.Companion.KEY_DEEP_LINK_INTENT
import androidx.navigation.NavDeepLink
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.exoplayer2.ExoPlayer

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
    exoHolder: ExoHolderVM,
    extDeepLink: NavDeepLink,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = PlayerDestinations.HOME,
    mainOpenAction: () -> Unit = {},
    exoScreenBackAction: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = PlayerDestinations.HOME) {
            MainScreen(
                openLocal = mainOpenAction,
                launch = {navController.navigate(PlayerDestinations.DBG_MEDIA)}
            )
        }
        composable(route = PlayerDestinations.EXT_MEDIA, deepLinks = listOf(extDeepLink)) {
            // all these parsing here doesn't feel right, figure out how to refactor this
            val origIntent : Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.arguments?.getParcelable(KEY_DEEP_LINK_INTENT, Intent::class.java)
            } else {
                it.arguments?.getParcelable<Intent>(KEY_DEEP_LINK_INTENT)
            }

            if (origIntent != null) {
                val dataUri: Uri = requireNotNull(origIntent.data)
                exoHolder.replaceItem(dataUri)
                ExoPlayerScreen(player = exoHolder.player, onBack = exoScreenBackAction)
            }
        }
        composable(route = PlayerDestinations.LOCAL_MEDIA) {
            ExoPlayerScreen(player = exoHolder.player, onBack = exoScreenBackAction)
        }
        composable(route = PlayerDestinations.DBG_MEDIA) {
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
}