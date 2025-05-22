package com.cattailsw.mediaplayer

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cattailsw.mediaplayer.data.PlaybackHistory
import com.cattailsw.mediaplayer.data.PlaybackHistoryDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"
class MainViewModel(private val playbackHistoryDao: PlaybackHistoryDao): ViewModel() {
    val state : StateFlow<MainState>
        get() = _state
    private val _state = MutableStateFlow<MainState>(MainState.Empty)

    // private val db = AppDatabase.getDatabase(application) // Removed
    // private val playbackHistoryDao = db.playbackHistoryDao() // Removed

    private val _playbackHistoryFlow = MutableStateFlow<List<PlaybackHistory>>(emptyList())
    val playbackHistoryFlow: StateFlow<List<PlaybackHistory>> = _playbackHistoryFlow.asStateFlow()

    init {
        viewModelScope.launch {
            playbackHistoryDao.getAllHistory().collect { history ->
                _playbackHistoryFlow.value = history
            }
        }
    }

    suspend fun addOrUpdatePlaybackHistory(uri: Uri) {
        val existingItem = playbackHistoryDao.getHistoryItem(uri.toString())
        if (existingItem != null) {
            val updatedItem = existingItem.copy(
                lastTimestamp = System.currentTimeMillis(),
                playbackCount = existingItem.playbackCount + 1
            )
            playbackHistoryDao.insertOrUpdate(updatedItem)
        } else {
            val newItem = PlaybackHistory(
                uri = uri,
                lastTimestamp = System.currentTimeMillis(),
                playbackCount = 1
            )
            playbackHistoryDao.insertOrUpdate(newItem)
        }
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
    private val playbackHistoryDao: PlaybackHistoryDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(playbackHistoryDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
