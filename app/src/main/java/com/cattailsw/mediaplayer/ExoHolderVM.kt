package com.cattailsw.mediaplayer

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.CompositeMediaSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DataSchemeDataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.util.EventLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

sealed class PlayerState {
    object Idle: PlayerState()
    object Playing: PlayerState()
}

class ExoHolderVM(
    private val okHttpClient: OkHttpClient = DIGraph.okHttpClient,
) : ViewModel() {
    private var _exoPlayer: ExoPlayer? = null
    private var currentMediaItem: MediaItem? = null

    private val _exoPlayerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState>
        get() = _exoPlayerState

    val player: Player
        get() = requireNotNull(_exoPlayer)

    fun initPlayer(context: Context) {
        if (_exoPlayer == null) {
            val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
            val defaultDataSource = DefaultDataSource.Factory(context, okHttpFactory)
            val player = ExoPlayer.Builder(context.applicationContext)
                .setMediaSourceFactory(DefaultMediaSourceFactory(context.applicationContext)
                    .setDataSourceFactory(defaultDataSource)
                )
                .build()
            player.addAnalyticsListener(EventLogger())
            player.addListener(object: Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    viewModelScope.launch {
                        if (isPlaying) {
                            _exoPlayerState.emit(PlayerState.Playing)
                        } else {
                            _exoPlayerState.emit(PlayerState.Idle)
                        }
                    }
                }
            })
            _exoPlayer = player
        }
    }

    // we currently only support opening one item
    fun replaceItem(uri: Uri, mime:String? = null) {
        val builder = MediaItem.Builder().setUri(uri)
        if (mime != null) {
            builder.setMimeType(mime)
        }

        val mediaItem = builder.build()
        if (currentMediaItem != mediaItem) {

            _exoPlayer?.let { player ->
                player.clearMediaItems()
                player.addMediaItem(mediaItem)
                player.prepare()
            }
            currentMediaItem = mediaItem
        } else {
            Log.d("ExoHolderVM", "got the same media item, ignoring replace call")
        }
    }

    fun stop() {
        _exoPlayer?.stop()
    }

    fun releasePlayer() {
        _exoPlayer?.apply {
            release()
        }
        _exoPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}