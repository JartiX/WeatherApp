package com.example.weatherapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.Locale

class WeatherViewModel : ViewModel() {

    private val repository = WeatherRepository()

    private val _cities = MutableLiveData<MutableList<String>>(
        mutableListOf("Moscow", "London", "Paris", "New York", "Tokyo")
    )
    val cities: LiveData<MutableList<String>> = _cities

    private val _selectedCityIndex = MutableLiveData<Int>(0)
    val selectedCityIndex: LiveData<Int> = _selectedCityIndex

    private val _weatherCache = MutableLiveData<MutableMap<String, WeatherData>>(mutableMapOf())
    val weatherCache: LiveData<MutableMap<String, WeatherData>> = _weatherCache

    private val _weatherData = MutableLiveData<WeatherData?>()
    val weatherData: LiveData<WeatherData?> = _weatherData

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isCelsius = MutableLiveData<Boolean>(true)
    val isCelsius: LiveData<Boolean> = _isCelsius

    private val _showWindDirection = MutableLiveData<Boolean>(true)
    val showWindDirection: LiveData<Boolean> = _showWindDirection

    private val _citiesUpdated = MutableLiveData<Boolean>()
    val citiesUpdated: LiveData<Boolean> = _citiesUpdated

    private val _weatherUpdatedEvent = MutableLiveData<Pair<Int, WeatherData>?>()
    val weatherUpdatedEvent: LiveData<Pair<Int, WeatherData>?> = _weatherUpdatedEvent

    fun setTemperatureUnit(celsius: Boolean) {
        _isCelsius.value = celsius
    }

    fun setShowWindDirection(show: Boolean) {
        _showWindDirection.value = show
    }

    fun selectCity(position: Int) {
        if (position < 0 || position >= (_cities.value?.size ?: 0)) return
        _selectedCityIndex.value = position
        val city = _cities.value?.get(position) ?: return

        val cache = _weatherCache.value
        val cached = cache?.get(city.lowercase())
        if (cached != null) {
            _weatherData.value = cached
        } else {
            _weatherData.value = null
        }

        loadWeather(city, position)
    }

    fun addCity(cityName: String) {
        val trimmed = cityName.trim()
        if (trimmed.isEmpty()) return

        val currentList = _cities.value ?: mutableListOf()
        if (currentList.any { it.equals(trimmed, ignoreCase = true) }) {
            _errorMessage.value = "Город уже в списке"
            return
        }

        currentList.add(trimmed)
        _cities.value = currentList
        _citiesUpdated.value = true

        selectCity(currentList.size - 1)
    }

    fun removeCity(position: Int) {
        val currentList = _cities.value ?: return
        if (currentList.size <= 1) {
            _errorMessage.value = "Нельзя удалить последний город"
            return
        }
        if (position < 0 || position >= currentList.size) return

        val removedCity = currentList[position]
        _weatherCache.value?.remove(removedCity.lowercase())

        currentList.removeAt(position)
        _cities.value = currentList
        _citiesUpdated.value = true

        val selectedIndex = _selectedCityIndex.value ?: 0
        when {
            position == selectedIndex -> {
                val newIndex = if (selectedIndex >= currentList.size) currentList.size - 1 else selectedIndex
                selectCity(newIndex)
            }
            position < selectedIndex -> {
                _selectedCityIndex.value = selectedIndex - 1
            }
        }
    }

    fun loadWeather(city: String, position: Int = -1) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val response = repository.getWeather(city)

                if (response != null) {
                    val weatherData = WeatherData(
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
                        icon = response.weather.firstOrNull()?.icon ?: "",
                        clouds = response.clouds.all
                    )

                    val cache = _weatherCache.value ?: mutableMapOf()
                    cache[city.lowercase()] = weatherData
                    _weatherCache.value = cache

                    val currentCities = _cities.value ?: return@launch
                    val actualPosition = if (position >= 0) position else currentCities.indexOfFirst {
                        it.equals(city, ignoreCase = true)
                    }

                    if (actualPosition == _selectedCityIndex.value) {
                        _weatherData.value = weatherData
                    }

                    if (actualPosition >= 0) {
                        _weatherUpdatedEvent.value = Pair(actualPosition, weatherData)
                    }

                } else {
                    _errorMessage.value = "Не удалось загрузить данные для \"$city\""
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAllCitiesWeather() {
        val cityList = _cities.value ?: return
        cityList.forEachIndexed { index, city ->
            loadWeather(city, index)
        }
    }

    fun refreshCurrentCity() {
        val index = _selectedCityIndex.value ?: 0
        val city = _cities.value?.getOrNull(index) ?: return
        _isLoading.value = false
        loadWeather(city, index)
    }
}