package com.cattailsw.mediaplayer

import android.app.Application
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.cattailsw.mediaplayer.data.AppDatabase
import com.cattailsw.mediaplayer.data.PlaybackHistory
import com.cattailsw.mediaplayer.data.PlaybackHistoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MainViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Mocks
    // @Mock
    // private lateinit var mockApplication: Application // Removed
    // @Mock
    // private lateinit var mockAppDatabase: AppDatabase // Removed
    @Mock
    private lateinit var mockPlaybackHistoryDao: PlaybackHistoryDao

    // Test Dispatcher
    private val testDispatcher = StandardTestDispatcher()

    // Subject under test
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher) // Set main dispatcher for tests

        // `mockPlaybackHistoryDao` is already initialized by MockitoJUnitRunner
        // Default behavior for DAO methods (can be overridden in specific tests)
        whenever(mockPlaybackHistoryDao.getAllHistory()).thenReturn(emptyFlow())

        // Instantiate ViewModel with the mocked DAO
        viewModel = MainViewModel(mockPlaybackHistoryDao)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain() // Reset main dispatcher
    }

    @Test
    fun `playbackHistoryFlow initially emits empty list when DAO returns empty flow`() = runTest(testDispatcher) {
        // ViewModel is already initialized in setup with DAO returning emptyFlow by default
        val firstEmission = viewModel.playbackHistoryFlow.first()
        assertThat(firstEmission).isEmpty()
        // Verify that getAllHistory was called during ViewModel's init block
        verify(mockPlaybackHistoryDao).getAllHistory()
    }

    @Test
    fun `playbackHistoryFlow emits list provided by DAO`() = runTest(testDispatcher) {
        val testUri = Uri.parse("content://test/history1")
        val historyList = listOf(PlaybackHistory(testUri, System.currentTimeMillis(), 1))
        // Override default mock behavior for this specific test
        whenever(mockPlaybackHistoryDao.getAllHistory()).thenReturn(flowOf(historyList))

        // Re-initialize ViewModel to pick up this specific mock for its init block
        val localViewModel = MainViewModel(mockPlaybackHistoryDao)
        val firstEmission = localViewModel.playbackHistoryFlow.first()

        assertThat(firstEmission).isEqualTo(historyList)
        assertThat(firstEmission).hasSize(1)
        assertThat(firstEmission[0].uri).isEqualTo(testUri)
        verify(mockPlaybackHistoryDao).getAllHistory() // Verify DAO was called
    }


    @Test
    fun `addOrUpdatePlaybackHistory adds new URI correctly`() = runTest(testDispatcher) {
        val testUri = Uri.parse("content://test/new_uri")
        val uriString = testUri.toString()

        // Mock DAO behavior for this test
        whenever(mockPlaybackHistoryDao.getHistoryItem(uriString)).thenReturn(null)

        // Call the ViewModel method
        viewModel.addOrUpdatePlaybackHistory(testUri)

        // Verify interactions
        verify(mockPlaybackHistoryDao).getHistoryItem(uriString)
        // Use an ArgumentCaptor to verify the PlaybackHistory object passed to insertOrUpdate
        val playbackHistoryCaptor = org.mockito.kotlin.argumentCaptor<PlaybackHistory>()
        verify(mockPlaybackHistoryDao).insertOrUpdate(playbackHistoryCaptor.capture())

        val capturedItem = playbackHistoryCaptor.firstValue
        assertThat(capturedItem.uri).isEqualTo(testUri)
        assertThat(capturedItem.playbackCount).isEqualTo(1)
        // Optionally, check timestamp is recent, though exact match is tricky
        assertThat(capturedItem.lastTimestamp).isGreaterThan(0L)
    }


    @Test
    fun `addOrUpdatePlaybackHistory updates existing URI correctly`() = runTest(testDispatcher) {
        val testUri = Uri.parse("content://test/existing_uri")
        val uriString = testUri.toString()
        val existingTimestamp = System.currentTimeMillis() - 10000
        val existingCount = 2
        val existingItem = PlaybackHistory(testUri, existingTimestamp, existingCount)

        // Mock DAO behavior for this test
        whenever(mockPlaybackHistoryDao.getHistoryItem(uriString)).thenReturn(existingItem)

        // Call the ViewModel method
        viewModel.addOrUpdatePlaybackHistory(testUri)

        // Verify interactions
        verify(mockPlaybackHistoryDao).getHistoryItem(uriString)
        // Use an ArgumentCaptor
        val playbackHistoryCaptor = org.mockito.kotlin.argumentCaptor<PlaybackHistory>()
        verify(mockPlaybackHistoryDao).insertOrUpdate(playbackHistoryCaptor.capture())

        val capturedItem = playbackHistoryCaptor.firstValue
        assertThat(capturedItem.uri).isEqualTo(testUri)
        assertThat(capturedItem.playbackCount).isEqualTo(existingCount + 1)
        assertThat(capturedItem.lastTimestamp).isGreaterThan(existingTimestamp)
    }

    @Test
    fun `deletePlaybackHistoryItem calls DAO's deleteHistoryItem`() = runTest(testDispatcher) {
        val testUri = Uri.parse("content://test/delete_uri")
        val uriString = testUri.toString()

        // Call the ViewModel method
        viewModel.deletePlaybackHistoryItem(testUri)

        // Verify interaction
        verify(mockPlaybackHistoryDao).deleteHistoryItem(uriString)
    }

    // Note: The previous notes about testability are now addressed by injecting the DAO.
    // The tests should now fully function as unit tests.
}
