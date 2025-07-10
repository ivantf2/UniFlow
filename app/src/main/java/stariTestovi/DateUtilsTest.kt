package stariTestovi

import com.example.uniflow.formatDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun formatDateReturnsCorrectFormat() {
        val date = LocalDate.of(2024, 5, 20)
        val expected = "20.05.2024"
        val result = formatDate(date)
        assertEquals(expected, result)
    }
}
