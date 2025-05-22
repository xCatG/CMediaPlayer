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
    val playbackCount:Int = 0,
    @ColumnInfo(name = "title")
    val title: String? = null,
    @ColumnInfo(name = "duration")
    val duration: Long? = null,
    @ColumnInfo(name = "artist")
    val artist: String? = null,
    @ColumnInfo(name = "artwork_uri")
    val artworkUri: String? = null,
    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String? = null
)
