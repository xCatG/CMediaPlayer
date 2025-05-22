package com.cattailsw.mediaplayer

import android.app.Application // Added Application import
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cattailsw.mediaplayer.data.PlaybackHistory
import com.cattailsw.mediaplayer.data.PlaybackHistoryDao
import com.cattailsw.mediaplayer.util.ThumbnailExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import androidx.media3.common.C // For C.TIME_UNSET

private const val TAG = "MainViewModel"
class MainViewModel(
    private val application: Application,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val exoHolderVM: ExoHolderVM // Added ExoHolderVM
): ViewModel() {
    val state : StateFlow<MainState>
        get() = _state
    private val _state = MutableStateFlow<MainState>(MainState.Empty)

    private val _playbackHistoryFlow = MutableStateFlow<List<PlaybackHistory>>(emptyList())
    val playbackHistoryFlow: StateFlow<List<PlaybackHistory>> = _playbackHistoryFlow.asStateFlow()

    init {
        viewModelScope.launch {
            playbackHistoryDao.getAllHistory().collect { history ->
                _playbackHistoryFlow.value = history
            }
        }

        // Collect metadata from ExoHolderVM
        viewModelScope.launch {
            exoHolderVM.mediaMetadataFlow.filterNotNull().collect { (uri, metadata) ->
                Log.d(TAG, "Collected metadata for URI: $uri, Title: ${metadata.title}")
                val historyItem = playbackHistoryDao.getHistoryItem(uri.toString())
                if (historyItem != null) {
                    var updated = false
                    var itemToUpdate = historyItem

                    if (itemToUpdate.title.isNullOrEmpty() && !metadata.title.isNullOrEmpty()) {
                        itemToUpdate = itemToUpdate.copy(title = metadata.title.toString())
                        updated = true
                    } else if (itemToUpdate.title.isNullOrEmpty() && !metadata.displayTitle.isNullOrEmpty()) {
                        // Fallback to displayTitle
                        itemToUpdate = itemToUpdate.copy(title = metadata.displayTitle.toString())
                        updated = true
                    }

                    if (itemToUpdate.artist.isNullOrEmpty() && !metadata.artist.isNullOrEmpty()) {
                        itemToUpdate = itemToUpdate.copy(artist = metadata.artist.toString())
                        updated = true
                    }

                    if (itemToUpdate.artworkUri.isNullOrEmpty() && metadata.artworkUri != null) {
                        itemToUpdate = itemToUpdate.copy(artworkUri = metadata.artworkUri.toString())
                        updated = true
                    }
                    
                    // Get duration from player if not set or zero
                    val playerDuration = exoHolderVM.player.duration
                    if (itemToUpdate.duration == null || itemToUpdate.duration == 0L) {
                        if (playerDuration != C.TIME_UNSET && playerDuration > 0) {
                            itemToUpdate = itemToUpdate.copy(duration = playerDuration)
                            updated = true
                            Log.d(TAG, "Updated duration for $uri to $playerDuration from player.duration")
                        }
                    }


                    if (updated) {
                        Log.d(TAG, "Updating history item $uri with new metadata.")
                        playbackHistoryDao.insertOrUpdate(itemToUpdate)
                    }
                }
            }
        }
    }

    suspend fun addOrUpdatePlaybackHistory(uri: Uri) {
        val existingItem = playbackHistoryDao.getHistoryItem(uri.toString())
        var itemToSave: PlaybackHistory

        if (existingItem != null) {
            itemToSave = existingItem.copy(
                lastTimestamp = System.currentTimeMillis(),
                playbackCount = existingItem.playbackCount + 1
            )
        } else {
            itemToSave = PlaybackHistory(
                uri = uri,
                lastTimestamp = System.currentTimeMillis(),
                playbackCount = 1
            )
        }

        // Attempt to extract thumbnail if path is missing
        if (itemToSave.thumbnailPath.isNullOrEmpty()) {
            // Launch as a separate task, but wait for it before final DB update
            // to ensure thumbnail is included in the displayed history if possible.
            // If this causes too much delay, it could be launched independently
            // and update the DB in a second step.
            try {
                val thumbnailPath = ThumbnailExtractor.extractThumbnail(application.applicationContext, uri)
                if (thumbnailPath != null) {
                    itemToSave = itemToSave.copy(thumbnailPath = thumbnailPath)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during thumbnail extraction for $uri", e)
                // Proceed without thumbnail if extraction fails
            }
        }
        playbackHistoryDao.insertOrUpdate(itemToSave)
    }

    suspend fun deletePlaybackHistoryItem(uri: Uri) {
        playbackHistoryDao.deleteHistoryItem(uri.toString())
    }


    fun openLocalFileBrowser() {
        viewModelScope.launch {
            _state.emit(MainState.OpenFile)
        }
    }

    private fun launchMedia(uri: Uri) {
        viewModelScope.launch {
            addOrUpdatePlaybackHistory(uri) // Add/Update history when media is launched
            _state.emit(MainState.LaunchMedia(uri))
        }
    }

    fun handleResult(uri: Uri?) {
        viewModelScope.launch {
            if (uri == null) {
                _state.emit(MainState.ErrorOpen)
            } else {
                launchMedia(uri)
            }
        }
    }

    fun handleFileOpenResult(resultCode:Int, data: Intent?) {
        Log.d(TAG, "got intent data= ${data?.data} and ${data?.action}")

        viewModelScope.launch {
            // check if data.data starts with content:// ?
            if (data == null || resultCode != RESULT_OK || data.data == null) {
                _state.emit(MainState.ErrorOpen)
                _state.emit(MainState.Empty)
            } else {
                launchMedia(data.data!!)
            }
        }
    }

}

sealed class MainState {
    object Empty: MainState()
    object ErrorOpen: MainState()
    object OpenFile: MainState()
    data class LaunchMedia(val uri: Uri): MainState()
}

// Simple ViewModel Factory for MainViewModel
class MainViewModelFactory(
    private val application: Application,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val exoHolderVM: ExoHolderVM // New parameter
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, playbackHistoryDao, exoHolderVM) as T // Pass to constructor
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
