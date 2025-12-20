package com.example.weatherapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.Locale

class WeatherViewModel : ViewModel() {

    private val repository = WeatherRepository()

    private val _weatherData = MutableLiveData<WeatherData?>()
    val weatherData: LiveData<WeatherData?> = _weatherData

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Свойство для выбора шкалы температуры (true = Celsius, false = Fahrenheit)
    private val _isCelsius = MutableLiveData<Boolean>(true)
    val isCelsius: LiveData<Boolean> = _isCelsius

    // Свойство для отображения направления ветра
    private val _showWindDirection = MutableLiveData<Boolean>(true)
    val showWindDirection: LiveData<Boolean> = _showWindDirection

    fun setTemperatureUnit(celsius: Boolean) {
        _isCelsius.value = celsius
    }

    fun setShowWindDirection(show: Boolean) {
        _showWindDirection.value = show
    }

    fun loadWeather(city: String) {
        if (_isLoading.value == true) return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val response = repository.getWeather(city)

                if (response != null) {
                    _weatherData.value = WeatherData(
                        city = response.name,
                        temperature = response.main.temp,
                        feelsLike = response.main.feels_like,
                        description = response.weather.firstOrNull()?.description?.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        } ?: "N/A",
                        humidity = response.main.humidity,
                        pressure = response.main.pressure,
                        windSpeed = response.wind.speed,
                        windDegree = response.wind.deg,
                        icon = response.weather.firstOrNull()?.icon ?: ""
                    )
                } else {
                    _errorMessage.value = "Не удалось загрузить данные о погоде"
                    _weatherData.value = null
                }

            } catch (e: Exception) {
                _errorMessage.value = "Ошибка: ${e.message}"
                _weatherData.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
}