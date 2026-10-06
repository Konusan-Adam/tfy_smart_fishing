package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    @Json(name = "current") val current: OpenMeteoCurrent
)

@JsonClass(generateAdapter = true)
data class OpenMeteoCurrent(
    @Json(name = "temperature_2m") val temp: Double,
    @Json(name = "relative_humidity_2m") val humidity: Int,
    @Json(name = "surface_pressure") val pressure: Double,
    @Json(name = "wind_speed_10m") val windSpeed: Double,
    @Json(name = "weather_code") val weatherCode: Int
)

data class WeatherDisplayData(
    val temperature: String,
    val pressure: String,
    val humidity: String,
    val windSpeed: String,
    val cityName: String,
    val description: String,
    val isFromCache: Boolean = false,
    val noNetwork: Boolean = false
)
