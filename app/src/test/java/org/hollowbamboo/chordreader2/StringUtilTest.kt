package org.hollowbamboo.chordreader2

import org.hollowbamboo.chordreader2.util.StringUtil
import org.junit.Assert
import org.junit.Test

class StringUtilTest {
    @Test
    fun split() {
        // Given
        val input = "lorem\nipsum"
        val delimeter = "\n"

        // When
        val actual = StringUtil.split(input, delimeter)

        // Then
        Assert.assertEquals(arrayOf("lorem", "ipsum"), actual)
    }

    @Test
    fun replace() {
        // Given
        val originalString = "a1a1a1a1a1"
        val searchString = "a"
        val replaceString = "b"

        // When
        val actual = StringUtil.replace(originalString, searchString, replaceString)

        // Then
        Assert.assertEquals("b1b1b1b1b1", actual)
    }

    @Test
    fun isAllWhitespace_true() {
        // Given
        val input = "      "

        // When
        val actual = StringUtil.isAllWhitespace(input)

        // Then
        Assert.assertTrue(actual)
    }

    @Test
    fun isAllWhitespace_false() {
        // Given
        val input = "     lorem ipsum"

        // When
        val actual = StringUtil.isAllWhitespace(input)

        // Then
        Assert.assertFalse(actual)
    }
}