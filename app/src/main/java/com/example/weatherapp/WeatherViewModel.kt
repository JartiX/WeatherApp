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

    private val _isLoading = MutableLiveData<Boolean>(false) // Задаем начальное значение
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

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

