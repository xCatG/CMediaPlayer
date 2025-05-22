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
        val newItem = PlaybackHistory(uri, System.currentTimeMillis(), 1)

        dao.insertOrUpdate(newItem)

        val retrievedItem = dao.getHistoryItem(uriString)
        assertThat(retrievedItem).isNotNull()
        assertThat(retrievedItem?.uri).isEqualTo(uri)
        assertThat(retrievedItem?.playbackCount).isEqualTo(1)
    }

    @Test
    fun insertOrUpdate_updatesExistingItem() = runTest {
        val uriString = "content://test/2"
        val uri = Uri.parse(uriString)
        val initialTimestamp = System.currentTimeMillis() - 1000
        val item = PlaybackHistory(uri, initialTimestamp, 1)
        dao.insertOrUpdate(item)

        val updatedTimestamp = System.currentTimeMillis()
        val updatedItem = PlaybackHistory(uri, updatedTimestamp, 2)
        dao.insertOrUpdate(updatedItem)

        val retrievedItem = dao.getHistoryItem(uriString)
        assertThat(retrievedItem).isNotNull()
        assertThat(retrievedItem?.uri).isEqualTo(uri)
        assertThat(retrievedItem?.playbackCount).isEqualTo(2)
        assertThat(retrievedItem?.lastTimestamp).isEqualTo(updatedTimestamp)
        assertThat(retrievedItem?.lastTimestamp).isGreaterThan(initialTimestamp)
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

        val item1 = PlaybackHistory(uri1, System.currentTimeMillis() - 200, 1) // Oldest
        val item2 = PlaybackHistory(uri2, System.currentTimeMillis(), 1)         // Newest
        val item3 = PlaybackHistory(uri3, System.currentTimeMillis() - 100, 1) // Middle

        dao.insertOrUpdate(item1)
        dao.insertOrUpdate(item2)
        dao.insertOrUpdate(item3)

        val historyList = dao.getAllHistory().first()
        assertThat(historyList).hasSize(3)
        assertThat(historyList[0].uri).isEqualTo(uri2) // Newest
        assertThat(historyList[1].uri).isEqualTo(uri3) // Middle
        assertThat(historyList[2].uri).isEqualTo(uri1) // Oldest
    }

    @Test
    fun getHistoryItem_retrievesExistingItem() = runTest {
        val uriString = "content://test/existing"
        val uri = Uri.parse(uriString)
        val item = PlaybackHistory(uri, System.currentTimeMillis(), 1)
        dao.insertOrUpdate(item)

        val retrievedItem = dao.getHistoryItem(uriString)
        assertThat(retrievedItem).isNotNull()
        assertThat(retrievedItem?.uri).isEqualTo(uri)
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
    fun updateTimestamp_updatesTimestampAndCountCorrectly() = runTest {
        val uriString = "content://test/update_ts"
        val uri = Uri.parse(uriString)
        val initialTime = System.currentTimeMillis() - 1000
        val initialCount = 1
        val item = PlaybackHistory(uri, initialTime, initialCount)
        dao.insertOrUpdate(item)

        val retrievedBeforeUpdate = dao.getHistoryItem(uriString)
        assertThat(retrievedBeforeUpdate?.lastTimestamp).isEqualTo(initialTime)
        assertThat(retrievedBeforeUpdate?.playbackCount).isEqualTo(initialCount)

        val updatedTime = System.currentTimeMillis()
        val updatedCount = initialCount + 1
        // The updateTimestamp method in DAO is actually just an @Update,
        // so we pass the whole object. If it were a specific query to update only timestamp,
        // this test would be different.
        val itemToUpdate = retrievedBeforeUpdate!!.copy(lastTimestamp = updatedTime, playbackCount = updatedCount)
        dao.updateTimestamp(itemToUpdate) // Assuming updateTimestamp is meant to take the updated object

        val retrievedAfterUpdate = dao.getHistoryItem(uriString)
        assertThat(retrievedAfterUpdate?.lastTimestamp).isEqualTo(updatedTime)
        assertThat(retrievedAfterUpdate?.playbackCount).isEqualTo(updatedCount)
        assertThat(retrievedAfterUpdate?.lastTimestamp).isGreaterThan(initialTime)
    }
}
