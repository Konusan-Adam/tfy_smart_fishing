package com.example.api

import com.example.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,surface_pressure,wind_speed_10m,weather_code"
    ): WeatherResponse

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/v1/"
    }
}
