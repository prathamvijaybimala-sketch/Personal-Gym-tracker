package com.gympro.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasContentDescriptionStartingWith
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gympro.app.domain.Dates
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Walks every screen and modal, capturing PNG screenshots into
 * filesDir/screens (pulled via `adb exec-out run-as` in CI and uploaded
 * as a workflow artifact).
 */
@RunWith(AndroidJUnit4::class)
class AppScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val targetContext get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val today = Dates.todayKey()

    @Before
    fun setUp() {
        composeRule.mainClock.autoAdvance = false
        TestSeeder.seedRichState()
        composeRule.mainClock.advanceTimeBy(800)
    }

    private fun advance(ms: Long = 400) {
        composeRule.mainClock.advanceTimeBy(ms)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun capture(name: String) {
        composeRule.mainClock.advanceTimeBy(500)
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(targetContext.filesDir, "screens").apply { mkdirs() }
        val out = File(dir, "$name.png")
        FileOutputStream(out).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
    }

    private fun scrollToIn(listTag: String, selector: androidx.compose.ui.test.SemanticsMatcher) {
        composeRule.onNode(hasTestTag(listTag) and hasScrollAction()).performScrollToNode(selector)
        advance(200)
    }

    private fun waitForText(text: String, timeoutMs: Long = 8000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            composeRule.mainClock.advanceTimeBy(100)
            if (composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) return
        }
        composeRule.onNodeWithText(text).assertExists()
    }

    @Test
    fun captureAllScreens() {
        // ---------------------------------------------------------------
        // HOME (dark theme)
        // ---------------------------------------------------------------
        waitForText("This Week")
        capture("01_home_top")

        scrollToIn("homeList", hasContentDescriptionStartingWith("calendar day $today"))
        composeRule.onNode(hasContentDescriptionStartingWith("calendar day $today")).performClick()
        advance()
        waitForText("NOTE")
        capture("02_home_history_modal")

        // Close via the sheet handle area (tap outside) — use back
        androidx.test.espresso.Espresso.pressBack()
        advance(500)

        scrollToIn("homeList", hasContentDescription("Weight graph"))
        composeRule.onNodeWithContentDescription("Weight graph").performClick()
        advance()
        waitForText("Latest")
        capture("03_home_graph_modal")
        androidx.test.espresso.Espresso.pressBack()
        advance(500)

        composeRule.onNodeWithContentDescription("header stats").performClick()
        advance()
        waitForText("US Navy Method")
        capture("04_home_bf_modal")
        androidx.test.espresso.Espresso.pressBack()
        advance(500)

        // ---------------------------------------------------------------
        // TRAIN
        // ---------------------------------------------------------------
        composeRule.onNodeWithContentDescription("Train").performClick()
        advance()
        waitForText("Chest Press")
        // Draft a heavier weight so the PR badge shows in the screenshot
        runBlocking {
            TestSeeder.container.workoutRepository.saveDraft("cp", "100", "10")
        }
        advance(600)
        waitForText("🏆 PR")
        capture("05_train_day_workout")

        // Rest day
        composeRule.onNodeWithContentDescription("day chip 0").performClick()
        advance(600)
        waitForText("Active Recovery")
        capture("06_train_rest_day")

        // Routine manager
        composeRule.onNodeWithContentDescription("day chip 1").performClick()
        advance(300)
        composeRule.onNodeWithContentDescription("routine pill").performClick()
        advance()
        waitForText("Routines")
        capture("07_train_routine_manager")

        // Exercise manager
        androidx.test.espresso.Espresso.pressBack()
        advance(500)
        composeRule.onNodeWithContentDescription("Edit exercises").performClick()
        advance()
        waitForText("ADD EXERCISE")
        capture("08_train_exercise_manager")
        androidx.test.espresso.Espresso.pressBack()
        advance(500)

        // Import modal
        composeRule.onNodeWithContentDescription("routine pill").performClick()
        advance()
        composeRule.onNodeWithText("📥 Import via JSON").performClick()
        advance()
        waitForText("IMPORT ROUTINE")
        capture("09_train_import_modal")
        androidx.test.espresso.Espresso.pressBack()
        advance(500)

        // AI prompt modal
        composeRule.onNodeWithContentDescription("routine pill").performClick()
        advance()
        composeRule.onNodeWithText("🤖 AI Prompt Generator").performClick()
        advance()
        waitForText("GENERATE PROMPT")
        composeRule.onNodeWithTag("aiRoutineType").performTextInput("6-day PPL")
        advance(200)
        composeRule.onNodeWithText("GENERATE PROMPT").performClick()
        advance(600)
        waitForText("Copy this → paste in Claude / ChatGPT")
        capture("10_train_ai_prompt")
        androidx.test.espresso.Espresso.pressBack()
        advance(500)

        // Cardio modal
        composeRule.onNodeWithContentDescription("Open cardio log").performClick()
        advance()
        waitForText("LOG CARDIO")
        capture("11_train_cardio")
        androidx.test.espresso.Espresso.pressBack()
        advance(500)

        // Timer overlay
        composeRule.onAllNodesWithContentDescription("Start rest timer")[0].performClick()
        advance(300)
        capture("12_train_timer_overlay")
        composeRule.onNodeWithContentDescription("timer overlay").performClick()
        advance(300)

        // ---------------------------------------------------------------
        // FUEL
        // ---------------------------------------------------------------
        composeRule.onNodeWithContentDescription("Fuel").performClick()
        advance()
        waitForText("Protein")
        capture("13_fuel_protein")

        scrollToIn("fuelList", hasContentDescriptionStartingWith("water glass 0"))
        capture("14_fuel_water")

        scrollToIn("fuelList", hasText("Supplements"))
        capture("15_fuel_supps")

        scrollToIn("fuelList", hasText("7-Day Protein"))
        advance(300)
        capture("16_fuel_trends")

        // Diet manager (protein card is at the top; "✎ Edit" [0] is its button)
        scrollToIn("fuelList", hasText("Protein"))
        composeRule.onAllNodesWithText("✎ Edit")[0].performClick()
        advance()
        waitForText("Food Menu")
        capture("17_fuel_diet_manager")
        androidx.test.espresso.Espresso.pressBack()
        advance(500)

        // Goal editor
        scrollToIn("fuelList", hasText("Protein"))
        composeRule.onAllNodesWithText("⚙ Goal")[0].performClick()
        advance()
        waitForText("Protein Goal")
        capture("18_fuel_goal_editor")
        androidx.test.espresso.Espresso.pressBack()
        advance(500)

        // ---------------------------------------------------------------
        // LIGHT THEME
        // ---------------------------------------------------------------
        composeRule.onNodeWithContentDescription("Home").performClick()
        advance()
        composeRule.onNodeWithContentDescription("theme toggle").performClick()
        advance(600)
        waitForText("This Week")
        capture("19_home_light_theme")
    }
}
