package com.cattailsw.mediaplayer

import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.analytics.AnalyticsListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

const val POSITION_NONE = -1L

class ExoPlayerViewModel : ViewModel() {
    private val _playPosition = MutableStateFlow<Long>(POSITION_NONE)
    val playPosition: StateFlow<Long>
        get() = _playPosition

    private var _player: Player? = null

    private var exoAnalyticsListener: AnalyticsListener? = null

    fun playPause() {
        _player?.play()
    }

    fun setPlayer(player: Player) {
        _player = player.also { exoPlayer ->
            // no op
            exoAnalyticsListener?.let {

            }
        }
    }

    fun releasePlayer() {
        _player?.let { player ->
            // some things here
            player.release()
        }
        exoAnalyticsListener = null
        _player = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}