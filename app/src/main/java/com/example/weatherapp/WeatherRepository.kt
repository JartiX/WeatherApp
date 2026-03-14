package com.example.weatherapp

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class WeatherRepository {

    private val apiKey = "7d56f638dbd0babfd3ad3407397edcf9"
    private val baseUrl = "https://api.openweathermap.org/data/2.5/weather"
    private val geoUrl = "https://api.openweathermap.org/geo/1.0/direct"

    data class GeoResult(val lat: Double, val lon: Double, val displayName: String)

    suspend fun getWeather(city: String, lang: String = "en"): WeatherResponse? = withContext(Dispatchers.IO) {
        try {
            val needsLocalizedName = lang in listOf("zh_cn", "ja", "ru")
            if (needsLocalizedName) {
                val geo = fetchCoordinates(city, lang) ?: return@withContext null
                val response = fetchWeatherByCoords(geo.lat, geo.lon, lang)
                response?.copy(name = geo.displayName)
            } else {
                var response = fetchWeatherByCity(city, lang)
                if (response == null) {
                    val geo = fetchCoordinates(city, lang) ?: return@withContext null
                    response = fetchWeatherByCoords(geo.lat, geo.lon, lang)
                }
                response
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun fetchWeatherByCity(city: String, lang: String): WeatherResponse? {
        return try {
            val urlString = Uri.parse(baseUrl).buildUpon()
                .appendQueryParameter("q", city)
                .appendQueryParameter("appid", apiKey)
                .appendQueryParameter("lang", lang)
                .appendQueryParameter("units", "metric")
                .build()
                .toString()
            val response = executeRequest(urlString)
            if (response != null) {
                val json = JSONObject(response)
                if (json.has("cod") && json.get("cod").toString() != "200") return null
                parseWeatherResponse(response)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchCoordinates(city: String, lang: String = "en"): GeoResult? {
        val urlString = Uri.parse(geoUrl).buildUpon()
            .appendQueryParameter("q", city)
            .appendQueryParameter("limit", "1")
            .appendQueryParameter("appid", apiKey)
            .build()
            .toString()
        val response = executeRequest(urlString) ?: return null
        val jsonArray = org.json.JSONArray(response)
        if (jsonArray.length() == 0) return null
        val item = jsonArray.getJSONObject(0)
        val lat = item.getDouble("lat")
        val lon = item.getDouble("lon")
        val defaultName = item.optString("name", city)
        val displayName = getLocalizedName(item, lang) ?: defaultName
        return GeoResult(lat, lon, displayName)
    }

    private fun getLocalizedName(geoItem: JSONObject, lang: String): String? {
        val localNames = geoItem.optJSONObject("local_names") ?: return null
        val keysToTry = when (lang) {
            "zh_cn" -> listOf("zh_cn", "zh")
            "ja" -> listOf("ja")
            "ru" -> listOf("ru")
            else -> listOf(lang)
        }
        for (key in keysToTry) {
            val value = localNames.optString(key)
            if (value.isNotEmpty()) return value
        }
        return null
    }

    private fun fetchWeatherByCoords(lat: Double, lon: Double, lang: String): WeatherResponse? {
        val urlString = Uri.parse(baseUrl).buildUpon()
            .appendQueryParameter("lat", lat.toString())
            .appendQueryParameter("lon", lon.toString())
            .appendQueryParameter("appid", apiKey)
            .appendQueryParameter("lang", lang)
            .appendQueryParameter("units", "metric")
            .build()
            .toString()
        val response = executeRequest(urlString) ?: return null
        val json = JSONObject(response)
        if (json.has("cod") && json.get("cod").toString() != "200") return null
        return parseWeatherResponse(response)
    }

    private fun executeRequest(urlString: String): String? {
        return try {
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.requestMethod = "GET"
            val response = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
            connection.disconnect()
            response
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseWeatherResponse(json: String): WeatherResponse {
        val jsonObject = JSONObject(json)

        val name = jsonObject.getString("name")

        val mainObject = jsonObject.getJSONObject("main")
        val main = Main(
            temp = mainObject.getDouble("temp"),
            feels_like = mainObject.getDouble("feels_like"),
            humidity = mainObject.getInt("humidity"),
            pressure = mainObject.getInt("pressure")
        )

        val weatherArray = jsonObject.getJSONArray("weather")
        val weather = mutableListOf<Weather>()
        for (i in 0 until weatherArray.length()) {
            val weatherObj = weatherArray.getJSONObject(i)
            weather.add(
                Weather(
                    description = weatherObj.getString("description"),
                    icon = weatherObj.getString("icon")
                )
            )
        }

        val windObject = jsonObject.getJSONObject("wind")
        val wind = Wind(
            speed = windObject.getDouble("speed"),
            deg = windObject.optInt("deg", 0)
        )

        val cloudsObject = jsonObject.getJSONObject("clouds")
        val clouds = Clouds(
            all = cloudsObject.getInt("all")
        )

        return WeatherResponse(
            name = name,
            main = main,
            weather = weather,
            wind = wind,
            clouds = clouds
        )
    }
}