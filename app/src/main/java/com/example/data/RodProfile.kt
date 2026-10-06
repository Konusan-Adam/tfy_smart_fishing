package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.engine.RodAction
import com.example.engine.RodCategory
import com.example.engine.RodMaterial
import com.example.engine.RodPhysicsInput
import com.example.engine.WaterEnvironment

@Entity(tableName = "rod_profiles")
data class RodProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val lengthMeters: Double = 3.60,
    val sensorDistanceCm: Int = 60,
    val testCurveLbs: Double = 3.50,
    val materialType: String = RodMaterial.HIGH_MODULUS_CARBON.name,
    val actionType: String = RodAction.FAST_ACTION.name,
    val waterType: String = WaterEnvironment.CALM_LAKE.name,
    val categoryType: String = RodCategory.CARP.name,
    val isPreset: Boolean = false
) {
    fun getMaterial(): RodMaterial {
        return try {
            RodMaterial.valueOf(materialType)
        } catch (_: Exception) {
            RodMaterial.HIGH_MODULUS_CARBON
        }
    }

    fun getAction(): RodAction {
        return try {
            RodAction.valueOf(actionType)
        } catch (_: Exception) {
            RodAction.FAST_ACTION
        }
    }

    fun getEnvironment(): WaterEnvironment {
        return try {
            WaterEnvironment.valueOf(waterType)
        } catch (_: Exception) {
            WaterEnvironment.CALM_LAKE
        }
    }

    fun getCategory(): RodCategory {
        return try {
            RodCategory.valueOf(categoryType)
        } catch (_: Exception) {
            RodCategory.CARP
        }
    }

    fun toPhysicsInput(): RodPhysicsInput {
        return RodPhysicsInput(
            rodName = name,
            lengthMeters = lengthMeters,
            sensorDistanceMeters = sensorDistanceCm / 100.0,
            testCurveLbs = testCurveLbs,
            material = getMaterial(),
            action = getAction(),
            environment = getEnvironment(),
            category = getCategory()
        )
    }

    val lengthFormatted: String get() = String.format("%.2f m", lengthMeters)
    val sensorDistanceFormatted: String get() = "$sensorDistanceCm cm (Sap Üstü)"
    val testCurveFormatted: String get() = "${testCurveLbs} lbs"
}
