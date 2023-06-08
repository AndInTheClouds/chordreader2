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
}