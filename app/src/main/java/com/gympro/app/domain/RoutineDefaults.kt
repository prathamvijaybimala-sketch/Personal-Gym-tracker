package com.gympro.app.domain

/**
 * Default seed data — exact port of the web app's defaultWorkouts / defaultDiet /
 * defaultSupps constants (PPL split with exrx.net / strengthlog.com links).
 */
object RoutineDefaults {

    data class ExSeed(val id: String, val n: String, val t: String, val l: String)
    data class DaySeed(val n: String, val ex: List<ExSeed>)

    val defaultWorkouts: Map<Int, DaySeed> = mapOf(
        1 to DaySeed("PUSH", listOf(
            ExSeed("cp", "Chest Press", "3×10", "https://exrx.net/WeightExercises/PectoralSternal/LVChestPressS"),
            ExSeed("idp", "Incline DB Press", "2×8", "https://exrx.net/WeightExercises/PectoralClavicular/DBInclineBenchPress"),
            ExSeed("lr", "Lateral Raises", "3×12", "https://exrx.net/WeightExercises/DeltoidLateral/DBLateralRaise"),
            ExSeed("tp", "Tri Pushdown", "2×12", "https://exrx.net/WeightExercises/Triceps/CBPushdown"),
        )),
        2 to DaySeed("PULL", listOf(
            ExSeed("lp", "Lat Pulldown", "3×10", "https://exrx.net/WeightExercises/LatissimusDorsi/CBFrontPulldown"),
            ExSeed("sr", "Cable Row", "3×10", "https://exrx.net/WeightExercises/BackGeneral/CBSeatedRow"),
            ExSeed("sh", "DB Shrugs", "2×12", "https://exrx.net/WeightExercises/TrapeziusUpper/DBShrug"),
            ExSeed("bc", "Bicep Curl", "2×12", "https://exrx.net/WeightExercises/Biceps/CBCurl"),
        )),
        3 to DaySeed("LEGS", listOf(
            ExSeed("lpr", "Leg Press", "3×12", "https://www.strengthlog.com/leg-press/"),
            ExSeed("lc", "Leg Curl", "3×10", "https://exrx.net/WeightExercises/Hamstrings/LVLyingLegCurl"),
            ExSeed("cr", "Calf Raise", "3×15", "https://exrx.net/WeightExercises/Gastrocnemius/BWStandingCalfRaise"),
            ExSeed("bd", "Bird Dog", "2×10", "https://exrx.net/WeightExercises/ErectorSpinae/BWBirdDog"),
        )),
        4 to DaySeed("PUSH B", listOf(
            ExSeed("pd", "Pec Deck", "3×12", "https://exrx.net/WeightExercises/PectoralSternal/LVPecDeckFly"),
            ExSeed("sp", "Seated Press", "2×8", "https://exrx.net/WeightExercises/DeltoidAnterior/DBShoulderPress"),
            ExSeed("te", "Tri Extension", "2×10", "https://exrx.net/WeightExercises/Triceps/DBTriExt"),
        )),
        5 to DaySeed("PULL B", listOf(
            ExSeed("ulp", "Underhand Pull", "3×10", "https://exrx.net/WeightExercises/LatissimusDorsi/CBUnderhandPulldown"),
            ExSeed("dr", "DB Row", "2×10", "https://exrx.net/WeightExercises/BackGeneral/DBBentOverRow"),
            ExSeed("hc", "Hammer Curl", "2×10", "https://exrx.net/WeightExercises/Brachioradialis/DBHammerCurl"),
        )),
        6 to DaySeed("LEGS B", listOf(
            ExSeed("gs", "Goblet Squat", "3×10", "https://www.strengthlog.com/goblet-squat/"),
            ExSeed("rdl", "DB RDL", "2×10", "https://www.strengthlog.com/dumbbell-romanian-deadlift/"),
            ExSeed("pk", "Plank", "2×45s", "https://exrx.net/WeightExercises/RectusAbdominis/BWFrontPlank"),
        )),
        0 to DaySeed("REST", emptyList()),
    )

    val defaultDiet = listOf(
        Triple("e1", "2 Eggs", 12.0), Triple("w1", "Whey Scoop", 24.0),
        Triple("d1", "Dal Bowl", 7.0), Triple("c1", "Curd Bowl", 5.0),
        Triple("rt", "3 Roti", 9.0), Triple("ch", "Chicken Breast", 25.0),
        Triple("pn", "Paneer 100g", 18.0), Triple("sy", "Soya 50g", 26.0),
    )

    val defaultSupps = listOf(
        Triple("cr", "Creatine", "daily"),
        Triple("mv", "Multivitamin", "alt"),
        Triple("d3", "Vit D3", "6"),
    )

    /** Empty 7-day template used when creating a brand-new routine. */
    fun emptyDays(): Map<Int, DaySeed> = (0..6).associateWith { d ->
        DaySeed(if (d == 0) "REST" else "DAY $d", emptyList())
    }
}
