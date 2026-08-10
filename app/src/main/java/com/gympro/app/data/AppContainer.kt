package com.gympro.app.data

import android.content.Context
import androidx.room.Room
import com.gympro.app.data.db.GymDatabase
import com.gympro.app.data.repo.BackupRepository
import com.gympro.app.data.repo.BodyRepository
import com.gympro.app.data.repo.DietRepository
import com.gympro.app.data.repo.RoutineRepository
import com.gympro.app.data.repo.WorkoutRepository
import com.gympro.app.data.settings.SettingsRepository

/**
 * Manual dependency container — one per process. The instrumented tests use
 * this same container to seed and assert state.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: GymDatabase = Room.databaseBuilder(
        appContext,
        GymDatabase::class.java,
        "gympro.db"
    ).fallbackToDestructiveMigration().build()

    val settings = SettingsRepository(appContext)

    val routineRepository = RoutineRepository(database.routineDao(), settings)
    val workoutRepository = WorkoutRepository(database, database.exerciseDao(), database.dayDao())
    val dietRepository = DietRepository(database.dayDao(), database.configDao(), settings)
    val bodyRepository = BodyRepository(database.bodyDao(), settings)
    val backupRepository = BackupRepository(
        database,
        database.routineDao(),
        database.exerciseDao(),
        database.dayDao(),
        database.bodyDao(),
        database.configDao(),
        settings,
    )

    /** Seed defaults (first run) or restore after a wipe. */
    suspend fun ensureSeeded() {
        routineRepository.ensureSeeded()
        dietRepository.ensureSeeded()
    }

    /** Wipe everything — used by "restore" and by tests. */
    suspend fun wipeAll() {
        database.clearAllTables()
        settings.clear()
    }
}
