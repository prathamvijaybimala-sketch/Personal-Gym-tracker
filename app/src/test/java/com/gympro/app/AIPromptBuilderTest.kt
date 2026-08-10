package com.gympro.app

import com.gympro.app.domain.AIPromptBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AIPromptBuilderTest {

    @Test
    fun `prompt embeds the requested routine type`() {
        val prompt = AIPromptBuilder.build("6-day PPL")
        assertTrue(prompt.startsWith("Generate a 6-day PPL gym workout routine for me."))
        assertTrue(prompt.contains("- Routine type: 6-day PPL"))
    }

    @Test
    fun `prompt contains the exact JSON schema block`() {
        val prompt = AIPromptBuilder.build("PPL")
        assertTrue(prompt.contains("""Return ONLY a valid JSON object — no explanation, no markdown, no extra text."""))
        assertTrue(prompt.contains(""""day": 1"""))
        assertTrue(prompt.contains(""""day": 0"""))
        assertTrue(prompt.contains(""""exercises": []"""))
        assertTrue(prompt.contains(""""sets": "3","""))
        assertTrue(prompt.contains(""""reps": "10","""))
        assertTrue(prompt.contains(""""link": "https://exrx.net/...""""))
    }

    @Test
    fun `prompt contains the exact rules with unicode dashes`() {
        val prompt = AIPromptBuilder.build("PPL")
        // en dash in 0–6, ellipsis in the weekday list
        assertTrue(prompt.contains(""""day" must be 0–6 (0 = Sunday, 1 = Monday, … 6 = Saturday)"""))
        assertTrue(prompt.contains("- Include all 7 days (0–6). Use \"REST\" with empty exercises for rest days"))
        assertTrue(prompt.contains(""""sets" and "reps" are strings (e.g. "3" and "10")"""))
        assertTrue(prompt.contains("- Include 3–6 exercises per training day"))
    }

    @Test
    fun `prompt does not include markdown fences`() {
        val prompt = AIPromptBuilder.build("PPL")
        assertTrue(!prompt.contains("```"))
    }

    @Test
    fun `same type yields identical prompt (deterministic)`() {
        assertEquals(AIPromptBuilder.build("4-day Upper Lower"), AIPromptBuilder.build("4-day Upper Lower"))
    }
}
