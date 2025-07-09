package com.example.uniflow

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun `formatDate returns correct format`() {
        val date = LocalDate.of(2024, 5, 20)
        val expected = "20.05.2024"
        val result = formatDate(date)
        assertEquals(expected, result)
    }
}
