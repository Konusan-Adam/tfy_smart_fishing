package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "weather_cache")
data class WeatherEntity(
    @PrimaryKey val id: Int = 0,
    val temp: Double,
    val pressure: Int,
    val humidity: Int,
    val windSpeed: Double,
    val cityName: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_cache WHERE id = 0")
    suspend fun getCachedWeather(): WeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheWeather(weather: WeatherEntity)
}
