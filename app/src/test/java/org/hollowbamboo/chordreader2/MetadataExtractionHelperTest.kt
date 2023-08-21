package org.hollowbamboo.chordreader2

import android.net.Uri
import org.hollowbamboo.chordreader2.helper.MetadataExtractionHelper
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
public class MetadataExtractionHelperTest {
    @Test
    fun extractSuggestedFilename_when_notUltimateGuitarHost_should_returnNull() {
        // Given
        val url = "https://example.com"

        // When
        val actual = MetadataExtractionHelper().extractSuggestedFilename(url, "")

        // Then
        Assert.assertNull(actual)
    }

    @Test
    fun extractSuggestedFilename_when_ultimateGuitarHost_butNoOGTitleMetaTag_should_returnNull() {
        // Given
        val url = "https://${MetadataExtractionHelper.ULTIMATE_GUITAR_HOST}"
        val html = ""

        // When
        val actual = MetadataExtractionHelper().extractSuggestedFilename(url, html)

        // Then
        Assert.assertNull(actual)
    }

    @Test
    fun extractSuggestedFilename_when_ultimateGuitarHost_andOGTitleMetaTagPresent_should_returnTheContent() {
        // Given
        val url = "https://${MetadataExtractionHelper.ULTIMATE_GUITAR_HOST}"
        val metaTagContent = "Lorem ipsum dolor sit"
        val html = """
        <meta property="og:type" content="music.song">
        <meta property="og:title" content="$metaTagContent (Chords)">
        <meta property="og:description" content="CHORDS foo bar">
        """.trimIndent()

        // When
        val actual = MetadataExtractionHelper().extractSuggestedFilename(url, html)

        // Then
        Assert.assertEquals(metaTagContent, actual)
    }
}