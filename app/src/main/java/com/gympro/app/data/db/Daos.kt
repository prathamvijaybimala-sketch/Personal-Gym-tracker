package com.gympro.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY position ASC")
    fun observeAll(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines ORDER BY position ASC")
    suspend fun getAll(): List<RoutineEntity>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getById(id: String): RoutineEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM routines")
    suspend fun maxPosition(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(routine: RoutineEntity)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM routines")
    suspend fun deleteAll()
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise_log WHERE exerciseId = :exerciseId ORDER BY id ASC")
    suspend fun history(exerciseId: String): List<ExerciseLogEntity>

    @Query("SELECT * FROM exercise_log ORDER BY id ASC")
    suspend fun allHistory(): List<ExerciseLogEntity>

    @Query("SELECT * FROM exercise_log ORDER BY id ASC")
    fun observeAllHistory(): Flow<List<ExerciseLogEntity>>

    @Query("SELECT * FROM exercise_log WHERE date = :date")
    suspend fun historyForDate(date: String): List<ExerciseLogEntity>

    @Insert
    suspend fun insert(log: ExerciseLogEntity)

    @Query("DELETE FROM exercise_log WHERE exerciseId = :exerciseId")
    suspend fun deleteForExercise(exerciseId: String)

    @Query("DELETE FROM exercise_log")
    suspend fun deleteAll()

    @Query("SELECT * FROM drafts WHERE exerciseId = :exerciseId AND date = :date")
    suspend fun draft(exerciseId: String, date: String): DraftEntity?

    @Query("SELECT * FROM drafts WHERE date = :date")
    suspend fun draftsForDate(date: String): List<DraftEntity>

    @Query("SELECT * FROM drafts WHERE date = :date")
    fun observeDraftsForDate(date: String): Flow<List<DraftEntity>>

    @Query("SELECT * FROM drafts")
    suspend fun allDrafts(): List<DraftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(draft: DraftEntity)

    @Delete
    suspend fun deleteDraft(draft: DraftEntity)

    @Query("DELETE FROM drafts")
    suspend fun deleteAllDrafts()
}

@Dao
interface DayDao {
    @Query("SELECT * FROM attendance ORDER BY date ASC")
    fun observeAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance ORDER BY date ASC")
    suspend fun allAttendance(): List<AttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(attendance: AttendanceEntity)

    @Query("DELETE FROM attendance")
    suspend fun deleteAllAttendance()

    @Query("SELECT * FROM day_state ORDER BY date ASC")
    fun observeDayStates(): Flow<List<DayStateEntity>>

    @Query("SELECT * FROM day_state ORDER BY date ASC")
    suspend fun allDayStates(): List<DayStateEntity>

    @Query("SELECT * FROM day_state WHERE date = :date")
    suspend fun dayState(date: String): DayStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDayState(state: DayStateEntity)

    @Query("DELETE FROM day_state")
    suspend fun deleteAllDayStates()

    @Query("SELECT * FROM diet_checks ORDER BY date ASC")
    fun observeChecks(): Flow<List<DietCheckEntity>>

    @Query("SELECT * FROM diet_checks WHERE date = :date")
    suspend fun checks(date: String): List<DietCheckEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChecks(checks: List<DietCheckEntity>)

    @Query("DELETE FROM diet_checks WHERE date = :date")
    suspend fun deleteChecks(date: String)

    @Query("DELETE FROM diet_checks")
    suspend fun deleteAllChecks()
}

@Dao
interface BodyDao {
    @Query("SELECT * FROM body_weight ORDER BY date ASC")
    fun observeWeights(): Flow<List<BodyWeightEntity>>

    @Query("SELECT * FROM body_weight ORDER BY date ASC")
    suspend fun allWeights(): List<BodyWeightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeight(weight: BodyWeightEntity)

    @Query("DELETE FROM body_weight")
    suspend fun deleteAllWeights()

    @Query("SELECT * FROM body_fat ORDER BY date ASC")
    fun observeBodyFat(): Flow<List<BodyFatEntity>>

    @Query("SELECT * FROM body_fat ORDER BY date ASC")
    suspend fun allBodyFat(): List<BodyFatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBodyFat(bf: BodyFatEntity)

    @Query("DELETE FROM body_fat")
    suspend fun deleteAllBodyFat()

    @Query("SELECT * FROM cardio ORDER BY date ASC")
    suspend fun allCardio(): List<CardioEntity>

    @Query("SELECT * FROM cardio ORDER BY date ASC, id ASC")
    fun observeCardio(): Flow<List<CardioEntity>>

    @Insert
    suspend fun insertCardio(cardio: CardioEntity)

    @Query("DELETE FROM cardio WHERE id = :id")
    suspend fun deleteCardio(id: String)

    @Query("DELETE FROM cardio")
    suspend fun deleteAllCardio()
}

@Dao
interface ConfigDao {
    @Query("SELECT * FROM diet_items ORDER BY position ASC")
    fun observeDietItems(): Flow<List<DietItemEntity>>

    @Query("SELECT * FROM diet_items ORDER BY position ASC")
    suspend fun dietItems(): List<DietItemEntity>

    @Query("SELECT * FROM supp_items ORDER BY position ASC")
    fun observeSuppItems(): Flow<List<SuppItemEntity>>

    @Query("SELECT * FROM supp_items ORDER BY position ASC")
    suspend fun suppItems(): List<SuppItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDietItem(item: DietItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSuppItem(item: SuppItemEntity)

    @Query("DELETE FROM diet_items WHERE id = :id")
    suspend fun deleteDietItem(id: String)

    @Query("DELETE FROM supp_items WHERE id = :id")
    suspend fun deleteSuppItem(id: String)

    @Query("DELETE FROM diet_items")
    suspend fun deleteAllDietItems()

    @Query("DELETE FROM supp_items")
    suspend fun deleteAllSuppItems()
}
