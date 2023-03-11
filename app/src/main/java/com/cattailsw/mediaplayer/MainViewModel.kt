package com.cattailsw.mediaplayer

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"
class MainViewModel: ViewModel() {
    val state : StateFlow<MainState>
        get() = _state
    private val _state = MutableStateFlow<MainState>(MainState.Empty)

    fun openLocalFileBrowser() {
        viewModelScope.launch {
            // open SAF to read some kind of files
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type="video/*"
            }
            _state.emit(MainState.OpenFile(intent))
        }
    }

    private fun launchMedia(uri: Uri) {
        viewModelScope.launch {
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
    data class OpenFile(val intentToLaunch: Intent): MainState()
    data class LaunchMedia(val uri: Uri): MainState()
}