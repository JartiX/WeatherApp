package com.example.weatherapp

data class WeatherData(
    val city: String,
    val temperature: Double,
    val feelsLike: Double,
    val description: String,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val icon: String
) {
    val temperatureCelsius: String
        get() = "${(temperature - 273.15).toInt()}°C"

    val feelsLikeCelsius: String
        get() = "${(feelsLike - 273.15).toInt()}°C"

    val descriptionCapitalized: String
        get() = description.replaceFirstChar { it.uppercase() }

    val humidityPercent: String
        get() = "$humidity%"

    val pressureHpa: String
        get() = "$pressure hPa"

    val windSpeedMs: String
        get() = "%.1f м/с".format(windSpeed)

    val iconUrl: String
        get() = "https://openweathermap.org/img/wn/${icon}@2x.png"
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
    val speed: Double
)