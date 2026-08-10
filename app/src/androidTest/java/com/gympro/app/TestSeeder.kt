package com.gympro.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gympro.app.data.AppContainer
import com.gympro.app.data.db.AttendanceEntity
import com.gympro.app.data.db.BodyFatEntity
import com.gympro.app.data.db.BodyWeightEntity
import com.gympro.app.data.db.CardioEntity
import com.gympro.app.data.db.DayStateEntity
import com.gympro.app.data.db.ExerciseLogEntity
import com.gympro.app.data.db.RoutineEntity
import com.gympro.app.data.db.DaysCodec
import com.gympro.app.domain.Dates
import kotlinx.coroutines.runBlocking

/**
 * Seeds deterministic data through the real AppContainer so UI tests can
 * assert against known values and screenshots look rich.
 */
object TestSeeder {

    val container: AppContainer
        get() = (ApplicationProvider.getApplicationContext<GymProApp>()).container

    private fun dayKey(offsetDays: Long): String = Dates.key(Dates.today().plusDays(offsetDays))

    /** Full wipe + defaults (fresh app state). */
    fun reset() = runBlocking {
        container.wipeAll()
        container.ensureSeeded()
    }

    /**
     * Rich demo state:
     * - PPL routine active, plus a second routine "FULL BODY"
     * - workout history for "cp" (50kg×10 => est 1RM 67) and others on past days
     * - attendance on the last 3 days (streak 3)
     * - body weight log + body-fat log
     * - today's diet checks, quick protein, water, mood, note
     * - cardio sessions
     */
    fun seedRichState() = runBlocking {
        reset()

        // Second routine for the routine manager screenshot
        val dao = container.database.routineDao()
        val days = RoutineDefaultsMin.emptyDaysFor("FULL BODY")
        dao.upsert(
            RoutineEntity(
                id = "r-seed2",
                name = "FULL BODY",
                daysJson = DaysCodec.encode(days),
                position = 1,
            )
        )

        // History + attendance: streak of 3 (yesterday, -2, -3)
        val exerciseDao = container.database.exerciseDao()
        exerciseDao.insert(ExerciseLogEntity(exerciseId = "cp", date = dayKey(-3), weight = 45.0, reps = 10))
        exerciseDao.insert(ExerciseLogEntity(exerciseId = "cp", date = dayKey(-2), weight = 47.5, reps = 10))
        exerciseDao.insert(ExerciseLogEntity(exerciseId = "cp", date = dayKey(-1), weight = 50.0, reps = 10))
        exerciseDao.insert(ExerciseLogEntity(exerciseId = "lr", date = dayKey(-2), weight = 10.0, reps = 12))
        exerciseDao.insert(ExerciseLogEntity(exerciseId = "lr", date = dayKey(-1), weight = 12.5, reps = 12))
        exerciseDao.insert(ExerciseLogEntity(exerciseId = "tp", date = dayKey(-1), weight = 20.0, reps = 12))

        val dayDao = container.database.dayDao()
        dayDao.upsertAttendance(AttendanceEntity(dayKey(-3)))
        dayDao.upsertAttendance(AttendanceEntity(dayKey(-2)))
        dayDao.upsertAttendance(AttendanceEntity(dayKey(-1)))

        // Body weight + BF history
        val bodyDao = container.database.bodyDao()
        bodyDao.upsertWeight(BodyWeightEntity(dayKey(-6), 74.0))
        bodyDao.upsertWeight(BodyWeightEntity(dayKey(-3), 75.0))
        bodyDao.upsertWeight(BodyWeightEntity(dayKey(-1), 76.2))
        bodyDao.upsertBodyFat(BodyFatEntity(dayKey(-4), 18.2, 82.0, 38.0))
        bodyDao.upsertBodyFat(BodyFatEntity(dayKey(-1), 17.5, 81.0, 38.0))
        container.settings.setUwt("76.2")

        // Diet: checked items + quick protein (total = 12 + 20 + 24 = 56g for today's checks e1)
        val configDao = container.database.configDao()
        val today = Dates.todayKey()
        dayDao.upsertChecks(
            listOf(
                com.gympro.app.data.db.DietCheckEntity(today, "e1"),
                com.gympro.app.data.db.DietCheckEntity(today, "cr"),
            )
        )
        dayDao.upsertDayState(
            DayStateEntity(
                date = today,
                extraProtein = 20.0,
                waterCount = 5,
                mood = "💪",
                note = "Great session today",
                proteinTotal = 0.0, // recomputed below
            )
        )
        container.dietRepository.recomputeProteinTotal(today)

        // Past-day diet data so the 7-day chart has bars
        dayDao.upsertDayState(DayStateEntity(date = dayKey(-1), extraProtein = 40.0, waterCount = 8, proteinTotal = 112.0))
        dayDao.upsertDayState(DayStateEntity(date = dayKey(-2), extraProtein = 0.0, waterCount = 6, proteinTotal = 95.0))
        dayDao.upsertDayState(DayStateEntity(date = dayKey(-3), extraProtein = 0.0, waterCount = 3, proteinTotal = 60.0))
        dayDao.upsertDayState(DayStateEntity(date = dayKey(-4), extraProtein = 0.0, waterCount = 7, proteinTotal = 88.0))
        dayDao.upsertDayState(DayStateEntity(date = dayKey(-5), extraProtein = 0.0, waterCount = 4, proteinTotal = 45.0))
        dayDao.upsertDayState(DayStateEntity(date = dayKey(-6), extraProtein = 0.0, waterCount = 8, proteinTotal = 120.0))

        // Cardio
        bodyDao.insertCardio(
            CardioEntity(
                id = "c1", date = dayKey(-1), type = "Treadmill", duration = 30,
                calories = 320, distance = 4.5, notes = "Zone 2",
            )
        )
        bodyDao.insertCardio(
            CardioEntity(
                id = "c2", date = today, type = "Jump Rope", duration = 15,
                calories = 150, distance = null, notes = "",
            )
        )
    }

    /** History max for "cp" is 67 (50kg×10). Returns the current day key. */
    fun todayKey(): String = Dates.todayKey()
}

/** Minimal default-days helper mirroring RoutineDefaults (kept out of the main package). */
private object RoutineDefaultsMin {
    fun emptyDaysFor(name: String): Map<Int, DaysCodec.DayData> {
        val base = (0..6).associateWith { d ->
            if (d == 0) DaysCodec.DayData("REST", emptyList())
            else DaysCodec.DayData("DAY $d", emptyList())
        }
        // Give the second routine one training day with exercises
        return base.toMutableMap().apply {
            this[1] = DaysCodec.DayData(
                "PUSH",
                listOf(
                    DaysCodec.ExerciseData("fb1", "Bench Press", "3×10", "https://exrx.net/bench"),
                    DaysCodec.ExerciseData("fb2", "Overhead Press", "3×8", ""),
                ),
            )
        }
    }
}
