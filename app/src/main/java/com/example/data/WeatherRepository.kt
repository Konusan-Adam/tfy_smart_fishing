package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.api.WeatherApi
import com.example.model.WeatherDisplayData
import com.example.model.WeatherResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Locale

class WeatherRepository(
    private val context: Context,
    private val weatherDao: WeatherDao
) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val api = Retrofit.Builder()
        .baseUrl(WeatherApi.BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(OkHttpClient())
        .build()
        .create(WeatherApi::class.java)

    suspend fun getWeatherData(lat: Double, lon: Double): WeatherDisplayData {
        return if (isNetworkAvailable()) {
            try {
                val response = api.getCurrentWeather(lat, lon)
                saveToCache(response)
                mapToDisplayData(response, isFromCache = false)
            } catch (e: Exception) {
                android.util.Log.e("WeatherRepo", "Fetch error: ${e.message}")
                getFromCache(noNetwork = false)
            }
        } else {
            getFromCache(noNetwork = true)
        }
    }

    private fun getNoDataState(isNoNetwork: Boolean): WeatherDisplayData {
        return WeatherDisplayData(
            temperature = "--°C",
            pressure = "-- hPa",
            humidity = "--",
            windSpeed = "-- m/s",
            cityName = "KONUM BEKLENİYOR",
            description = if (isNoNetwork) "İNTERNET BAĞLANTISI YOK" else "VERİ ALINIYOR...",
            isFromCache = true,
            noNetwork = isNoNetwork
        )
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    private suspend fun saveToCache(response: WeatherResponse) {
        val entity = WeatherEntity(
            temp = response.current.temp,
            pressure = response.current.pressure.toInt(),
            humidity = response.current.humidity,
            windSpeed = response.current.windSpeed,
            cityName = "Mera Konumu",
            description = getWeatherDescription(response.current.weatherCode)
        )
        weatherDao.cacheWeather(entity)
    }

    private suspend fun getFromCache(noNetwork: Boolean): WeatherDisplayData {
        val cached = weatherDao.getCachedWeather()
        return if (cached != null) {
            WeatherDisplayData(
                temperature = String.format(Locale.getDefault(), "%.1f°C", cached.temp),
                pressure = "${cached.pressure} hPa",
                humidity = "%${cached.humidity}",
                windSpeed = "${cached.windSpeed} m/s",
                cityName = cached.cityName,
                description = cached.description,
                isFromCache = true,
                noNetwork = noNetwork
            )
        } else {
            // Hiç veri yoksa nötr placeholders gösterelim
            getNoDataState(isNoNetwork = noNetwork)
        }
    }

    private fun mapToDisplayData(response: WeatherResponse, isFromCache: Boolean): WeatherDisplayData {
        return WeatherDisplayData(
            temperature = String.format(Locale.getDefault(), "%.1f°C", response.current.temp),
            pressure = "${response.current.pressure.toInt()} hPa",
            humidity = "%${response.current.humidity}",
            windSpeed = "${response.current.windSpeed} m/s",
            cityName = "BALIKÇI MERASI",
            description = getWeatherDescription(response.current.weatherCode),
            isFromCache = isFromCache,
            noNetwork = false
        )
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Açık Gökyüzü"
            1, 2, 3 -> "Parçalı Bulutlu"
            45, 48 -> "Sisli"
            51, 53, 55 -> "Çiseleme"
            61, 63, 65 -> "Yağmurlu"
            71, 73, 75 -> "Karlı"
            80, 81, 82 -> "Sağanak Yağış"
            95, 96, 99 -> "Fırtınalı"
            else -> "Bilinmiyor"
        }
    }
}
