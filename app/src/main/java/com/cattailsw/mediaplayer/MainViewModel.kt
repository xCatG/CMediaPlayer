package com.cattailsw.mediaplayer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel(): ViewModel() {

    fun openLocalFileBrowser() {
        viewModelScope.launch {
            // open SAF to read some kind of files
        }
    }

    fun launchMedia(uri: Uri) {
        viewModelScope.launch {
            // hand media uri off to player activity?
        }
    }

}