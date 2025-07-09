package com.example.uniflow

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class LoginToSettingsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<LoginActivity>()

    @Test
    fun endToEnd_Login_OpenDrawer_NavigateToSettings() {
        val taskText = "Kolokvij: Operacijska Istraživanja - 12:00"
        // Unesi korisničko ime
        composeTestRule.onNodeWithText("Korisničko ime").performTextInput("TestKorisnik")

        // Unesi lozinku
        composeTestRule.onNodeWithText("Lozinka").performTextInput("tajna")

        // Klikni gumb Prijava
        composeTestRule.onNodeWithText("Prijava").performClick()

        // Čekaj da se MainActivity otvori (dodaj delay ako je potrebno)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("UniFlow").fetchSemanticsNodes().isNotEmpty()
        }

        // Klikni FAB za otvaranje dijaloga
        composeTestRule.onNodeWithText("+").performClick()

        // Unesi tip obaveze
        composeTestRule.onNodeWithText("Vrsta obaveze").performTextInput("Kolokvij")

        // Unesi ime obaveze
        composeTestRule.onNodeWithText("Naziv obaveze").performTextInput("Operacijska Istraživanja")

        // Unesi vrijeme
        composeTestRule.onNodeWithText("Vrijeme (opcionalno)").performTextInput("12:00")

        // Klikni "Spremi"
        composeTestRule.onNodeWithText("Spremi").performClick()

        // Odaberi današnji datum u kalendaru
        val today = LocalDate.now().dayOfMonth.toString()
        composeTestRule.onNodeWithText(today).performClick()

        // Potvrdi prikaz obaveze na zaslonu
        composeTestRule.onNodeWithText(taskText, substring = true).assertExists()

        // Otvori meni klikom na ikonu
        composeTestRule.onNode(hasContentDescription("Menu")).performClick()

        // Klikni na Postavke
        composeTestRule.onNodeWithText("Postavke").performClick()

        // Čekaj SettingsActivity i provjeri postoji li "Dark mode"
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Dark mode").fetchSemanticsNodes().isNotEmpty()
        }

        // Klikni na prekidač za tamnu temu koristeći testTag
        composeTestRule.onNodeWithTag("darkModeSwitch").performClick()

        // Čekaj Compose da završi sve UI promjene
        composeTestRule.waitForIdle()

        Thread.sleep(2000)


    }
}