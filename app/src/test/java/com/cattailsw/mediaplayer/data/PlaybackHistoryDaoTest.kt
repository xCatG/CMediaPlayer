package com.cattailsw.mediaplayer.data

import android.content.Context
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class PlaybackHistoryDaoTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var dao: PlaybackHistoryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // For testing purposes only
            .build()
        dao = database.playbackHistoryDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertOrUpdate_insertsNewItem() = runTest {
        val uriString = "content://test/1"
        val uri = Uri.parse(uriString)
        val currentTime = System.currentTimeMillis()
        val newItem = PlaybackHistory(
            uri = uri,
            lastTimestamp = currentTime,
            playbackCount = 1,
            title = "Test Title 1",
            duration = 120000L,
            artist = "Test Artist 1",
            artworkUri = "content://artwork/1",
            thumbnailPath = "/path/to/thumb1.jpg"
        )

        dao.insertOrUpdate(newItem)

        val retrievedItem = dao.getHistoryItem(uriString)
        assertThat(retrievedItem).isNotNull()
        assertThat(retrievedItem?.uri).isEqualTo(uri)
        assertThat(retrievedItem?.playbackCount).isEqualTo(1)
        assertThat(retrievedItem?.lastTimestamp).isEqualTo(currentTime)
        assertThat(retrievedItem?.title).isEqualTo("Test Title 1")
        assertThat(retrievedItem?.duration).isEqualTo(120000L)
        assertThat(retrievedItem?.artist).isEqualTo("Test Artist 1")
        assertThat(retrievedItem?.artworkUri).isEqualTo("content://artwork/1")
        assertThat(retrievedItem?.thumbnailPath).isEqualTo("/path/to/thumb1.jpg")
    }

    @Test
    fun insertOrUpdate_updatesExistingItem() = runTest {
        val uriString = "content://test/2"
        val uri = Uri.parse(uriString)
        val initialTimestamp = System.currentTimeMillis() - 1000
        val item = PlaybackHistory(
            uri = uri,
            lastTimestamp = initialTimestamp,
            playbackCount = 1,
            title = "Initial Title",
            duration = 60000L
        )
        dao.insertOrUpdate(item)

        val updatedTimestamp = System.currentTimeMillis()
        val updatedItem = PlaybackHistory(
            uri = uri, // Key
            lastTimestamp = updatedTimestamp,
            playbackCount = 2,
            title = "Updated Title",
            duration = 75000L,
            artist = "Updated Artist",
            artworkUri = "content://artwork/updated",
            thumbnailPath = "/path/to/updated_thumb.jpg"
        )
        dao.insertOrUpdate(updatedItem)

        val retrievedItem = dao.getHistoryItem(uriString)
        assertThat(retrievedItem).isNotNull()
        assertThat(retrievedItem?.uri).isEqualTo(uri)
        assertThat(retrievedItem?.playbackCount).isEqualTo(2)
        assertThat(retrievedItem?.lastTimestamp).isEqualTo(updatedTimestamp)
        assertThat(retrievedItem?.lastTimestamp).isGreaterThan(initialTimestamp)
        assertThat(retrievedItem?.title).isEqualTo("Updated Title")
        assertThat(retrievedItem?.duration).isEqualTo(75000L)
        assertThat(retrievedItem?.artist).isEqualTo("Updated Artist")
        assertThat(retrievedItem?.artworkUri).isEqualTo("content://artwork/updated")
        assertThat(retrievedItem?.thumbnailPath).isEqualTo("/path/to/updated_thumb.jpg")
    }

    @Test
    fun getAllHistory_returnsEmptyListWhenTableIsEmpty() = runTest {
        val historyList = dao.getAllHistory().first()
        assertThat(historyList).isEmpty()
    }

    @Test
    fun getAllHistory_returnsAllItemsOrderedByTimestampDesc() = runTest {
        val uri1 = Uri.parse("content://test/item1")
        val uri2 = Uri.parse("content://test/item2")
        val uri3 = Uri.parse("content://test/item3")

        val item1 = PlaybackHistory(uri1, System.currentTimeMillis() - 200, 1, title = "Item 1") // Oldest
        val item2 = PlaybackHistory(uri2, System.currentTimeMillis(), 1, title = "Item 2")         // Newest
        val item3 = PlaybackHistory(uri3, System.currentTimeMillis() - 100, 1, title = "Item 3", duration = 10L) // Middle

        dao.insertOrUpdate(item1)
        dao.insertOrUpdate(item2)
        dao.insertOrUpdate(item3)

        val historyList = dao.getAllHistory().first()
        assertThat(historyList).hasSize(3)
        assertThat(historyList[0].uri).isEqualTo(uri2)
        assertThat(historyList[0].title).isEqualTo("Item 2")
        assertThat(historyList[1].uri).isEqualTo(uri3)
        assertThat(historyList[1].title).isEqualTo("Item 3")
        assertThat(historyList[1].duration).isEqualTo(10L)
        assertThat(historyList[2].uri).isEqualTo(uri1)
        assertThat(historyList[2].title).isEqualTo("Item 1")
    }

    @Test
    fun getHistoryItem_retrievesExistingItemWithAllFields() = runTest {
        val uriString = "content://test/existing_fields"
        val uri = Uri.parse(uriString)
        val item = PlaybackHistory(
            uri = uri,
            lastTimestamp = System.currentTimeMillis(),
            playbackCount = 1,
            title = "Full Item",
            duration = 30000L,
            artist = "Full Artist",
            artworkUri = "content://artwork/full",
            thumbnailPath = "/path/to/full_thumb.jpg"
        )
        dao.insertOrUpdate(item)

        val retrievedItem = dao.getHistoryItem(uriString)
        assertThat(retrievedItem).isNotNull()
        assertThat(retrievedItem).isEqualTo(item) // Check all fields by comparing the object
    }

    @Test
    fun getHistoryItem_returnsNullForNonExistentItem() = runTest {
        val retrievedItem = dao.getHistoryItem("content://test/nonexistent")
        assertThat(retrievedItem).isNull()
    }

    @Test
    fun deleteHistoryItem_removesItemFromDatabase() = runTest {
        val uriString = "content://test/to_delete"
        val uri = Uri.parse(uriString)
        val item = PlaybackHistory(uri, System.currentTimeMillis(), 1)
        dao.insertOrUpdate(item)

        // Ensure item is there
        var retrievedItem = dao.getHistoryItem(uriString)
        assertThat(retrievedItem).isNotNull()

        dao.deleteHistoryItem(uriString)

        // Ensure item is gone
        retrievedItem = dao.getHistoryItem(uriString)
        assertThat(retrievedItem).isNull()
    }

    @Test
    fun updateTimestamp_updatesTimestampAndCountCorrectlyAndPreservesOtherFields() = runTest {
        val uriString = "content://test/update_ts_fields"
        val uri = Uri.parse(uriString)
        val initialTime = System.currentTimeMillis() - 1000
        val initialCount = 1
        val initialTitle = "Timestamp Test Title"
        val initialDuration = 45000L
        val initialArtist = "Timestamp Artist"
        val initialArtwork = "content://artwork/ts"
        val initialThumb = "/path/ts_thumb.jpg"

        val item = PlaybackHistory(
            uri = uri,
            lastTimestamp = initialTime,
            playbackCount = initialCount,
            title = initialTitle,
            duration = initialDuration,
            artist = initialArtist,
            artworkUri = initialArtwork,
            thumbnailPath = initialThumb
        )
        dao.insertOrUpdate(item)

        val retrievedBeforeUpdate = dao.getHistoryItem(uriString)
        assertThat(retrievedBeforeUpdate?.lastTimestamp).isEqualTo(initialTime)
        assertThat(retrievedBeforeUpdate?.playbackCount).isEqualTo(initialCount)
        assertThat(retrievedBeforeUpdate?.title).isEqualTo(initialTitle)

        val updatedTime = System.currentTimeMillis()
        val updatedCount = initialCount + 1
        // The updateTimestamp method in DAO is actually just an @Update,
        // so we pass the whole object.
        // If we only want to update specific fields, we'd need a custom query or
        // ensure the object passed to @Update has other fields unchanged.
        val itemToUpdate = retrievedBeforeUpdate!!.copy(
            lastTimestamp = updatedTime,
            playbackCount = updatedCount,
            // Title and other fields should remain the same if not explicitly changed here
            title = "New Title For Update Test" // Explicitly change title to test if @Update picks it up
        )
        dao.updateTimestamp(itemToUpdate) // @Update updates the entire entity

        val retrievedAfterUpdate = dao.getHistoryItem(uriString)
        assertThat(retrievedAfterUpdate).isNotNull()
        assertThat(retrievedAfterUpdate?.lastTimestamp).isEqualTo(updatedTime)
        assertThat(retrievedAfterUpdate?.playbackCount).isEqualTo(updatedCount)
        assertThat(retrievedAfterUpdate?.lastTimestamp).isGreaterThan(initialTime)
        assertThat(retrievedAfterUpdate?.title).isEqualTo("New Title For Update Test") // Verify title changed
        assertThat(retrievedAfterUpdate?.duration).isEqualTo(initialDuration) // Verify other fields preserved
        assertThat(retrievedAfterUpdate?.artist).isEqualTo(initialArtist)
        assertThat(retrievedAfterUpdate?.artworkUri).isEqualTo(initialArtwork)
        assertThat(retrievedAfterUpdate?.thumbnailPath).isEqualTo(initialThumb)
    }
}
