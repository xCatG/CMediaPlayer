package com.cattailsw.mediaplayer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.cattailsw.mediaplayer.databinding.FragmentExoPlayerBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem

class ExoPlayerFragment : Fragment() {

    companion object {
        fun newInstance() = ExoPlayerFragment()
    }

    private var binding: FragmentExoPlayerBinding? = null

    private val viewModel: ExoPlayerViewModel by viewModels<ExoPlayerViewModel>()

    private var player: ExoPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentExoPlayerBinding.inflate(inflater)
        this.binding = binding

        return binding.videoView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (player == null) {
            initPlayer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    private fun initPlayer() {
        val localPlayer = ExoPlayer.Builder(requireContext()).build() .also { exoPlayer ->
            checkNotNull(binding).videoView.player = exoPlayer
            val mp3Url = "https://storage.googleapis.com/exoplayer-test-media-0/Jazz_In_Paris.mp3"
            val mediaItem = MediaItem.fromUri(mp3Url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.playWhenReady = playWhenReady
            exoPlayer.seekTo(curMediaItemIdx, playbackPosition)

            exoPlayer.prepare()
        }

        player = localPlayer

        viewModel.setPlayer(localPlayer)

    }
    private var playWhenReady = true
    private var curMediaItemIdx = 0
    private var playbackPosition = 0L

    private fun releasePlayer() {
        player?.run {
            playbackPosition = this.currentPosition
            curMediaItemIdx = this.currentMediaItemIndex
            playWhenReady = this.playWhenReady
            release()
        }
        player = null
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        val windowInsetsController = view?.let { ViewCompat.getWindowInsetsController(it) } ?: return
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
//
//        viewBinding.videoView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
//                or View.SYSTEM_UI_FLAG_FULLSCREEN
//                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }


}