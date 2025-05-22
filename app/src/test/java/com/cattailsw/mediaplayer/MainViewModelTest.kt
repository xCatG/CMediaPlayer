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

import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.cattailsw.mediaplayer.util.ThumbnailExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import androidx.media3.common.C // For C.TIME_UNSET


@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MainViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockApplication: Application // Added back
    @Mock
    private lateinit var mockPlaybackHistoryDao: PlaybackHistoryDao
    @Mock
    private lateinit var mockExoHolderVM: ExoHolderVM // Added
    @Mock
    private lateinit var mockPlayer: Player // For mocking player.duration

    private lateinit var metadataFlow: MutableStateFlow<Pair<Uri, MediaMetadata>?>


    // Test Dispatcher
    private val testDispatcher = StandardTestDispatcher()

    // Subject under test
    private lateinit var viewModel: MainViewModel
    private lateinit var mockStaticThumbnailExtractor: MockedStatic<ThumbnailExtractor>


    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        metadataFlow = MutableStateFlow(null) // Initialize the flow
        whenever(mockExoHolderVM.mediaMetadataFlow).thenReturn(metadataFlow)
        whenever(mockExoHolderVM.player).thenReturn(mockPlayer) // Mock player access
        whenever(mockPlayer.duration).thenReturn(C.TIME_UNSET) // Default duration

        // Default behavior for DAO
        whenever(mockPlaybackHistoryDao.getAllHistory()).thenReturn(emptyFlow())

        // Mock static ThumbnailExtractor.extractThumbnail
        // This is generally fragile and dependency injection is preferred.
        mockStaticThumbnailExtractor = mockStatic(ThumbnailExtractor::class.java)

        // Instantiate ViewModel
        viewModel = MainViewModel(mockApplication, mockPlaybackHistoryDao, mockExoHolderVM)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        mockStaticThumbnailExtractor.close() // Close static mock
    }

    @Test
    fun `playbackHistoryFlow initially emits empty list when DAO returns empty flow`() = runTest(testDispatcher) {
        val firstEmission = viewModel.playbackHistoryFlow.first()
        assertThat(firstEmission).isEmpty()
        verify(mockPlaybackHistoryDao).getAllHistory()
    }

    @Test
    fun `playbackHistoryFlow emits list provided by DAO`() = runTest(testDispatcher) {
        val testUri = Uri.parse("content://test/history1")
        val historyList = listOf(PlaybackHistory(testUri, System.currentTimeMillis(), 1))
        whenever(mockPlaybackHistoryDao.getAllHistory()).thenReturn(flowOf(historyList))

        // Re-initialize ViewModel to pick up this specific mock for its init block
        // Or ensure the flow is hot and can be re-collected if the test starts after initial collection
        val localViewModel = MainViewModel(mockApplication, mockPlaybackHistoryDao, mockExoHolderVM)
        val firstEmission = localViewModel.playbackHistoryFlow.first()

        assertThat(firstEmission).isEqualTo(historyList)
        assertThat(firstEmission).hasSize(1)
        assertThat(firstEmission[0].uri).isEqualTo(testUri)
        // getAllHistory might be called multiple times if ViewModel is re-initialized.
        verify(mockPlaybackHistoryDao, times(2)).getAllHistory() // Adjust based on actual init logic
    }


    @Test
    fun `addOrUpdatePlaybackHistory adds new URI with thumbnail path`() = runTest(testDispatcher) {
        val testUri = Uri.parse("content://test/new_uri_thumb")
        val uriString = testUri.toString()
        val mockThumbnailPath = "/cache/thumb1.jpg"

        whenever(mockPlaybackHistoryDao.getHistoryItem(uriString)).thenReturn(null)
        // Mock the static suspend function ThumbnailExtractor.extractThumbnail
        whenever(ThumbnailExtractor.extractThumbnail(mockApplication.applicationContext, testUri))
            .thenReturn(mockThumbnailPath)

        viewModel.addOrUpdatePlaybackHistory(testUri)

        verify(mockPlaybackHistoryDao).getHistoryItem(uriString)
        val playbackHistoryCaptor = ArgumentCaptor.forClass(PlaybackHistory::class.java)
        verify(mockPlaybackHistoryDao).insertOrUpdate(playbackHistoryCaptor.capture())

        val capturedItem = playbackHistoryCaptor.value
        assertThat(capturedItem.uri).isEqualTo(testUri)
        assertThat(capturedItem.playbackCount).isEqualTo(1)
        assertThat(capturedItem.thumbnailPath).isEqualTo(mockThumbnailPath) // Verify thumbnail path
        assertThat(capturedItem.lastTimestamp).isGreaterThan(0L)
    }


    @Test
    fun `addOrUpdatePlaybackHistory updates existing URI correctly without re-extracting thumbnail if path exists`() = runTest(testDispatcher) {
        val testUri = Uri.parse("content://test/existing_uri_thumb")
        val uriString = testUri.toString()
        val existingTimestamp = System.currentTimeMillis() - 10000
        val existingCount = 2
        val existingThumbnailPath = "/cache/existing_thumb.jpg"
        val existingItem = PlaybackHistory(
            uri = testUri,
            lastTimestamp = existingTimestamp,
            playbackCount = existingCount,
            thumbnailPath = existingThumbnailPath // Existing item already has a thumbnail
        )

        whenever(mockPlaybackHistoryDao.getHistoryItem(uriString)).thenReturn(existingItem)
        // ThumbnailExtractor.extractThumbnail should NOT be called again if path exists
        // (mockStatic will verify if any interaction happens with ThumbnailExtractor if not specified)

        viewModel.addOrUpdatePlaybackHistory(testUri)

        verify(mockPlaybackHistoryDao).getHistoryItem(uriString)
        val playbackHistoryCaptor = ArgumentCaptor.forClass(PlaybackHistory::class.java)
        verify(mockPlaybackHistoryDao).insertOrUpdate(playbackHistoryCaptor.capture())

        val capturedItem = playbackHistoryCaptor.value
        assertThat(capturedItem.uri).isEqualTo(testUri)
        assertThat(capturedItem.playbackCount).isEqualTo(existingCount + 1)
        assertThat(capturedItem.lastTimestamp).isGreaterThan(existingTimestamp)
        assertThat(capturedItem.thumbnailPath).isEqualTo(existingThumbnailPath) // Path should remain unchanged

        // Verify extractThumbnail was NOT called
        verify(mockStaticThumbnailExtractor, never()) {
            ThumbnailExtractor.extractThumbnail(any(), any())
        }
    }

    @Test
    fun `metadata collection updates PlaybackHistory item with title, artist, artworkUri and duration`() = runTest(testDispatcher) {
        val testUri = Uri.parse("content://test/metadata_update")
        val initialHistoryItem = PlaybackHistory(uri = testUri, playbackCount = 1, title = null, duration = 0L)
        val expectedTitle = "Test Video Title"
        val expectedArtist = "Test Artist"
        val expectedArtworkUri = Uri.parse("content://artwork/test")
        val expectedDuration = 120000L // 2 minutes

        // Setup: DAO returns the initial item, player returns duration
        whenever(mockPlaybackHistoryDao.getHistoryItem(testUri.toString())).thenReturn(initialHistoryItem)
        whenever(mockPlayer.duration).thenReturn(expectedDuration)

        // Create mock metadata
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(expectedTitle)
            .setArtist(expectedArtist)
            .setArtworkUri(expectedArtworkUri)
            .build()

        // Emit metadata
        metadataFlow.emit(Pair(testUri, mediaMetadata))
        testDispatcher.scheduler.advanceUntilIdle() // Ensure coroutine in ViewModel's init block runs

        // Verify: DAO's insertOrUpdate was called with the updated item
        val playbackHistoryCaptor = ArgumentCaptor.forClass(PlaybackHistory::class.java)
        verify(mockPlaybackHistoryDao).insertOrUpdate(playbackHistoryCaptor.capture())

        val capturedItem = playbackHistoryCaptor.value
        assertThat(capturedItem.uri).isEqualTo(testUri)
        assertThat(capturedItem.title).isEqualTo(expectedTitle)
        assertThat(capturedItem.artist).isEqualTo(expectedArtist)
        assertThat(capturedItem.artworkUri).isEqualTo(expectedArtworkUri.toString())
        assertThat(capturedItem.duration).isEqualTo(expectedDuration)
    }
    
    @Test
    fun `metadata collection updates PlaybackHistory item with displayTitle if title is null`() = runTest(testDispatcher) {
        val testUri = Uri.parse("content://test/metadata_display_title")
        val initialHistoryItem = PlaybackHistory(uri = testUri, playbackCount = 1, title = null)
        val expectedDisplayTitle = "Test Video Display Title"

        whenever(mockPlaybackHistoryDao.getHistoryItem(testUri.toString())).thenReturn(initialHistoryItem)
        val mediaMetadata = MediaMetadata.Builder().setDisplayTitle(expectedDisplayTitle).build()

        metadataFlow.emit(Pair(testUri, mediaMetadata))
        testDispatcher.scheduler.advanceUntilIdle()

        val playbackHistoryCaptor = ArgumentCaptor.forClass(PlaybackHistory::class.java)
        verify(mockPlaybackHistoryDao).insertOrUpdate(playbackHistoryCaptor.capture())
        val capturedItem = playbackHistoryCaptor.value
        assertThat(capturedItem.title).isEqualTo(expectedDisplayTitle)
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
