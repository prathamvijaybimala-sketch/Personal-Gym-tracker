package com.gympro.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// ---------------------------------------------------------------------------
// Room entities — one per logical localStorage group from the web app.
// ---------------------------------------------------------------------------

/** routines[] — a routine's 7 days are stored as JSON (daysJson) keyed "0".."6". */
@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val daysJson: String,
    val position: Int,
)

/** h_<exerciseId> — per-exercise history rows, insertion order preserved. */
@Entity(tableName = "exercise_log")
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: String,
    val date: String,
    val weight: Double,
    val reps: Int,
)

/** t_<exerciseId>_<date> — today's in-progress draft (raw strings, like the web). */
@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val exerciseId: String,
    val date: String,
    val weight: String,
    val reps: String,
)

/** gatt — attendance per date. */
@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey val date: String,
    val done: Boolean = true,
)

/**
 * Per-date day state: water_<date>, mood_<date>, note_<date>, de_<date>
 * (extra protein) and dlog (computed daily protein total).
 */
@Entity(tableName = "day_state")
data class DayStateEntity(
    @PrimaryKey val date: String,
    val extraProtein: Double = 0.0,
    val waterCount: Int = 0,
    val mood: String? = null,
    val note: String? = null,
    val proteinTotal: Double = 0.0,
)

/** dc_<date> — checked diet + supplement item ids for a date. */
@Entity(tableName = "diet_checks")
data class DietCheckEntity(
    @PrimaryKey val date: String,
    val itemId: String,
)

/** bw_log — body weight per date. */
@Entity(tableName = "body_weight")
data class BodyWeightEntity(
    @PrimaryKey val date: String,
    val weight: Double,
)

/** bfLog — body-fat entries {val, w: waist, n: neck} per date. */
@Entity(tableName = "body_fat")
data class BodyFatEntity(
    @PrimaryKey val date: String,
    val value: Double,
    val waist: Double,
    val neck: Double,
)

/** cardioLog — cardio sessions. */
@Entity(tableName = "cardio")
data class CardioEntity(
    @PrimaryKey val id: String,
    val date: String,
    val type: String,
    val duration: Int,
    val calories: Int?,
    val distance: Double?,
    val notes: String,
)

/** dietConfig — tracked food items with protein grams. */
@Entity(tableName = "diet_items")
data class DietItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val protein: Double,
    val position: Int,
)

/** suppConfig — supplements with a schedule: daily / alt / weekday number. */
@Entity(tableName = "supp_items")
data class SuppItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val schedule: String,
    val position: Int,
)
