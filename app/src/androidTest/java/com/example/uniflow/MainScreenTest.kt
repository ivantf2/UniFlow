package com.example.uniflow

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testAddingTaskDisplaysItOnScreen() {
        val taskText = "Predavanje: Matematika - 10:00"

        // Klikni FAB za otvaranje dijaloga
        composeTestRule.onNodeWithText("+").performClick()

        // Unesi tip obaveze
        composeTestRule.onNodeWithText("Vrsta obaveze").performTextInput("Predavanje")

        // Unesi ime obaveze
        composeTestRule.onNodeWithText("Naziv obaveze").performTextInput("Matematika")

        // Unesi vrijeme
        composeTestRule.onNodeWithText("Vrijeme (opcionalno)").performTextInput("10:00")

        // Klikni "Spremi"
        composeTestRule.onNodeWithText("Spremi").performClick()

        // Odaberi današnji datum u kalendaru
        val today = LocalDate.now().dayOfMonth.toString()
        composeTestRule.onNodeWithText(today).performClick()

        // Potvrdi prikaz obaveze na zaslonu
        composeTestRule.onNodeWithText(taskText, substring = true).assertExists()
    }

    private fun ComposeTestRule.waitUntilExists(
        matcher: SemanticsMatcher,
        timeoutMillis: Long = 5000L
    ) {
        this.waitUntil(timeoutMillis) {
            this.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
