package com.cattailsw.mediaplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: PlaybackHistory)

    @Query("SELECT * FROM playback_history ORDER BY lastTimestamp DESC")
    fun getAllHistory(): Flow<List<PlaybackHistory>>

    @Query("SELECT * FROM playback_history WHERE uri = :uriString")
    suspend fun getHistoryItem(uriString: String): PlaybackHistory?

    @Query("DELETE FROM playback_history WHERE uri = :uriString")
    suspend fun deleteHistoryItem(uriString: String)

    @Update
    suspend fun updateTimestamp(item: PlaybackHistory) // Consider if insertOrUpdate already covers this
}
