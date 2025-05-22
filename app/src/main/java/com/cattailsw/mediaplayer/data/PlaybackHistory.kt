package com.cattailsw.mediaplayer.data

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistory(
    @PrimaryKey
    @ColumnInfo(name = "uri")
    val uri: Uri,
    @ColumnInfo(name = "lastTimestamp")
    val lastTimestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "playbackCount")
    val playbackCount:Int = 0
)
