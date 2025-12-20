package com.example.weatherapp

data class WeatherData(
    val city: String,
    val temperature: Double,
    val feelsLike: Double,
    val description: String,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val windDegree: Int,
    val icon: String
) {
    val temperatureCelsius: String
        get() = "${(temperature - 273.15).toInt()}°C"

    val temperatureFahrenheit: String
        get() = "${((temperature - 273.15) * 9/5 + 32).toInt()}°F"

    val feelsLikeCelsius: String
        get() = "${(feelsLike - 273.15).toInt()}°C"

    val feelsLikeFahrenheit: String
        get() = "${((feelsLike - 273.15) * 9/5 + 32).toInt()}°F"

    val descriptionCapitalized: String
        get() = description.replaceFirstChar { it.uppercase() }

    val humidityPercent: String
        get() = "$humidity%"

    val pressureHpa: String
        get() = "$pressure hPa"

    val windSpeedMs: String
        get() = "%.1f м/с".format(windSpeed)

    val windDirection: String
        get() = getWindDirection(windDegree)

    val iconUrl: String
        get() = "https://openweathermap.org/img/wn/${icon}@2x.png"

    private fun getWindDirection(degree: Int): String {
        return when (degree) {
            in 0..22 -> "С"
            in 23..67 -> "СВ"
            in 68..112 -> "В"
            in 113..157 -> "ЮВ"
            in 158..202 -> "Ю"
            in 203..247 -> "ЮЗ"
            in 248..292 -> "З"
            in 293..337 -> "СЗ"
            in 338..360 -> "С"
            else -> "N/A"
        }
    }
}

// Response модели для API
data class WeatherResponse(
    val name: String,
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind
)

data class Main(
    val temp: Double,
    val feels_like: Double,
    val humidity: Int,
    val pressure: Int
)

data class Weather(
    val description: String,
    val icon: String
)

data class Wind(
    val speed: Double,
    val deg: Int
)