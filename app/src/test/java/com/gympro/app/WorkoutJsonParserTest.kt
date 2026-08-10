package com.gympro.app

import com.gympro.app.domain.WorkoutJsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutJsonParserTest {

    @Test
    fun `parses format A - direct days object`() {
        val json = """
            {"name":"Full Body","days":{"1":{"n":"PUSH","ex":[
                {"name":"Bench Press","sets":"3","reps":"10","link":"https://exrx.net/bench"}
            ]}}}
        """.trimIndent()
        val draft = WorkoutJsonParser.parse(json)
        assertNotNull(draft)
        assertEquals("Full Body", draft!!.name)
        val day1 = draft.days[1]
        assertNotNull(day1)
        assertEquals("PUSH", day1!!.name)
        assertEquals(1, day1.exercises.size)
        assertEquals("Bench Press", day1.exercises[0].name)
        // Format A maps sets -> target when t is missing
        assertEquals("3", day1.exercises[0].target)
        assertEquals("https://exrx.net/bench", day1.exercises[0].link)
    }

    @Test
    fun `format A fills missing days as rest`() {
        val json = """{"name":"X","days":{"3":{"n":"LEGS","ex":[]}}}"""
        val draft = WorkoutJsonParser.parse(json)!!
        assertEquals(7, draft.days.size)
        assertEquals("REST", draft.days[0]!!.name)
        assertEquals("DAY 1", draft.days[1]!!.name)
        assertEquals("LEGS", draft.days[3]!!.name)
        assertTrue(draft.days[0]!!.exercises.isEmpty())
    }

    @Test
    fun `parses format B - AI days array with sets and reps`() {
        val json = """
            {"name":"AI Split","days":[
                {"day":1,"name":"PUSH","exercises":[
                    {"name":"Bench Press","sets":"3","reps":"10","link":"https://exrx.net/bench"}
                ]},
                {"day":0,"name":"REST","exercises":[]}
            ]}
        """.trimIndent()
        val draft = WorkoutJsonParser.parse(json)
        assertNotNull(draft)
        assertEquals("AI Split", draft!!.name)
        val day1 = draft.days[1]!!
        assertEquals("PUSH", day1.name)
        assertEquals(1, day1.exercises.size)
        // Format B joins sets × reps with the multiplication sign (U+00D7)
        assertEquals("3×10", day1.exercises[0].target)
        assertEquals("Bench Press", day1.exercises[0].name)
        assertEquals("https://exrx.net/bench", day1.exercises[0].link)
        assertEquals("REST", draft.days[0]!!.name)
    }

    @Test
    fun `format B default day is 0 when missing`() {
        val json = """{"name":"X","days":[{"name":"REST","exercises":[]}]}"""
        val draft = WorkoutJsonParser.parse(json)!!
        assertEquals("REST", draft.days[0]!!.name)
    }

    @Test
    fun `format A supports name-based fields`() {
        val json = """{"name":"X","days":{"2":{"name":"PULL","exercises":[{"name":"Row","sets":"4","reps":"8"}]}}}"""
        val draft = WorkoutJsonParser.parse(json)!!
        assertEquals("PULL", draft.days[2]!!.name)
        assertEquals("4", draft.days[2]!!.exercises[0].target)
    }

    @Test
    fun `format B supports alternate field names`() {
        val json = """{"name":"X","days":[{"day":5,"n":"PULL B","ex":[{"n":"Curl","t":"2×12","l":"https://exrx.net/curl"}]}]}"""
        val draft = WorkoutJsonParser.parse(json)!!
        assertEquals("PULL B", draft.days[5]!!.name)
        assertEquals("2×12", draft.days[5]!!.exercises[0].target)
        assertEquals("https://exrx.net/curl", draft.days[5]!!.exercises[0].link)
    }

    @Test
    fun `invalid json returns null`() {
        assertNull(WorkoutJsonParser.parse("not json at all"))
        assertNull(WorkoutJsonParser.parse(""))
        assertNull(WorkoutJsonParser.parse("{broken"))
    }

    @Test
    fun `missing days structure returns null`() {
        assertNull(WorkoutJsonParser.parse("""{"name":"X"}"""))
        assertNull(WorkoutJsonParser.parse("""{"name":"X","days":"nope"}"""))
    }

    @Test
    fun `generated exercise ids are unique per exercise`() {
        val json = """{"name":"X","days":[{"day":1,"name":"A","exercises":[
            {"name":"One","sets":"3","reps":"10"},
            {"name":"Two","sets":"3","reps":"10"}
        ]}]}"""
        val draft = WorkoutJsonParser.parse(json)!!
        val ids = draft.days[1]!!.exercises.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
        assertTrue(ids.all { it.startsWith("ex") })
    }

    @Test
    fun `format A keeps provided exercise ids`() {
        val json = """{"name":"X","days":{"1":{"n":"PUSH","ex":[{"id":"cp","n":"Chest Press","t":"3×10"}]}}}"""
        val draft = WorkoutJsonParser.parse(json)!!
        assertEquals("cp", draft.days[1]!!.exercises[0].id)
    }
}
