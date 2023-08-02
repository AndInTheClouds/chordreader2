package org.hollowbamboo.chordreader2.helper

import android.net.Uri

class MetadataExtractionHelper {
    fun extractSuggestedFilename(url: String, html: String): String? {
        val uri = Uri.parse(url)

        if (uri.host == ULTIMATE_GUITAR_HOST) {
            return getFilenameFromUltimateGuitarHTML(html)
        }

        return null
    }

    private fun getFilenameFromUltimateGuitarHTML(html: String): String? {
        val pattern = "<meta property=\"og:title\" content=\"([^\"]+) \\(Chords\\)".toRegex()
        val matchResult = pattern.find(html) ?: return null

        val groupValues = matchResult.groupValues
        if (groupValues.isEmpty()) {
            return null
        }

        return groupValues[1]
    }

    companion object {
        const val ULTIMATE_GUITAR_HOST = "tabs.ultimate-guitar.com"
    }
}