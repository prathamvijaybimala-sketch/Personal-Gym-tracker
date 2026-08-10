package com.gympro.app

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasContentDescriptionStartingWith
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gympro.app.data.db.ExerciseLogEntity
import com.gympro.app.domain.Dates
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end test covering the Definition of Done:
 * create routine → log workout → PR celebration → calendar + streak →
 * diet/water/mood/notes → 7-day trend → export → wipe → import → identical state.
 */
@RunWith(AndroidJUnit4::class)
class AppE2eTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        // Pause the frame clock so infinite animations (breathing, PR pulse)
        // never make waitForIdle hang; we advance manually.
        composeRule.mainClock.autoAdvance = false
        Intents.init()
        TestSeeder.reset()
        composeRule.mainClock.advanceTimeBy(800)
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    private fun advance(ms: Long = 400) {
        composeRule.mainClock.advanceTimeBy(ms)
    }

    private fun waitForText(text: String, timeoutMs: Long = 8000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            composeRule.mainClock.advanceTimeBy(100)
            if (composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) return
        }
        composeRule.onNodeWithText(text).assertExists()
    }

    private fun waitForContentDescription(prefix: String, timeoutMs: Long = 8000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            composeRule.mainClock.advanceTimeBy(100)
            if (composeRule.onAllNodes(hasContentDescriptionStartingWith(prefix)).fetchSemanticsNodes().isNotEmpty()) return
        }
        composeRule.onNode(hasContentDescriptionStartingWith(prefix)).assertExists()
    }

    private fun scrollToIn(listTag: String, selector: androidx.compose.ui.test.SemanticsMatcher) {
        composeRule.onNode(hasTestTag(listTag) and hasScrollAction()).performScrollToNode(selector)
        advance(200)
    }

    @Test
    fun definitionOfDone_fullFlow() {
        val today = Dates.todayKey()

        // ---------------------------------------------------------------
        // 1. HOME: initial state renders
        // ---------------------------------------------------------------
        waitForText("This Week")
        composeRule.onNode(hasContentDescriptionStartingWith("calendar day $today")).assertExists()

        // ---------------------------------------------------------------
        // 2. TRAIN: log a workout that beats history → PR celebration
        // ---------------------------------------------------------------
        composeRule.onNodeWithContentDescription("Train").performClick()
        advance()
        composeRule.onNodeWithContentDescription("day chip 1").performClick()
        advance()
        waitForText("Chest Press")

        // Seed a prior session (50kg×10 → est 1RM 67) so the log below is a PR
        runBlocking {
            TestSeeder.container.database.exerciseDao().insert(
                ExerciseLogEntity(
                    exerciseId = "cp",
                    date = Dates.key(Dates.today().minusDays(1)),
                    weight = 50.0,
                    reps = 10,
                )
            )
        }
        advance(400)

        composeRule.onNodeWithTag("w_cp").performTextReplacement("100")
        advance(200)
        composeRule.onNodeWithTag("r_cp").performTextReplacement("10")
        advance(600)

        // PR badge appears while drafting
        waitForText("🏆 PR")

        // Log the complete workout
        scrollToIn("trainList", hasText("✓ LOG COMPLETE WORKOUT"))
        composeRule.onNodeWithText("✓ LOG COMPLETE WORKOUT").performClick()
        advance(600)

        // PR celebration toast + success toast
        waitForText("1 new PR! 🏆")
        advance(300)
        waitForText("Workout saved! 1 exercises logged 💪")

        // ---------------------------------------------------------------
        // 3. HOME: streak 1, today green on calendar and week dots
        // ---------------------------------------------------------------
        composeRule.onNodeWithContentDescription("Home").performClick()
        advance()
        waitForContentDescription("streak 1")
        composeRule.onNode(hasContentDescriptionStartingWith("calendar day $today green")).assertExists()
        composeRule.onNode(hasContentDescription("week dot $today done")).assertExists()

        // ---------------------------------------------------------------
        // 4. FUEL: diet, water, mood, notes
        // ---------------------------------------------------------------
        composeRule.onNodeWithContentDescription("Fuel").performClick()
        advance()
        waitForText("Protein")

        composeRule.onNodeWithContentDescription("diet item e1 unchecked").performClick()
        advance(600)
        composeRule.onNodeWithContentDescription("diet item e1 checked").assertExists()

        composeRule.onNodeWithTag("quickProt").performTextInput("20")
        advance(200)
        composeRule.onNodeWithText("ADD").performClick()
        advance(600)
        waitForText("+20g protein added")

        scrollToIn("fuelList", hasContentDescriptionStartingWith("water glass 0"))
        composeRule.onNodeWithContentDescription("water glass 0 empty").performClick()
        advance(600)
        composeRule.onNodeWithContentDescription("water glass 0 filled").assertExists()

        // Mood (Home) + note (history modal)
        composeRule.onNodeWithContentDescription("Home").performClick()
        advance()
        scrollToIn("homeList", hasText("🔥"))
        composeRule.onNodeWithText("🔥").performClick()
        advance(400)
        waitForText("Mood logged 🔥")

        scrollToIn("homeList", hasContentDescriptionStartingWith("calendar day $today"))
        composeRule.onNode(hasContentDescriptionStartingWith("calendar day $today")).performClick()
        advance()
        waitForText("NOTE")
        composeRule.onNodeWithTag("dayNoteField").performTextInput("E2E note")
        advance(400)
        composeRule.onNode(hasText("Diet (", substring = true)).assertExists()

        // ---------------------------------------------------------------
        // 5. Export → wipe → import → identical state
        // ---------------------------------------------------------------
        // Intercept the share sheet so the test can continue
        Intents.intending(hasAction(Intent.ACTION_SEND))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
        Espresso.pressBack() // close history modal
        advance(500)

        scrollToIn("homeList", hasContentDescription("Export backup"))
        composeRule.onNodeWithContentDescription("Export backup").performClick()
        advance(600)

        val exported = runBlocking { TestSeeder.container.backupRepository.exportJson() }
        assertTrue(exported.contains("\"routines\""))
        assertTrue(exported.contains("\"gatt\""))
        assertTrue(exported.contains("\"h_cp\""))
        assertTrue(exported.contains("\"dc_$today\""))
        assertTrue(exported.contains("\"note_$today\""))
        assertTrue(exported.contains("\"mood_$today\""))
        assertTrue(exported.contains("\"water_$today\""))
        assertTrue(exported.contains("\"dlog\""))

        // Wipe everything (as if the user cleared app data)
        runBlocking {
            TestSeeder.container.wipeAll()
            TestSeeder.container.ensureSeeded()
        }
        advance(800)

        // Empty state: no streak pill
        assertTrue(
            composeRule.onAllNodes(hasContentDescriptionStartingWith("streak "))
                .fetchSemanticsNodes().isEmpty()
        )

        // Import the exported JSON back
        runBlocking {
            val result = TestSeeder.container.backupRepository.importJson(exported)
            assertTrue(result.ok)
        }
        advance(800)

        // Identical state: streak restored, today green, diet + mood + note present
        waitForContentDescription("streak 1")
        composeRule.onNode(hasContentDescriptionStartingWith("calendar day $today green")).assertExists()

        val dayState = runBlocking { TestSeeder.container.database.dayDao().dayState(today) }
        assertEquals("E2E note", dayState?.note)
        assertEquals("🔥", dayState?.mood)
        assertEquals(1, dayState?.waterCount)
        assertTrue((dayState?.proteinTotal ?: 0.0) >= 32.0) // 12g egg + 20g quick add

        // ---------------------------------------------------------------
        // 6. Theme toggle persists
        // ---------------------------------------------------------------
        composeRule.onNodeWithContentDescription("theme toggle").performClick()
        advance(400)
        assertEquals("light", runBlocking { TestSeeder.container.settings.theme.firstOrNull() })
        composeRule.onNodeWithContentDescription("theme toggle").performClick()
        advance(400)
        assertEquals("dark", runBlocking { TestSeeder.container.settings.theme.firstOrNull() })

        // ---------------------------------------------------------------
        // 7. Weight log + BF calculator
        // ---------------------------------------------------------------
        scrollToIn("homeList", hasTestTag("wtInput"))
        composeRule.onNodeWithTag("wtInput").performTextInput("77.5")
        advance(200)
        composeRule.onNodeWithText("LOG").performClick()
        advance(600)
        waitForText("77.5kg logged")

        val weights = runBlocking { TestSeeder.container.database.bodyDao().allWeights() }
        assertEquals(1, weights.size)
        assertEquals(77.5, weights[0].weight, 0.001)

        composeRule.onNodeWithContentDescription("header stats").performClick()
        advance()
        composeRule.onNodeWithTag("bfHeight").performTextReplacement("175")
        composeRule.onNodeWithTag("bfNeck").performTextReplacement("38")
        composeRule.onNodeWithTag("bfWaist").performTextReplacement("85")
        advance(200)
        composeRule.onNodeWithText("CALCULATE & LOG").performClick()
        advance(600)
        waitForText("16.9% Body Fat")
    }
}

private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.firstOrNull(): T? {
    var result: T? = null
    var done = false
    collect { value ->
        if (!done) {
            result = value
            done = true
        }
    }
    return result
}
