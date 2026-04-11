package com.example.weatherapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.util.Locale

class SequentialWeatherSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val repository = WeatherRepository()

    override suspend fun doWork(): Result {
        val city = inputData.getString(KEY_CITY)?.trim().orEmpty()
        if (city.isBlank()) {
            return Result.failure(errorData("Missing city"))
        }
        val cityIndex = inputData.getInt(KEY_CITY_INDEX, -1)
        val lang = inputData.getString(KEY_LANG) ?: "en"

        if (isStopped) return Result.failure(errorData("Work cancelled"))

        val response = repository.getWeather(city, lang)
        if (response == null) {
            return Result.success(
                Data.Builder()
                    .putInt(KEY_CITY_INDEX, cityIndex)
                    .putString(KEY_REQUESTED_CITY, city)
                    .putString(KEY_ERROR_MESSAGE, "Failed to load weather for $city")
                    .build()
            )
        }

        val item = JSONObject().apply {
            put("index", cityIndex)
            put("requestedCity", city)
            put("city", response.name)
            put("temperature", response.main.temp)
            put("feelsLike", response.main.feels_like)
            put(
                "description",
                response.weather.firstOrNull()?.description?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                } ?: "N/A"
            )
            put("humidity", response.main.humidity)
            put("pressure", response.main.pressure)
            put("windSpeed", response.wind.speed)
            put("windDegree", response.wind.deg)
            put("icon", response.weather.firstOrNull()?.icon ?: "")
            put("clouds", response.clouds.all)
        }

        return Result.success(
            Data.Builder()
                .putInt(KEY_CITY_INDEX, cityIndex)
                .putString(KEY_REQUESTED_CITY, city)
                .putString(KEY_RESULT_JSON, item.toString())
                .build()
        )
    }

    private fun errorData(message: String): Data {
        return Data.Builder()
            .putString(KEY_ERROR_MESSAGE, message)
            .build()
    }

    companion object {
        const val KEY_CITY = "city"
        const val KEY_CITY_INDEX = "city_index"
        const val KEY_LANG = "lang"
        const val KEY_REQUESTED_CITY = "requested_city"
        const val KEY_RESULT_JSON = "result_json"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val UNIQUE_WORK_NAME = "sequential_weather_sync"
    }
}
