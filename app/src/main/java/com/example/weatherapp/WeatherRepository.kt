package com.example.weatherapp

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class WeatherRepository {

    private val apiKey = "7d56f638dbd0babfd3ad3407397edcf9"
    private val forecastUrl = "https://api.openweathermap.org/data/2.5/forecast"
    private val geoUrl = "https://api.openweathermap.org/geo/1.0/direct"

    data class GeoResult(val lat: Double, val lon: Double, val displayName: String)

    suspend fun getWeather(city: String, lang: String = "en"): WeatherResponse? = withContext(Dispatchers.IO) {
        try {
            val needsLocalizedName = lang in listOf("zh_cn", "ja", "ru")
            if (needsLocalizedName) {
                val geo = fetchCoordinates(city, lang) ?: return@withContext null
                val response = fetchForecastByCoords(geo.lat, geo.lon, lang)
                response?.copy(name = geo.displayName)
            } else {
                var response = fetchForecastByCity(city, lang)
                if (response == null) {
                    val geo = fetchCoordinates(city, lang) ?: return@withContext null
                    response = fetchForecastByCoords(geo.lat, geo.lon, lang)
                }
                response
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun fetchForecastByCity(city: String, lang: String): WeatherResponse? {
        return try {
            val urlString = Uri.parse(forecastUrl).buildUpon()
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

    private fun fetchForecastByCoords(lat: Double, lon: Double, lang: String): WeatherResponse? {
        val urlString = Uri.parse(forecastUrl).buildUpon()
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
        val forecastList = jsonObject.optJSONArray("list")
        val sourceObject = if (forecastList != null && forecastList.length() > 0) {
            forecastList.getJSONObject(0)
        } else {
            jsonObject
        }
        val name = jsonObject.optJSONObject("city")?.optString("name")
            ?.takeIf { it.isNotBlank() }
            ?: jsonObject.optString("name")
        val mainObject = sourceObject.optJSONObject("main") ?: JSONObject()
        val main = Main(
            temp = mainObject.optDouble("temp", 0.0),
            feels_like = mainObject.optDouble("feels_like", 0.0),
            humidity = mainObject.optInt("humidity", 0),
            pressure = mainObject.optInt("pressure", 0)
        )
        val weatherArray = sourceObject.optJSONArray("weather") ?: org.json.JSONArray()
        val weather = mutableListOf<Weather>()
        for (i in 0 until weatherArray.length()) {
            val weatherObj = weatherArray.getJSONObject(i)
            weather.add(
                Weather(
                    description = weatherObj.optString("description", "N/A"),
                    icon = weatherObj.optString("icon", "")
                )
            )
        }
        val windObject = sourceObject.optJSONObject("wind") ?: JSONObject()
        val wind = Wind(
            speed = windObject.optDouble("speed", 0.0),
            deg = windObject.optInt("deg", 0)
        )
        val cloudsObject = sourceObject.optJSONObject("clouds") ?: JSONObject()
        val clouds = Clouds(
            all = cloudsObject.optInt("all", 0)
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
