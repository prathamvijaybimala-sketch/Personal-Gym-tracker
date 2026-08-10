package com.gympro.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RoutineEntity::class,
        ExerciseLogEntity::class,
        DraftEntity::class,
        AttendanceEntity::class,
        DayStateEntity::class,
        DietCheckEntity::class,
        BodyWeightEntity::class,
        BodyFatEntity::class,
        CardioEntity::class,
        DietItemEntity::class,
        SuppItemEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun dayDao(): DayDao
    abstract fun bodyDao(): BodyDao
    abstract fun configDao(): ConfigDao
}
