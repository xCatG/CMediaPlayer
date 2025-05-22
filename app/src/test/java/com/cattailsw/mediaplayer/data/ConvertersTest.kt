package com.cattailsw.mediaplayer.data

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `fromString returns null when given null`() {
        val result = converters.fromString(null)
        assertThat(result).isNull()
    }

    @Test
    fun `fromString returns Uri when given valid string`() {
        val uriString = "content://com.example/document/1"
        val result = converters.fromString(uriString)
        assertThat(result).isInstanceOf(Uri::class.java)
        assertThat(result.toString()).isEqualTo(uriString)
    }

    @Test
    fun `fromString returns Uri for different valid schemes`() {
        val fileUriString = "file:///sdcard/video.mp4"
        val httpUriString = "http://example.com/video.mp4"

        val fileResult = converters.fromString(fileUriString)
        assertThat(fileResult.toString()).isEqualTo(fileUriString)

        val httpResult = converters.fromString(httpUriString)
        assertThat(httpResult.toString()).isEqualTo(httpUriString)
    }

    @Test
    fun `uriToString returns null when given null Uri`() {
        val result = converters.uriToString(null)
        assertThat(result).isNull()
    }

    @Test
    fun `uriToString returns string representation when given valid Uri`() {
        val uriString = "content://com.example/document/2"
        val uri = Uri.parse(uriString)
        val result = converters.uriToString(uri)
        assertThat(result).isEqualTo(uriString)
    }

    @Test
    fun `uriToString returns correct string for Uris with query and fragment`() {
        val uriWithQueryFragment = "http://example.com/path?query=123#fragment"
        val uri = Uri.parse(uriWithQueryFragment)
        val result = converters.uriToString(uri)
        assertThat(result).isEqualTo(uriWithQueryFragment)
    }
}
