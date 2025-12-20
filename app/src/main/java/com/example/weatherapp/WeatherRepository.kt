package com.example.weatherapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class WeatherRepository {

    private val apiKey = "7d56f638dbd0babfd3ad3407397edcf9"
    private val baseUrl = "https://api.openweathermap.org/data/2.5/weather"

    suspend fun getWeather(city: String): WeatherResponse? = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl?q=$city&appid=$apiKey"
            val response = URL(url).readText()
            parseWeatherResponse(response)
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

        return WeatherResponse(
            name = name,
            main = main,
            weather = weather,
            wind = wind
        )
    }
}