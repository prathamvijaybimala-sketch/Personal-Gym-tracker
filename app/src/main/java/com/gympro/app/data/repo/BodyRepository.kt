package com.gympro.app.data.repo

import com.gympro.app.data.db.BodyDao
import com.gympro.app.data.db.BodyFatEntity
import com.gympro.app.data.db.BodyWeightEntity
import com.gympro.app.data.db.CardioEntity
import com.gympro.app.data.settings.SettingsRepository
import com.gympro.app.domain.BodyFatCalculator
import com.gympro.app.domain.Dates
import kotlinx.coroutines.flow.Flow

/** Body weight / body fat / cardio — ports of saveWeight, calcAndSaveBF, logCardio. */
class BodyRepository(
    private val bodyDao: BodyDao,
    private val settings: SettingsRepository,
) {

    fun observeWeights(): Flow<List<BodyWeightEntity>> = bodyDao.observeWeights()
    fun observeBodyFat(): Flow<List<BodyFatEntity>> = bodyDao.observeBodyFat()
    fun observeCardio(): Flow<List<CardioEntity>> = bodyDao.observeCardio()
    val uwt: Flow<String> = settings.uwt

    suspend fun logWeight(kg: Double) {
        bodyDao.upsertWeight(BodyWeightEntity(date = Dates.todayKey(), weight = kg))
        settings.setUwt(kg.toString())
    }

    suspend fun allWeights(): List<BodyWeightEntity> = bodyDao.allWeights()

    /** US Navy calculation + persistence (height saved for next time). */
    suspend fun calcAndLogBodyFat(heightCm: Double, neckCm: Double, waistCm: Double): Double {
        settings.setBfHeight(heightCm.toString())
        val value = BodyFatCalculator.navy(heightCm, neckCm, waistCm)
        bodyDao.upsertBodyFat(BodyFatEntity(date = Dates.todayKey(), value = value, waist = waistCm, neck = neckCm))
        return value
    }

    suspend fun allBodyFat(): List<BodyFatEntity> = bodyDao.allBodyFat()

    suspend fun allCardio(): List<CardioEntity> = bodyDao.allCardio()

    suspend fun logCardio(entry: CardioEntity) = bodyDao.insertCardio(entry)

    suspend fun deleteCardio(id: String) = bodyDao.deleteCardio(id)
}
