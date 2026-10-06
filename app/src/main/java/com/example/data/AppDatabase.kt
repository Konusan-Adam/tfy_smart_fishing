package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.engine.RodAction
import com.example.engine.RodCategory
import com.example.engine.RodMaterial
import com.example.engine.WaterEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TrophyCatch::class, RodProfile::class, WeatherEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trophyDao(): TrophyDao
    abstract fun rodProfileDao(): RodProfileDao
    abstract fun weatherDao(): WeatherDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "balik_zili_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getDatabase(context).rodProfileDao()
                                if (dao.getCount() < 10) {
                                    dao.clearAll()
                                    dao.insertAll(DEFAULT_PRESETS)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_PRESETS = listOf(
            // ==================== 🎋 1. GÖL & ŞAMANDIRA / TELESKOPİK KAMIŞLAR (5.00m - 9.00m) ====================
            RodProfile(
                name = "Albastar Bosphorus Pole (7.00 m - Yüksek Karbon)",
                lengthMeters = 7.00,
                sensorDistanceCm = 80,
                testCurveLbs = 2.00,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.POLE_LAKE.name,
                isPreset = true
            ),
            RodProfile(
                name = "Albastar Bosphorus Pole (8.00 m - Yüksek Karbon)",
                lengthMeters = 8.00,
                sensorDistanceCm = 95,
                testCurveLbs = 2.20,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.POLE_LAKE.name,
                isPreset = true
            ),
            RodProfile(
                name = "Albastar Bosphorus Pole (6.00 m - Karbon)",
                lengthMeters = 6.00,
                sensorDistanceCm = 70,
                testCurveLbs = 1.80,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.POLE_LAKE.name,
                isPreset = true
            ),
            RodProfile(
                name = "Lineaeffe Delta Carbon Pole (7.00 m)",
                lengthMeters = 7.00,
                sensorDistanceCm = 75,
                testCurveLbs = 2.00,
                materialType = RodMaterial.STANDARD_CARBON_COMPOSITE.name,
                actionType = RodAction.MEDIUM_FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.POLE_LAKE.name,
                isPreset = true
            ),
            RodProfile(
                name = "Lineaeffe Delta Carbon Pole (8.00 m)",
                lengthMeters = 8.00,
                sensorDistanceCm = 90,
                testCurveLbs = 2.25,
                materialType = RodMaterial.STANDARD_CARBON_COMPOSITE.name,
                actionType = RodAction.MEDIUM_FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.POLE_LAKE.name,
                isPreset = true
            ),
            RodProfile(
                name = "Trabucco Energhia XR Pole (7.00 m - 40T Karbon)",
                lengthMeters = 7.00,
                sensorDistanceCm = 85,
                testCurveLbs = 2.10,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.POLE_LAKE.name,
                isPreset = true
            ),
            RodProfile(
                name = "Trabucco Activa Pole (8.00 m - High Modulus)",
                lengthMeters = 8.00,
                sensorDistanceCm = 100,
                testCurveLbs = 2.30,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.MEDIUM_FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.POLE_LAKE.name,
                isPreset = true
            ),
            RodProfile(
                name = "Tubertini Area Pro 7400 (7.00 m)",
                lengthMeters = 7.00,
                sensorDistanceCm = 80,
                testCurveLbs = 2.00,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.POLE_LAKE.name,
                isPreset = true
            ),
            RodProfile(
                name = "Tubertini Area Pro 7400 (8.00 m)",
                lengthMeters = 8.00,
                sensorDistanceCm = 95,
                testCurveLbs = 2.20,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.POLE_LAKE.name,
                isPreset = true
            ),
            RodProfile(
                name = "DAM Shadow Tele Pole (6.00 m - Kompozit)",
                lengthMeters = 6.00,
                sensorDistanceCm = 65,
                testCurveLbs = 1.90,
                materialType = RodMaterial.STANDARD_CARBON_COMPOSITE.name,
                actionType = RodAction.MEDIUM_PARABOLIC.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.POLE_LAKE.name,
                isPreset = true
            ),
            RodProfile(
                name = "DAM Shadow Tele Pole (7.00 m - Kompozit)",
                lengthMeters = 7.00,
                sensorDistanceCm = 75,
                testCurveLbs = 2.10,
                materialType = RodMaterial.STANDARD_CARBON_COMPOSITE.name,
                actionType = RodAction.MEDIUM_PARABOLIC.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.POLE_LAKE.name,
                isPreset = true
            ),
            RodProfile(
                name = "Captain Bosphorus Göl Kamışı (7.00 m)",
                lengthMeters = 7.00,
                sensorDistanceCm = 70,
                testCurveLbs = 2.00,
                materialType = RodMaterial.STANDARD_CARBON_COMPOSITE.name,
                actionType = RodAction.MEDIUM_FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.POLE_LAKE.name,
                isPreset = true
            ),
            RodProfile(
                name = "Captain Bosphorus Göl Kamışı (8.00 m)",
                lengthMeters = 8.00,
                sensorDistanceCm = 85,
                testCurveLbs = 2.20,
                materialType = RodMaterial.STANDARD_CARBON_COMPOSITE.name,
                actionType = RodAction.MEDIUM_FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.POLE_LAKE.name,
                isPreset = true
            ),
            RodProfile(
                name = "Remixon Master Pole (7.00 m - Karbon)",
                lengthMeters = 7.00,
                sensorDistanceCm = 75,
                testCurveLbs = 2.00,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.POLE_LAKE.name,
                isPreset = true
            ),

            // ==================== 🎣 2. DÜNYA STANDARDI SAZAN KAMIŞLARI (CARP 12ft / 13ft) ====================
            RodProfile(
                name = "Fox Horizon X5-S (3.90 m - 3.75 lb)",
                lengthMeters = 3.90,
                sensorDistanceCm = 70,
                testCurveLbs = 3.75,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.CARP.name,
                isPreset = true
            ),
            RodProfile(
                name = "Fox Horizon X4 (3.60 m - 3.50 lb)",
                lengthMeters = 3.60,
                sensorDistanceCm = 60,
                testCurveLbs = 3.50,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.CARP.name,
                isPreset = true
            ),
            RodProfile(
                name = "Fox Horizon X3 (3.60 m - 3.25 lb)",
                lengthMeters = 3.60,
                sensorDistanceCm = 55,
                testCurveLbs = 3.25,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.MEDIUM_FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.CARP.name,
                isPreset = true
            ),
            RodProfile(
                name = "Nash Scope Black Ops (3.00 m - 3.50 lb)",
                lengthMeters = 3.00,
                sensorDistanceCm = 45,
                testCurveLbs = 3.50,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.CARP.name,
                isPreset = true
            ),
            RodProfile(
                name = "Nash Dwarf Carp (3.60 m - 3.25 lb)",
                lengthMeters = 3.60,
                sensorDistanceCm = 55,
                testCurveLbs = 3.25,
                materialType = RodMaterial.STANDARD_CARBON_COMPOSITE.name,
                actionType = RodAction.MEDIUM_FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.CARP.name,
                isPreset = true
            ),
            RodProfile(
                name = "Sonik DominatorX RS (3.90 m - 3.50 lb)",
                lengthMeters = 3.90,
                sensorDistanceCm = 65,
                testCurveLbs = 3.50,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.CARP.name,
                isPreset = true
            ),
            RodProfile(
                name = "Sonik VaderX RS (3.60 m - 3.25 lb)",
                lengthMeters = 3.60,
                sensorDistanceCm = 55,
                testCurveLbs = 3.25,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.MEDIUM_FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.CARP.name,
                isPreset = true
            ),
            RodProfile(
                name = "Prologic C3 Fulcrum (3.90 m - 3.50 lb)",
                lengthMeters = 3.90,
                sensorDistanceCm = 65,
                testCurveLbs = 3.50,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.CARP.name,
                isPreset = true
            ),
            RodProfile(
                name = "Prologic C-Series SC (3.60 m - 3.25 lb)",
                lengthMeters = 3.60,
                sensorDistanceCm = 55,
                testCurveLbs = 3.25,
                materialType = RodMaterial.STANDARD_CARBON_COMPOSITE.name,
                actionType = RodAction.MEDIUM_FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.CARP.name,
                isPreset = true
            ),
            RodProfile(
                name = "Century Stealth Graphene (3.90 m - 3.75 lb)",
                lengthMeters = 3.90,
                sensorDistanceCm = 70,
                testCurveLbs = 3.75,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.CARP.name,
                isPreset = true
            ),
            RodProfile(
                name = "Shimano Tribal TX-9A (3.90 m - 3.50 lb)",
                lengthMeters = 3.90,
                sensorDistanceCm = 65,
                testCurveLbs = 3.50,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.CARP.name,
                isPreset = true
            ),
            RodProfile(
                name = "Shimano Tribal TX-5A (3.60 m - 3.25 lb)",
                lengthMeters = 3.60,
                sensorDistanceCm = 60,
                testCurveLbs = 3.25,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.MEDIUM_FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.CARP.name,
                isPreset = true
            ),
            RodProfile(
                name = "Shimano Tribal TX-2 (3.90 m - 3.25 lb)",
                lengthMeters = 3.90,
                sensorDistanceCm = 60,
                testCurveLbs = 3.25,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.MEDIUM_FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.CARP.name,
                isPreset = true
            ),
            RodProfile(
                name = "Daiwa Infinity DF (3.90 m - 3.75 lb)",
                lengthMeters = 3.90,
                sensorDistanceCm = 70,
                testCurveLbs = 3.75,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.CARP.name,
                isPreset = true
            ),
            RodProfile(
                name = "Daiwa Black Widow G50 (3.60 m - 3.50 lb)",
                lengthMeters = 3.60,
                sensorDistanceCm = 60,
                testCurveLbs = 3.50,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.CARP.name,
                isPreset = true
            ),
            RodProfile(
                name = "Okuma Custom Black Carp (3.60 m - 3.00 lb)",
                lengthMeters = 3.60,
                sensorDistanceCm = 50,
                testCurveLbs = 3.00,
                materialType = RodMaterial.STANDARD_CARBON_COMPOSITE.name,
                actionType = RodAction.MEDIUM_FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.CARP.name,
                isPreset = true
            ),

            // ==================== 🌊 3. SURFCASTING & KIYI DENİZ (4.20m - 5.00m) ====================
            RodProfile(
                name = "Shimano Ultegra Surf Tubular (4.25 m - 200 gr)",
                lengthMeters = 4.25,
                sensorDistanceCm = 100,
                testCurveLbs = 4.00,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.WAVY_SEA.name,
                categoryType = RodCategory.SURF.name,
                isPreset = true
            ),
            RodProfile(
                name = "Shimano Speedmaster Surf (4.50 m - 225 gr)",
                lengthMeters = 4.50,
                sensorDistanceCm = 110,
                testCurveLbs = 4.50,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.WAVY_SEA.name,
                categoryType = RodCategory.SURF.name,
                isPreset = true
            ),
            RodProfile(
                name = "Daiwa Tournament Surf (4.50 m - 100-225 gr)",
                lengthMeters = 4.50,
                sensorDistanceCm = 110,
                testCurveLbs = 4.50,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.WAVY_SEA.name,
                categoryType = RodCategory.SURF.name,
                isPreset = true
            ),
            RodProfile(
                name = "Daiwa Cast'izm Surf (4.25 m - 100-225 gr)",
                lengthMeters = 4.25,
                sensorDistanceCm = 95,
                testCurveLbs = 4.20,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.WAVY_SEA.name,
                categoryType = RodCategory.SURF.name,
                isPreset = true
            ),
            RodProfile(
                name = "Okuma Trio Rex Surf (4.20 m - 100-250 gr)",
                lengthMeters = 4.20,
                sensorDistanceCm = 95,
                testCurveLbs = 4.50,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.WAVY_SEA.name,
                categoryType = RodCategory.SURF.name,
                isPreset = true
            ),
            RodProfile(
                name = "Trabucco Kronos Surf (4.20 m - 200 gr)",
                lengthMeters = 4.20,
                sensorDistanceCm = 90,
                testCurveLbs = 4.00,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.WAVY_SEA.name,
                categoryType = RodCategory.SURF.name,
                isPreset = true
            ),
            RodProfile(
                name = "Trabucco Cassiopea XTR (4.50 m - 200 gr)",
                lengthMeters = 4.50,
                sensorDistanceCm = 105,
                testCurveLbs = 4.20,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.WAVY_SEA.name,
                categoryType = RodCategory.SURF.name,
                isPreset = true
            ),
            RodProfile(
                name = "Vercelli Enygma Scuderia (4.20 m - 100-250 gr)",
                lengthMeters = 4.20,
                sensorDistanceCm = 95,
                testCurveLbs = 4.50,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.WAVY_SEA.name,
                categoryType = RodCategory.SURF.name,
                isPreset = true
            ),
            RodProfile(
                name = "Yuki Neox Orata Surf (4.20 m - 100-250 gr)",
                lengthMeters = 4.20,
                sensorDistanceCm = 95,
                testCurveLbs = 4.50,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.WAVY_SEA.name,
                categoryType = RodCategory.SURF.name,
                isPreset = true
            ),
            RodProfile(
                name = "Albastar Leader Surf (3.90 m - 100-200 gr)",
                lengthMeters = 3.90,
                sensorDistanceCm = 70,
                testCurveLbs = 3.80,
                materialType = RodMaterial.STANDARD_CARBON_COMPOSITE.name,
                actionType = RodAction.MEDIUM_FAST_ACTION.name,
                waterType = WaterEnvironment.WAVY_SEA.name,
                categoryType = RodCategory.SURF.name,
                isPreset = true
            ),

            // ==================== 🦈 4. AĞIR YAYIN & CATFISH (2.70m - 3.20m / 300-1000gr) ====================
            RodProfile(
                name = "Black Cat Perfect Passion (3.20 m - 600 gr)",
                lengthMeters = 3.20,
                sensorDistanceCm = 50,
                testCurveLbs = 5.50,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.SLOW_FULL_PARABOLIC.name,
                waterType = WaterEnvironment.RIVER_FLOW.name,
                categoryType = RodCategory.CATFISH.name,
                isPreset = true
            ),
            RodProfile(
                name = "Black Cat Freestyle Bank (3.00 m - 400 gr)",
                lengthMeters = 3.00,
                sensorDistanceCm = 45,
                testCurveLbs = 5.00,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.MEDIUM_PARABOLIC.name,
                waterType = WaterEnvironment.RIVER_FLOW.name,
                categoryType = RodCategory.CATFISH.name,
                isPreset = true
            ),
            RodProfile(
                name = "Zeck Fishing Pro-Cat (3.00 m - 500 gr)",
                lengthMeters = 3.00,
                sensorDistanceCm = 45,
                testCurveLbs = 5.20,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.SLOW_FULL_PARABOLIC.name,
                waterType = WaterEnvironment.RIVER_FLOW.name,
                categoryType = RodCategory.CATFISH.name,
                isPreset = true
            ),
            RodProfile(
                name = "Madcat Green Heavy Duty (3.00 m - 400 gr)",
                lengthMeters = 3.00,
                sensorDistanceCm = 45,
                testCurveLbs = 4.80,
                materialType = RodMaterial.FIBERGLASS.name,
                actionType = RodAction.SLOW_FULL_PARABOLIC.name,
                waterType = WaterEnvironment.RIVER_FLOW.name,
                categoryType = RodCategory.CATFISH.name,
                isPreset = true
            ),
            RodProfile(
                name = "Shimano Beastmaster Monster (3.00 m - 400 gr)",
                lengthMeters = 3.00,
                sensorDistanceCm = 45,
                testCurveLbs = 5.50,
                materialType = RodMaterial.FIBERGLASS.name,
                actionType = RodAction.SLOW_FULL_PARABOLIC.name,
                waterType = WaterEnvironment.RIVER_FLOW.name,
                categoryType = RodCategory.CATFISH.name,
                isPreset = true
            ),

            // ==================== 🌾 5. FEEDER & METHOD FEEDER (3.00m - 4.20m) ====================
            RodProfile(
                name = "Preston Innovations Supera X (3.90 m - 120 gr)",
                lengthMeters = 3.90,
                sensorDistanceCm = 80,
                testCurveLbs = 2.80,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.MEDIUM_FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.FEEDER.name,
                isPreset = true
            ),
            RodProfile(
                name = "Matrix Horizon Pro Distance (4.00 m - 130 gr)",
                lengthMeters = 4.00,
                sensorDistanceCm = 85,
                testCurveLbs = 3.00,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.FEEDER.name,
                isPreset = true
            ),
            RodProfile(
                name = "Guru N-Gauge Pro Feeder (3.60 m - 90 gr)",
                lengthMeters = 3.60,
                sensorDistanceCm = 70,
                testCurveLbs = 2.40,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.MEDIUM_FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.FEEDER.name,
                isPreset = true
            ),
            RodProfile(
                name = "Daiwa N'Zon Distance Feeder (3.90 m - 150 gr)",
                lengthMeters = 3.90,
                sensorDistanceCm = 80,
                testCurveLbs = 3.20,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.FEEDER.name,
                isPreset = true
            ),
            RodProfile(
                name = "Daiwa Ninja Feeder (3.60 m - 120 gr)",
                lengthMeters = 3.60,
                sensorDistanceCm = 70,
                testCurveLbs = 2.60,
                materialType = RodMaterial.HIGH_MODULUS_CARBON.name,
                actionType = RodAction.MEDIUM_FAST_ACTION.name,
                waterType = WaterEnvironment.CALM_LAKE.name,
                categoryType = RodCategory.FEEDER.name,
                isPreset = true
            )
        )
    }
}
