package com.cattailsw.mediaplayer

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainViewModel(): ViewModel() {

    var state = MutableStateFlow<MainState>(MainState.Empty)

    fun openLocalFileBrowser() {
        viewModelScope.launch {
            // open SAF to read some kind of files
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type="video/*"
            }
            state.emit(MainState.OpenFile(intent))
        }
    }

    fun launchMedia(uri: Uri) {
        viewModelScope.launch {
            state.emit(MainState.LaunchMedia(uri))
        }
    }

    fun handleFileOpenResult(resultCode:Int, data: Intent?) {
        Log.d("XXXX", "got intent data= ${data?.data} and ${data?.action}")

        viewModelScope.launch {
            // check if data.data starts with content:// ?
            if (data == null || resultCode != RESULT_OK || data.data == null) {
                state.emit(MainState.ErrorOpen)
                state.emit(MainState.Empty)
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